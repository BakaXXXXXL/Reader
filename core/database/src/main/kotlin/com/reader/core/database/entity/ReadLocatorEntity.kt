package com.reader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 统一定位器数据库持久化实体。
 * 表名: read_locators
 * 每本书籍唯一映射当前最后一次阅读进度。
 */
@Entity(
    tableName = "read_locators",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReadLocatorEntity(
    @PrimaryKey
    @ColumnInfo(name = "book_id")
    val bookId: Long,

    @ColumnInfo(name = "chapter_index")
    val chapterIndex: Int,

    @ColumnInfo(name = "chapter_title")
    val chapterTitle: String,

    @ColumnInfo(name = "char_offset")
    val charOffset: Int,

    @ColumnInfo(name = "progression")
    val progression: Float,

    @ColumnInfo(name = "page_index_in_chapter")
    val pageIndexInChapter: Int = 0,

    @ColumnInfo(name = "total_pages_in_chapter")
    val totalPagesInChapter: Int = 1,

    @ColumnInfo(name = "update_time")
    val updateTime: Long = System.currentTimeMillis()
)
