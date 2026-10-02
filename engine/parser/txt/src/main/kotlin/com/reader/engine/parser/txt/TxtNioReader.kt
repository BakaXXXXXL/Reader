package com.reader.engine.parser.txt

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * 文本行解析元数据。
 *
 * @property text 解码后的行文本 (不包含末尾的换行符 \r, \n)
 * @property startByteOffset 该行在文件中的起始绝对字节偏移
 * @property endByteOffset 该行包含换行符在内的结束绝对字节偏移
 */
data class TxtLine(
    val text: String,
    val startByteOffset: Long,
    val endByteOffset: Long
)

/**
 * 基于 NIO FileChannel / RandomAccessFile 的大文件分块流式读取器。
 *
 * 核心特性：
 * 1. 严格杜绝全量加载到内存，单次仅使用固定大小的轻量缓冲区 (默认 64KB - 128KB)，即使读取 1GB+ 巨型文本也仅占用毫秒级与常量级内存；
 * 2. 精确维护每个文本行的起始字节偏移 (startByteOffset) 与结束字节偏移 (endByteOffset)，为秒级分章与随机快速 Seek 提供基石；
 * 3. 完美处理跨分块边界的换行符与多字节字符截断问题；
 * 4. 支持 UTF-8, GBK, GB18030, Big5, UTF-16LE, UTF-16BE 等多种字符集。
 */
class TxtNioReader(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) {

    companion object {
        const val DEFAULT_BUFFER_SIZE = 128 * 1024 // 128 KB
        private const val LF: Byte = 0x0A
        private const val CR: Byte = 0x0D
    }

    /**
     * 流式扫描文件中的每一行。
     *
     * @param file 目标文本文件
     * @param charset 文本编码
     * @param startOffset 开始扫描的字节偏移 (默认为 0L，若有 BOM 则传入 BOM 长度)
     * @param maxOffset 最大扫描字节偏移 (默认文件末尾)
     * @param onLine 每解析出一行时的回调。返回 true 继续扫描，返回 false 提前终止扫描
     */
    fun forEachLine(
        file: File,
        charset: Charset,
        startOffset: Long = 0L,
        maxOffset: Long = file.length(),
        onLine: (TxtLine) -> Boolean
    ) {
        if (!file.exists() || file.length() == 0L || startOffset >= maxOffset) {
            return
        }

        RandomAccessFile(file, "r").use { raf ->
            val channel = raf.channel
            forEachLine(channel, charset, startOffset, maxOffset, onLine)
        }
    }

    /**
     * 基于 FileChannel 流式扫描每一行。
     */
    fun forEachLine(
        channel: FileChannel,
        charset: Charset,
        startOffset: Long = 0L,
        maxOffset: Long = channel.size(),
        onLine: (TxtLine) -> Boolean
    ) {
        val fileSize = channel.size()
        val limitOffset = minOf(maxOffset, fileSize)
        if (startOffset >= limitOffset) return

        val isUtf16 = charset == StandardCharsets.UTF_16LE || charset == StandardCharsets.UTF_16BE
        if (isUtf16) {
            forEachLineUtf16(channel, charset, startOffset, limitOffset, onLine)
            return
        }

        forEachLineSingleByteNewline(channel, charset, startOffset, limitOffset, onLine)
    }

    /**
     * 针对 UTF-8, GBK, GB18030, Big5, ASCII, ISO-8859-1 等换行符为单字节 (0x0A) 的编码进行极速扫描。
     */
    private fun forEachLineSingleByteNewline(
        channel: FileChannel,
        charset: Charset,
        startOffset: Long,
        limitOffset: Long,
        onLine: (TxtLine) -> Boolean
    ) {
        channel.position(startOffset)
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
        val lineBytesAccumulator = ByteArrayOutputStream(256)

        var currentFileOffset = startOffset
        var currentLineStartOffset = startOffset

        while (currentFileOffset < limitOffset) {
            byteBuffer.clear()
            val remainingToRead = minOf(bufferSize.toLong(), limitOffset - currentFileOffset).toInt()
            byteBuffer.limit(remainingToRead)

            val bytesRead = channel.read(byteBuffer)
            if (bytesRead <= 0) break

            byteBuffer.flip()
            var chunkIndex = 0

            while (chunkIndex < bytesRead) {
                val b = byteBuffer.get(chunkIndex)
                if (b == LF) {
                    // 找到换行符 LF (0x0A)
                    val lineEndOffset = currentFileOffset + chunkIndex + 1
                    val lineBytes = lineBytesAccumulator.toByteArray()
                    lineBytesAccumulator.reset()

                    val lineStr = decodeCleanString(lineBytes, charset)
                    val line = TxtLine(
                        text = lineStr,
                        startByteOffset = currentLineStartOffset,
                        endByteOffset = lineEndOffset
                    )

                    val continueScan = onLine(line)
                    currentLineStartOffset = lineEndOffset

                    if (!continueScan) {
                        return
                    }
                } else if (b != CR) {
                    // 非 CR (\r) 的普通字节，累加到行缓冲区
                    lineBytesAccumulator.write(b.toInt())
                }
                chunkIndex++
            }

            currentFileOffset += bytesRead
        }

        // 文件末尾残留内容 (最后一行没有以 \n 结尾)
        if (lineBytesAccumulator.size() > 0 || currentLineStartOffset < limitOffset) {
            val lineBytes = lineBytesAccumulator.toByteArray()
            val lineStr = decodeCleanString(lineBytes, charset)
            val line = TxtLine(
                text = lineStr,
                startByteOffset = currentLineStartOffset,
                endByteOffset = limitOffset
            )
            onLine(line)
        }
    }

    /**
     * 针对 UTF-16LE / UTF-16BE (换行符为双字节) 的扫描。
     */
    private fun forEachLineUtf16(
        channel: FileChannel,
        charset: Charset,
        startOffset: Long,
        limitOffset: Long,
        onLine: (TxtLine) -> Boolean
    ) {
        val isLe = charset == StandardCharsets.UTF_16LE
        channel.position(startOffset)
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
        val lineBytesAccumulator = ByteArrayOutputStream(256)

        var currentFileOffset = startOffset
        var currentLineStartOffset = startOffset

        while (currentFileOffset < limitOffset) {
            byteBuffer.clear()
            val remainingToRead = minOf(bufferSize.toLong(), limitOffset - currentFileOffset).toInt()
            byteBuffer.limit(remainingToRead)

            val bytesRead = channel.read(byteBuffer)
            if (bytesRead <= 1) break

            byteBuffer.flip()
            var chunkIndex = 0

            while (chunkIndex + 1 < bytesRead) {
                val b1 = byteBuffer.get(chunkIndex)
                val b2 = byteBuffer.get(chunkIndex + 1)

                val isLf = if (isLe) (b1 == LF && b2 == 0.toByte()) else (b1 == 0.toByte() && b2 == LF)
                val isCr = if (isLe) (b1 == CR && b2 == 0.toByte()) else (b1 == 0.toByte() && b2 == CR)

                if (isLf) {
                    val lineEndOffset = currentFileOffset + chunkIndex + 2
                    val lineBytes = lineBytesAccumulator.toByteArray()
                    lineBytesAccumulator.reset()

                    val lineStr = decodeCleanString(lineBytes, charset)
                    val line = TxtLine(
                        text = lineStr,
                        startByteOffset = currentLineStartOffset,
                        endByteOffset = lineEndOffset
                    )

                    val continueScan = onLine(line)
                    currentLineStartOffset = lineEndOffset

                    if (!continueScan) return
                } else if (!isCr) {
                    lineBytesAccumulator.write(b1.toInt())
                    lineBytesAccumulator.write(b2.toInt())
                }
                chunkIndex += 2
            }

            currentFileOffset += bytesRead
        }

        if (lineBytesAccumulator.size() > 0 || currentLineStartOffset < limitOffset) {
            val lineBytes = lineBytesAccumulator.toByteArray()
            val lineStr = decodeCleanString(lineBytes, charset)
            val line = TxtLine(
                text = lineStr,
                startByteOffset = currentLineStartOffset,
                endByteOffset = limitOffset
            )
            onLine(line)
        }
    }

    /**
     * 挂起流式扫描文件中的每一行，允许在回调中执行挂起操作 (如 Flow 的 emit)。
     */
    suspend fun forEachLineSuspend(
        file: File,
        charset: Charset,
        startOffset: Long = 0L,
        maxOffset: Long = file.length(),
        onLine: suspend (TxtLine) -> Boolean
    ) {
        if (!file.exists() || file.length() == 0L || startOffset >= maxOffset) {
            return
        }

        RandomAccessFile(file, "r").use { raf ->
            val channel = raf.channel
            forEachLineSuspend(channel, charset, startOffset, maxOffset, onLine)
        }
    }

    /**
     * 基于 FileChannel 挂起扫描每一行。
     */
    suspend fun forEachLineSuspend(
        channel: FileChannel,
        charset: Charset,
        startOffset: Long = 0L,
        maxOffset: Long = channel.size(),
        onLine: suspend (TxtLine) -> Boolean
    ) {
        val fileSize = channel.size()
        val limitOffset = minOf(maxOffset, fileSize)
        if (startOffset >= limitOffset) return

        val isUtf16 = charset == StandardCharsets.UTF_16LE || charset == StandardCharsets.UTF_16BE
        if (isUtf16) {
            forEachLineUtf16Suspend(channel, charset, startOffset, limitOffset, onLine)
            return
        }

        forEachLineSingleByteNewlineSuspend(channel, charset, startOffset, limitOffset, onLine)
    }

    private suspend fun forEachLineSingleByteNewlineSuspend(
        channel: FileChannel,
        charset: Charset,
        startOffset: Long,
        limitOffset: Long,
        onLine: suspend (TxtLine) -> Boolean
    ) {
        channel.position(startOffset)
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
        val lineBytesAccumulator = ByteArrayOutputStream(256)

        var currentFileOffset = startOffset
        var currentLineStartOffset = startOffset

        while (currentFileOffset < limitOffset) {
            byteBuffer.clear()
            val remainingToRead = minOf(bufferSize.toLong(), limitOffset - currentFileOffset).toInt()
            byteBuffer.limit(remainingToRead)

            val bytesRead = channel.read(byteBuffer)
            if (bytesRead <= 0) break

            byteBuffer.flip()
            var chunkIndex = 0

            while (chunkIndex < bytesRead) {
                val b = byteBuffer.get(chunkIndex)
                if (b == LF) {
                    val lineEndOffset = currentFileOffset + chunkIndex + 1
                    val lineBytes = lineBytesAccumulator.toByteArray()
                    lineBytesAccumulator.reset()

                    val lineStr = decodeCleanString(lineBytes, charset)
                    val line = TxtLine(
                        text = lineStr,
                        startByteOffset = currentLineStartOffset,
                        endByteOffset = lineEndOffset
                    )

                    val continueScan = onLine(line)
                    currentLineStartOffset = lineEndOffset

                    if (!continueScan) {
                        return
                    }
                } else if (b != CR) {
                    lineBytesAccumulator.write(b.toInt())
                }
                chunkIndex++
            }

            currentFileOffset += bytesRead
        }

        if (lineBytesAccumulator.size() > 0 || currentLineStartOffset < limitOffset) {
            val lineBytes = lineBytesAccumulator.toByteArray()
            val lineStr = decodeCleanString(lineBytes, charset)
            val line = TxtLine(
                text = lineStr,
                startByteOffset = currentLineStartOffset,
                endByteOffset = limitOffset
            )
            onLine(line)
        }
    }

    private suspend fun forEachLineUtf16Suspend(
        channel: FileChannel,
        charset: Charset,
        startOffset: Long,
        limitOffset: Long,
        onLine: suspend (TxtLine) -> Boolean
    ) {
        val isLe = charset == StandardCharsets.UTF_16LE
        channel.position(startOffset)
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
        val lineBytesAccumulator = ByteArrayOutputStream(256)

        var currentFileOffset = startOffset
        var currentLineStartOffset = startOffset

        while (currentFileOffset < limitOffset) {
            byteBuffer.clear()
            val remainingToRead = minOf(bufferSize.toLong(), limitOffset - currentFileOffset).toInt()
            byteBuffer.limit(remainingToRead)

            val bytesRead = channel.read(byteBuffer)
            if (bytesRead <= 1) break

            byteBuffer.flip()
            var chunkIndex = 0

            while (chunkIndex + 1 < bytesRead) {
                val b1 = byteBuffer.get(chunkIndex)
                val b2 = byteBuffer.get(chunkIndex + 1)

                val isLf = if (isLe) (b1 == LF && b2 == 0.toByte()) else (b1 == 0.toByte() && b2 == LF)
                val isCr = if (isLe) (b1 == CR && b2 == 0.toByte()) else (b1 == 0.toByte() && b2 == CR)

                if (isLf) {
                    val lineEndOffset = currentFileOffset + chunkIndex + 2
                    val lineBytes = lineBytesAccumulator.toByteArray()
                    lineBytesAccumulator.reset()

                    val lineStr = decodeCleanString(lineBytes, charset)
                    val line = TxtLine(
                        text = lineStr,
                        startByteOffset = currentLineStartOffset,
                        endByteOffset = lineEndOffset
                    )

                    val continueScan = onLine(line)
                    currentLineStartOffset = lineEndOffset

                    if (!continueScan) return
                } else if (!isCr) {
                    lineBytesAccumulator.write(b1.toInt())
                    lineBytesAccumulator.write(b2.toInt())
                }
                chunkIndex += 2
            }

            currentFileOffset += bytesRead
        }

        if (lineBytesAccumulator.size() > 0 || currentLineStartOffset < limitOffset) {
            val lineBytes = lineBytesAccumulator.toByteArray()
            val lineStr = decodeCleanString(lineBytes, charset)
            val line = TxtLine(
                text = lineStr,
                startByteOffset = currentLineStartOffset,
                endByteOffset = limitOffset
            )
            onLine(line)
        }
    }

    /**
     * 安全读取指定字节区间的原始字节并解码为字符串。
     *
     * @param file 目标文件
     * @param startOffset 起始字节偏移
     * @param length 读取字节长度
     * @param charset 编码
     * @return 解码后的字符串
     */
    fun readString(file: File, startOffset: Long, length: Int, charset: Charset): String {
        if (!file.exists() || length <= 0) return ""
        val bytes = readBytes(file, startOffset, length)
        return decodeCleanString(bytes, charset)
    }

    /**
     * 从文件中读取指定偏移与长度的原始字节。
     */
    fun readBytes(file: File, startOffset: Long, length: Int): ByteArray {
        if (!file.exists() || length <= 0) return ByteArray(0)
        val fileLength = file.length()
        if (startOffset >= fileLength) return ByteArray(0)

        val actualLength = minOf(length.toLong(), fileLength - startOffset).toInt()
        val result = ByteArray(actualLength)

        RandomAccessFile(file, "r").use { raf ->
            raf.channel.use { channel ->
                channel.position(startOffset)
                val buffer = ByteBuffer.wrap(result)
                var total = 0
                while (total < actualLength) {
                    val read = channel.read(buffer)
                    if (read <= 0) break
                    total += read
                }
            }
        }
        return result
    }

    private fun decodeCleanString(bytes: ByteArray, charset: Charset): String {
        if (bytes.isEmpty()) return ""
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: Exception) {
            String(bytes, charset)
        }
    }
}
