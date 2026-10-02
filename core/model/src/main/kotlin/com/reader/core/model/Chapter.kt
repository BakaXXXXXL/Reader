package com.reader.core.model

/**
 * 章节领域模型。
 *
 * @property id 唯一主键标识 (0 表示未持久化新增对象)
 * @property bookId 所属书籍 ID
 * @property index 章节序号 (从 0 开始自增)
 * @property title 章节标题
 * @property startOffset 在源文本/文件流中的起始字节或字符偏移
 * @property endOffset 在源文本/文件流中的结束字节或字符偏移
 * @property contentPath EPUB 等归档格式中的相对文件路径 (如 "OEBPS/Text/ch01.xhtml")
 */
data class Chapter(
    val id: Long = 0L,
    val bookId: Long,
    val index: Int,
    val title: String,
    val startOffset: Long,
    val endOffset: Long,
    val contentPath: String? = null
) {
    /** 章节有效长度 (偏移区间) */
    val length: Long get() = (endOffset - startOffset).coerceAtLeast(0L)
}
