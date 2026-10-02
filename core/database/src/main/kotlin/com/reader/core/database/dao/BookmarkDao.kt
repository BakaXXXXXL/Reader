package com.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reader.core.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书签数据访问接口 (DAO)。
 */
@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE book_id = :bookId ORDER BY chapter_index ASC, char_offset ASC")
    fun getBookmarksFlow(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE book_id = :bookId ORDER BY chapter_index ASC, char_offset ASC")
    suspend fun getBookmarks(bookId: Long): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE book_id = :bookId")
    suspend fun deleteBookmarksByBookId(bookId: Long)
}
