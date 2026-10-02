package com.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reader.core.database.entity.ReadingLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * 阅读统计流水数据访问接口 (DAO)。
 */
@Dao
interface ReadingLogDao {

    @Query("SELECT * FROM reading_logs WHERE book_id = :bookId ORDER BY timestamp DESC")
    fun getLogsByBookFlow(bookId: Long): Flow<List<ReadingLogEntity>>

    @Query("SELECT * FROM reading_logs WHERE read_date = :date ORDER BY timestamp DESC")
    fun getLogsByDateFlow(date: String): Flow<List<ReadingLogEntity>>

    @Query("SELECT * FROM reading_logs WHERE read_date = :date ORDER BY timestamp DESC")
    suspend fun getLogsByDate(date: String): List<ReadingLogEntity>

    @Query("SELECT COALESCE(SUM(duration_seconds), 0) FROM reading_logs WHERE read_date = :date")
    fun getTotalDurationByDateFlow(date: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(duration_seconds), 0) FROM reading_logs WHERE read_date = :date")
    suspend fun getTotalDurationByDate(date: String): Long

    @Query("SELECT COALESCE(SUM(duration_seconds), 0) FROM reading_logs")
    fun getTotalReadingDurationFlow(): Flow<Long>

    @Query("SELECT COALESCE(SUM(duration_seconds), 0) FROM reading_logs")
    suspend fun getTotalReadingDuration(): Long

    @Query("SELECT COALESCE(SUM(characters_read), 0) FROM reading_logs")
    fun getTotalCharactersReadFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(characters_read), 0) FROM reading_logs")
    suspend fun getTotalCharactersRead(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ReadingLogEntity): Long

    @Query("DELETE FROM reading_logs WHERE book_id = :bookId")
    suspend fun deleteLogsByBookId(bookId: Long)
}
