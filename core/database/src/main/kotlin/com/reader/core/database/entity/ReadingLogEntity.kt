package com.reader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 阅读统计流水数据库持久化实体。
 * 表名: reading_logs
 * 用于统计每日阅读时长、字数与热力图。
 */
@Entity(
    tableName = "reading_logs",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["read_date"])
    ]
)
data class ReadingLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "book_id")
    val bookId: Long,

    @ColumnInfo(name = "read_date")
    val readDate: String,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long = 0L,

    @ColumnInfo(name = "characters_read")
    val charactersRead: Int = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
