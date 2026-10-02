package com.reader.feature.bookshelf.model

import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

/**
 * 书架 UI 领域聚合模型。
 * 包装 [Book] 核心实体并聚合阅读进度、收藏标星、自定义置顶排序等展示状态。
 *
 * @property book 核心书籍模型
 * @property readingProgress 阅读进度比例 (0.0f ~ 1.0f)
 * @property isFavorite 是否已加入收藏
 * @property isPinned 是否已置顶
 * @property customOrder 手动拖拽排序序号
 * @property lastReadChapterTitle 最近一次阅读的章节标题快照
 */
data class BookshelfItem(
    val book: Book,
    val readingProgress: Float = 0.0f,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val customOrder: Int = 0,
    val lastReadChapterTitle: String? = null
) {
    val id: Long get() = book.id
    val title: String get() = book.title
    val author: String get() = book.author
    val coverPath: String? get() = book.coverPath
    val uriString: String get() = book.uriString
    val format: BookFormat get() = book.format
    val fileSize: Long get() = book.fileSize
    val totalChapters: Int get() = book.totalChapters
    val addTime: Long get() = book.addTime
    val lastReadTime: Long get() = book.lastReadTime

    /** 是否已读完 (进度 >= 99% 视为已读完) */
    val isCompleted: Boolean get() = readingProgress >= 0.99f

    /** 是否正在阅读中 (有进度但未读完) */
    val isReading: Boolean get() = readingProgress > 0.0f && !isCompleted

    /** 格式化后的进度文字 (例如 "未读", "42%", "已读完") */
    val progressText: String
        get() {
            return when {
                readingProgress <= 0.001f -> "未读"
                readingProgress >= 0.99f -> "已读完"
                else -> {
                    val percent = (readingProgress * 100f).toInt().coerceIn(1, 99)
                    "已读 $percent%"
                }
            }
        }

    /** 规范化进度百分比 (0f ~ 100f) */
    val progressPercentage: Float
        get() = (readingProgress.coerceIn(0.0f, 1.0f) * 100.0f)

    /** 格式化文件大小 (B, KB, MB, GB) */
    val formattedFileSize: String
        get() {
            if (fileSize <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (log10(fileSize.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = fileSize / 1024.0.pow(digitGroups.toDouble())
            return if (digitGroups == 0) {
                "${fileSize} B"
            } else {
                String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
            }
        }

    /** 格式化最近阅读时间相对文本 */
    val formattedLastReadTime: String
        get() = formatRelativeTime(lastReadTime)

    companion object {
        fun formatRelativeTime(timestamp: Long): String {
            if (timestamp <= 0L) return "从未阅读"
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000L -> "刚刚"
                diff < 3600_000L -> "${diff / 60_000L} 分钟前"
                diff < 86400_000L -> "${diff / 3600_000L} 小时前"
                diff < 172800_000L -> "昨天"
                diff < 604800_000L -> "${diff / 86400_000L} 天前"
                else -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}
