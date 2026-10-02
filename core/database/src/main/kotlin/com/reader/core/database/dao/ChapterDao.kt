package com.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reader.core.database.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

/**
 * 章节数据访问接口 (DAO)。
 */
@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE book_id = :bookId ORDER BY chapter_index ASC")
    fun getChaptersFlow(bookId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE book_id = :bookId ORDER BY chapter_index ASC")
    suspend fun getChapters(bookId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE book_id = :bookId AND chapter_index = :index LIMIT 1")
    suspend fun getChapterByIndex(bookId: Long, index: Int): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE book_id = :bookId")
    suspend fun deleteChaptersByBookId(bookId: Long)

    @Query("SELECT COUNT(*) FROM chapters WHERE book_id = :bookId")
    suspend fun getChapterCount(bookId: Long): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE book_id = :bookId")
    fun getChapterCountFlow(bookId: Long): Flow<Int>
}
