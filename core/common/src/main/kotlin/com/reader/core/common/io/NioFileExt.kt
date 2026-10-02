package com.reader.core.common.io

import java.io.File
import java.nio.ByteBuffer

/**
 * 文件与 NIO 扩展函数集。
 */

/**
 * 安全使用 [NioFileReader]，并在操作完成后自动释放通道与文件句柄。
 */
inline fun <R> File.useNioReader(block: (NioFileReader) -> R): R {
    return NioFileReader.open(this).use(block)
}

/**
 * 安全获取文件大小，若文件不存在则返回 0L。
 */
fun File?.safeLength(): Long {
    return if (this != null && exists() && isFile) length() else 0L
}

/**
 * 读取文件前 4KB 样本数据，专门用于编码探测或格式特征校验。
 */
fun File.readSampleBytes(maxBytes: Int = NioFileReader.DEFAULT_SAMPLE_SIZE): ByteArray {
    return useNioReader { it.readSampleBytes(maxBytes) }
}

/**
 * 将 [ByteBuffer] 内剩余字节导出为 [ByteArray]。
 */
fun ByteBuffer.toByteArray(): ByteArray {
    val array = ByteArray(remaining())
    get(array)
    return array
}
