package com.reader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 章节数据库持久化实体。
 * 表名: chapters
 * 关联父表 books，书籍删除时级联删除其下所有章节。
 */
@Entity(
    tableName = "chapters",
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
        Index(value = ["book_id", "chapter_index"], unique = true)
    ]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "book_id")
    val bookId: Long,

    @ColumnInfo(name = "chapter_index")
    val index: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "start_offset")
    val startOffset: Long,

    @ColumnInfo(name = "end_offset")
    val endOffset: Long,

    @ColumnInfo(name = "content_path")
    val contentPath: String? = null
)
