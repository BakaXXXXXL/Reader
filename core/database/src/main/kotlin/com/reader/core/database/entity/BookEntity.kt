package com.reader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.reader.core.model.BookFormat

/**
 * 书籍数据库持久化实体。
 * 表名: books
 */
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["uri_string"], unique = true),
        Index(value = ["last_read_time"])
    ]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "author")
    val author: String = "未知作者",

    @ColumnInfo(name = "cover_path")
    val coverPath: String? = null,

    @ColumnInfo(name = "uri_string")
    val uriString: String,

    @ColumnInfo(name = "format")
    val format: BookFormat,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "total_chapters")
    val totalChapters: Int = 0,

    @ColumnInfo(name = "add_time")
    val addTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_read_time")
    val lastReadTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "archive_path")
    val archivePath: String? = null
)
