package com.reader.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.reader.core.database.converter.Converters
import com.reader.core.database.dao.BookmarkDao
import com.reader.core.database.dao.BookDao
import com.reader.core.database.dao.ChapterDao
import com.reader.core.database.dao.ReadLocatorDao
import com.reader.core.database.dao.ReadingLogDao
import com.reader.core.database.entity.BookmarkEntity
import com.reader.core.database.entity.BookEntity
import com.reader.core.database.entity.ChapterEntity
import com.reader.core.database.entity.ReadLocatorEntity
import com.reader.core.database.entity.ReadingLogEntity

/**
 * 应用主 Room 数据库。
 *
 * 包含表结构：
 * - books: 书架藏书及文件元数据
 * - chapters: 章节目录索引与物理/字符偏移
 * - read_locators: 每本书的阅读统一定位进度
 * - reading_logs: 每日阅读时长与字数统计流水
 * - bookmarks: 用户标记的书签
 */
@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        ReadLocatorEntity::class,
        ReadingLogEntity::class,
        BookmarkEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReaderDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun readLocatorDao(): ReadLocatorDao
    abstract fun readingLogDao(): ReadingLogDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        const val DATABASE_NAME = "reader_app.db"

        /**
         * 构建数据库实例。
         *
         * @param context Android 上下文
         * @param inMemory 是否为内存数据库 (主要用于单元测试与调试验证)
         */
        fun build(context: Context, inMemory: Boolean = false): ReaderDatabase {
            return if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, ReaderDatabase::class.java)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            } else {
                Room.databaseBuilder(context, ReaderDatabase::class.java, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }
    }
}
