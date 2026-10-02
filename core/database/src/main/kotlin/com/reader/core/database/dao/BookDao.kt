package com.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reader.core.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书籍数据访问接口 (DAO)。
 */
@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY last_read_time DESC")
    fun getAllBooksFlow(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY last_read_time DESC")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun getBookFlow(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE uri_string = :uriString LIMIT 1")
    suspend fun getBookByUri(uriString: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>): List<Long>

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("UPDATE books SET last_read_time = :time WHERE id = :id")
    suspend fun updateLastReadTime(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE books SET total_chapters = :total WHERE id = :id")
    suspend fun updateTotalChapters(id: Long, total: Int)

    @Query("UPDATE books SET cover_path = :coverPath WHERE id = :id")
    suspend fun updateCoverPath(id: Long, coverPath: String?)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Query("SELECT COUNT(*) FROM books")
    fun getBookCountFlow(): Flow<Int>
}
