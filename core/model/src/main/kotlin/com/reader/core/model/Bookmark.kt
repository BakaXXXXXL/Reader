package com.reader.core.model

/**
 * 书签模型。
 * 记录用户手动标记的精彩段落或特定位置。
 *
 * @property id 唯一书签 ID (0 表示新增)
 * @property bookId 所属书籍 ID
 * @property chapterIndex 章节序号
 * @property chapterTitle 章节标题
 * @property charOffset 章节内字符偏移量
 * @property previewText 书签上下文预览摘录文本
 * @property createTime 创建时间戳 (毫秒)
 */
data class Bookmark(
    val id: Long = 0L,
    val bookId: Long,
    val chapterIndex: Int,
    val chapterTitle: String,
    val charOffset: Int,
    val previewText: String,
    val createTime: Long = System.currentTimeMillis()
)
