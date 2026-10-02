package com.reader.core.common.io

import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset

/**
 * 高性能 NIO 文件安全读取器。
 *
 * 铁律约束：严禁全量读入内存 (如 readText())，通过底层 [FileChannel]、分块缓冲区与内存映射 [MappedByteBuffer]
 * 支撑 GB 级大文件的毫秒级随机寻址与流式分块读取。
 */
class NioFileReader private constructor(
    private val randomAccessFile: RandomAccessFile,
    val fileChannel: FileChannel,
    val filePath: String
) : Closeable {

    val fileSize: Long get() = fileChannel.size()

    /**
     * 在指定偏移量处读取固定长度字节片段。
     * 适合用于章节定位、段落流式解码与页面切割。
     *
     * @param startOffset 文件起始偏移量 (0-indexed)
     * @param length 欲读取字节数 (自动与剩余大小对齐)
     * @return 实际读取到的字节数组
     */
    fun readBytes(startOffset: Long, length: Int): ByteArray {
        val totalSize = fileSize
        if (startOffset >= totalSize || length <= 0) {
            return ByteArray(0)
        }
        val safeOffset = startOffset.coerceAtLeast(0L)
        val safeLength = length.toLong().coerceAtMost(totalSize - safeOffset).toInt()
        if (safeLength <= 0) return ByteArray(0)

        val buffer = ByteBuffer.allocate(safeLength)
        var totalRead = 0
        var currentPosition = safeOffset

        while (totalRead < safeLength) {
            val bytesRead = fileChannel.read(buffer, currentPosition)
            if (bytesRead == -1) break
            totalRead += bytesRead
            currentPosition += bytesRead
        }

        buffer.flip()
        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        return result
    }

    /**
     * 在指定偏移量处以特定字符编码读取文本片段。
     *
     * @param startOffset 文件起始偏移量
     * @param length 字节长度
     * @param charset 目标字符集 (默认 UTF-8)
     */
    fun readTextRange(
        startOffset: Long,
        length: Int,
        charset: Charset = Charsets.UTF_8
    ): String {
        val bytes = readBytes(startOffset, length)
        return if (bytes.isEmpty()) "" else String(bytes, charset)
    }

    /**
     * 提取头部样本字节（默认最多 4KB），用于编码探测 (如 juniversalchardet) 或文件头 Magic Number 校验，
     * 避免大文件被一次性拉入内存。
     */
    fun readSampleBytes(maxBytes: Int = DEFAULT_SAMPLE_SIZE): ByteArray {
        return readBytes(0L, maxBytes)
    }

    /**
     * 安全创建内存映射只读缓冲区 (Memory-Mapped Buffer)。
     * 仅用于只读模式，对 TXT 超大文件秒级分章搜索极度友好。
     *
     * @param position 映射起始位置
     * @param size 映射尺寸 (字节数，单个 MappedByteBuffer 不能超过 2GB)
     */
    fun mapReadOnly(position: Long, size: Long): MappedByteBuffer {
        val channelSize = fileChannel.size()
        require(position in 0..channelSize) {
            "Position $position is out of bounds (file size: $channelSize)"
        }
        val safeSize = size.coerceAtMost(channelSize - position)
        require(safeSize in 0..Int.MAX_VALUE.toLong()) {
            "Mapped size must not exceed Int.MAX_VALUE bytes: $safeSize"
        }
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, position, safeSize)
    }

    /**
     * 沿文件流流式分块迭代读取。
     *
     * @param chunkSize 每次迭代的缓冲区大小，默认 64KB
     * @param startOffset 起始偏移
     * @param action 回调函数：(offset, bytes, readLength) -> 是否继续读取 (返回 false 则提前中断)
     */
    fun forEachChunk(
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        startOffset: Long = 0L,
        action: (offset: Long, buffer: ByteArray, readCount: Int) -> Boolean
    ) {
        val buffer = ByteBuffer.allocate(chunkSize)
        var offset = startOffset.coerceAtLeast(0L)
        val totalSize = fileSize

        while (offset < totalSize) {
            buffer.clear()
            val readCount = fileChannel.read(buffer, offset)
            if (readCount <= 0) break

            buffer.flip()
            val chunk = ByteArray(readCount)
            buffer.get(chunk)

            val shouldContinue = action(offset, chunk, readCount)
            if (!shouldContinue) break

            offset += readCount
        }
    }

    override fun close() {
        try {
            fileChannel.close()
        } finally {
            randomAccessFile.close()
        }
    }

    companion object {
        const val DEFAULT_CHUNK_SIZE = 64 * 1024 // 64 KB
        const val DEFAULT_SAMPLE_SIZE = 4 * 1024 // 4 KB (用于编码嗅探)

        /**
         * 基于目标文件安全开启只读 [NioFileReader]。
         */
        fun open(file: File): NioFileReader {
            if (!file.exists()) {
                throw FileNotFoundException("File does not exist: ${file.absolutePath}")
            }
            if (file.isDirectory) {
                throw IllegalArgumentException("Target is a directory, not a file: ${file.absolutePath}")
            }
            val raf = RandomAccessFile(file, "r")
            val channel = raf.channel
            return NioFileReader(raf, channel, file.absolutePath)
        }

        /**
         * 基于文件路径开启只读 [NioFileReader]。
         */
        fun open(filePath: String): NioFileReader {
            return open(File(filePath))
        }
    }
}
