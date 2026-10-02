package com.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reader.core.database.entity.ReadLocatorEntity
import kotlinx.coroutines.flow.Flow

/**
 * 统一定位器数据访问接口 (DAO)。
 */
@Dao
interface ReadLocatorDao {

    @Query("SELECT * FROM read_locators WHERE book_id = :bookId LIMIT 1")
    fun getLocatorFlow(bookId: Long): Flow<ReadLocatorEntity?>

    @Query("SELECT * FROM read_locators WHERE book_id = :bookId LIMIT 1")
    suspend fun getLocator(bookId: Long): ReadLocatorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocator(locator: ReadLocatorEntity)

    @Query("DELETE FROM read_locators WHERE book_id = :bookId")
    suspend fun deleteLocatorByBookId(bookId: Long)
}
