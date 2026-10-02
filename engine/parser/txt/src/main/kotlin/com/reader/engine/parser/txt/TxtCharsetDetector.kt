package com.reader.engine.parser.txt

import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * 编码嗅探结果数据类。
 *
 * @property charset 最终解析出的 Java [Charset]
 * @property charsetName 探测到的原始编码名称 (如 "UTF-8", "GB18030")
 * @property confidence 探测置信度 (0.0f .. 1.0f)
 * @property hasBom 是否包含 BOM 头部
 * @property bomLength BOM 头部字节长度 (0, 2, 3, 4)
 */
data class CharsetDetectionResult(
    val charset: Charset,
    val charsetName: String,
    val confidence: Float,
    val hasBom: Boolean,
    val bomLength: Int
)

/**
 * TXT 字符集编码自动嗅探器。
 *
 * 支持自动识别：
 * - UTF-8 (含 BOM 与无 BOM)
 * - GBK / GB2312 / GB18030
 * - Big5 (繁体中文)
 * - UTF-16LE / UTF-16BE
 * - UTF-32LE / UTF-32BE
 * - ASCII / ISO-8859-1
 *
 * 采用“BOM 快速识别 -> juniversalchardet 头部多块统计 -> UTF-8/GBK 启发式校验”三层嗅探机制。
 * 杜绝全量加载，仅读取前 64KB (可配置) 文件切片进行秒级探测。
 */
object TxtCharsetDetector {

    /** 默认探测采样字节大小 (64KB 足够覆盖绝大多数文本特征) */
    const val DEFAULT_SAMPLE_SIZE = 64 * 1024

    private val BOM_UTF_8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val BOM_UTF_16_BE = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
    private val BOM_UTF_16_LE = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
    private val BOM_UTF_32_BE = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xFE.toByte(), 0xFF.toByte())
    private val BOM_UTF_32_LE = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00.toByte(), 0x00.toByte())

    /**
     * 探测指定文件的字符集编码。
     *
     * @param file 待探测的目标文件
     * @param sampleSize 采样字节大小 (默认 64KB)
     * @return [CharsetDetectionResult] 探测结果
     */
    fun detect(file: File, sampleSize: Int = DEFAULT_SAMPLE_SIZE): CharsetDetectionResult {
        require(file.exists()) { "目标文件不存在: ${file.absolutePath}" }
        val fileSize = file.length()
        if (fileSize == 0L) {
            return CharsetDetectionResult(
                charset = StandardCharsets.UTF_8,
                charsetName = "UTF-8",
                confidence = 1.0f,
                hasBom = false,
                bomLength = 0
            )
        }

        val bytesToRead = minOf(fileSize, sampleSize.toLong()).toInt()
        val sampleBytes = ByteArray(bytesToRead)

        RandomAccessFile(file, "r").use { raf ->
            raf.readFully(sampleBytes, 0, bytesToRead)
        }

        return detect(sampleBytes)
    }

    /**
     * 探测指定 FileChannel 的字符集编码。
     *
     * @param channel 已经打开的 FileChannel
     * @param sampleSize 采样字节大小 (默认 64KB)
     * @return [CharsetDetectionResult] 探测结果
     */
    fun detect(channel: FileChannel, sampleSize: Int = DEFAULT_SAMPLE_SIZE): CharsetDetectionResult {
        val originalPos = channel.position()
        try {
            channel.position(0)
            val bytesToRead = minOf(channel.size(), sampleSize.toLong()).toInt()
            if (bytesToRead == 0) {
                return CharsetDetectionResult(
                    charset = StandardCharsets.UTF_8,
                    charsetName = "UTF-8",
                    confidence = 1.0f,
                    hasBom = false,
                    bomLength = 0
                )
            }

            val buffer = ByteBuffer.allocate(bytesToRead)
            var totalRead = 0
            while (totalRead < bytesToRead) {
                val read = channel.read(buffer)
                if (read <= 0) break
                totalRead += read
            }
            buffer.flip()
            val sampleBytes = ByteArray(buffer.remaining())
            buffer.get(sampleBytes)

            return detect(sampleBytes)
        } finally {
            channel.position(originalPos)
        }
    }

    /**
     * 探测字节数组样本的字符集编码。
     *
     * @param sampleBytes 样本字节数组
     * @return [CharsetDetectionResult] 探测结果
     */
    fun detect(sampleBytes: ByteArray): CharsetDetectionResult {
        if (sampleBytes.isEmpty()) {
            return CharsetDetectionResult(
                charset = StandardCharsets.UTF_8,
                charsetName = "UTF-8",
                confidence = 1.0f,
                hasBom = false,
                bomLength = 0
            )
        }

        // 1. 第一级：BOM (Byte Order Mark) 快速精确识别
        detectBom(sampleBytes)?.let { return it }

        // 2. 第二级：基于 juniversalchardet 的统计模型识别
        val detector = UniversalDetector(null)
        detector.handleData(sampleBytes, 0, sampleBytes.size)
        detector.dataEnd()
        val detectedCharsetName = detector.detectedCharset
        detector.reset()

        if (!detectedCharsetName.isNullOrBlank()) {
            val normalizedCharset = normalizeCharset(detectedCharsetName)
            return CharsetDetectionResult(
                charset = normalizedCharset,
                charsetName = detectedCharsetName,
                confidence = 0.95f,
                hasBom = false,
                bomLength = 0
            )
        }

        // 3. 第三级：启发式规则兜底校验
        // 3.1 校验是否为纯 ASCII 或合法 UTF-8 字节流
        val utf8Check = checkUtf8Validity(sampleBytes)
        if (utf8Check.isValidUtf8) {
            return CharsetDetectionResult(
                charset = StandardCharsets.UTF_8,
                charsetName = "UTF-8",
                confidence = if (utf8Check.hasNonAscii) 0.90f else 0.70f,
                hasBom = false,
                bomLength = 0
            )
        }

        // 3.2 校验是否包含高字节，常见中文 TXT 大多为 GBK / GB18030
        if (utf8Check.hasNonAscii) {
            val gbkCharset = getChineseCharset()
            return CharsetDetectionResult(
                charset = gbkCharset,
                charsetName = gbkCharset.name(),
                confidence = 0.85f,
                hasBom = false,
                bomLength = 0
            )
        }

        // 默认兜底为 UTF-8
        return CharsetDetectionResult(
            charset = StandardCharsets.UTF_8,
            charsetName = "UTF-8",
            confidence = 0.50f,
            hasBom = false,
            bomLength = 0
        )
    }

    /**
     * 校验 BOM 头部。
     */
    private fun detectBom(bytes: ByteArray): CharsetDetectionResult? {
        val len = bytes.size
        // UTF-32 LE / BE (4 字节优先比对)
        if (len >= 4) {
            if (matchesPrefix(bytes, BOM_UTF_32_BE)) {
                return CharsetDetectionResult(
                    charset = Charset.forName("UTF-32BE"),
                    charsetName = "UTF-32BE",
                    confidence = 1.0f,
                    hasBom = true,
                    bomLength = 4
                )
            }
            if (matchesPrefix(bytes, BOM_UTF_32_LE)) {
                return CharsetDetectionResult(
                    charset = Charset.forName("UTF-32LE"),
                    charsetName = "UTF-32LE",
                    confidence = 1.0f,
                    hasBom = true,
                    bomLength = 4
                )
            }
        }

        // UTF-8 BOM (3 字节)
        if (len >= 3 && matchesPrefix(bytes, BOM_UTF_8)) {
            return CharsetDetectionResult(
                charset = StandardCharsets.UTF_8,
                charsetName = "UTF-8",
                confidence = 1.0f,
                hasBom = true,
                bomLength = 3
            )
        }

        // UTF-16 LE / BE (2 字节)
        if (len >= 2) {
            if (matchesPrefix(bytes, BOM_UTF_16_BE)) {
                return CharsetDetectionResult(
                    charset = StandardCharsets.UTF_16BE,
                    charsetName = "UTF-16BE",
                    confidence = 1.0f,
                    hasBom = true,
                    bomLength = 2
                )
            }
            if (matchesPrefix(bytes, BOM_UTF_16_LE)) {
                return CharsetDetectionResult(
                    charset = StandardCharsets.UTF_16LE,
                    charsetName = "UTF-16LE",
                    confidence = 1.0f,
                    hasBom = true,
                    bomLength = 2
                )
            }
        }

        return null
    }

    private fun matchesPrefix(data: ByteArray, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) return false
        for (i in prefix.indices) {
            if (data[i] != prefix[i]) return false
        }
        return true
    }

    /**
     * 将探测库返回的编码名称归一化为兼容度最高、最健壮的 Java [Charset]。
     * 特别针对中文：GB2312, GBK 均映射为 GB18030 (超集，覆盖生僻字)。
     */
    fun normalizeCharset(rawName: String): Charset {
        val upper = rawName.trim().uppercase()
        return when {
            upper in listOf("GB18030", "GBK", "GB2312", "WINDOWS-936", "EUC-CN") -> {
                getChineseCharset()
            }
            upper in listOf("BIG5", "BIG-5", "BIG5-HKSCS", "WINDOWS-950") -> {
                try {
                    Charset.forName("Big5")
                } catch (_: Exception) {
                    StandardCharsets.UTF_8
                }
            }
            upper in listOf("UTF-8", "UTF8") -> StandardCharsets.UTF_8
            upper in listOf("UTF-16LE", "UTF-16", "UNICODE") -> StandardCharsets.UTF_16LE
            upper in listOf("UTF-16BE") -> StandardCharsets.UTF_16BE
            upper in listOf("US-ASCII", "ASCII") -> StandardCharsets.US_ASCII
            upper in listOf("ISO-8859-1", "ISO_8859_1", "WINDOWS-1252") -> StandardCharsets.ISO_8859_1
            else -> {
                try {
                    Charset.forName(rawName)
                } catch (_: Exception) {
                    StandardCharsets.UTF_8
                }
            }
        }
    }

    private fun getChineseCharset(): Charset {
        return try {
            if (Charset.isSupported("GB18030")) {
                Charset.forName("GB18030")
            } else if (Charset.isSupported("GBK")) {
                Charset.forName("GBK")
            } else {
                Charset.forName("GB2312")
            }
        } catch (_: Exception) {
            StandardCharsets.UTF_8
        }
    }

    private data class Utf8CheckResult(val isValidUtf8: Boolean, val hasNonAscii: Boolean)

    /**
     * 校验字节流是否符合 UTF-8 编码规范。
     */
    private fun checkUtf8Validity(bytes: ByteArray): Utf8CheckResult {
        var i = 0
        var hasNonAscii = false
        val len = bytes.size

        while (i < len) {
            val b = bytes[i].toInt() and 0xFF
            if (b < 0x80) {
                // 1 字节 ASCII: 0xxxxxxx
                i++
            } else if (b in 0xC2..0xDF) {
                // 2 字节: 110xxxxx 10xxxxxx
                if (i + 1 >= len) break
                val b2 = bytes[i + 1].toInt() and 0xFF
                if (b2 !in 0x80..0xBF) return Utf8CheckResult(false, hasNonAscii = true)
                hasNonAscii = true
                i += 2
            } else if (b in 0xE0..0xEF) {
                // 3 字节: 1110xxxx 10xxxxxx 10xxxxxx
                if (i + 2 >= len) break
                val b2 = bytes[i + 1].toInt() and 0xFF
                val b3 = bytes[i + 2].toInt() and 0xFF
                if (b == 0xE0 && b2 !in 0xA0..0xBF) return Utf8CheckResult(false, hasNonAscii = true)
                if (b == 0xED && b2 !in 0x80..0x9F) return Utf8CheckResult(false, hasNonAscii = true) // 避免 surrogate
                if (b2 !in 0x80..0xBF || b3 !in 0x80..0xBF) return Utf8CheckResult(false, hasNonAscii = true)
                hasNonAscii = true
                i += 3
            } else if (b in 0xF0..0xF4) {
                // 4 字节: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                if (i + 3 >= len) break
                val b2 = bytes[i + 1].toInt() and 0xFF
                val b3 = bytes[i + 2].toInt() and 0xFF
                val b4 = bytes[i + 3].toInt() and 0xFF
                if (b == 0xF0 && b2 !in 0x90..0xBF) return Utf8CheckResult(false, hasNonAscii = true)
                if (b == 0xF4 && b2 !in 0x80..0x8F) return Utf8CheckResult(false, hasNonAscii = true)
                if (b2 !in 0x80..0xBF || b3 !in 0x80..0xBF || b4 !in 0x80..0xBF) {
                    return Utf8CheckResult(false, hasNonAscii = true)
                }
                hasNonAscii = true
                i += 4
            } else {
                // 非法 UTF-8 起始字节
                return Utf8CheckResult(false, hasNonAscii = true)
            }
        }

        return Utf8CheckResult(true, hasNonAscii)
    }
}
