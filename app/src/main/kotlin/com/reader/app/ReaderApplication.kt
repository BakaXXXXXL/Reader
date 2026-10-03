package com.reader.app

import android.app.Application
import com.reader.core.database.ReaderDatabase
import com.reader.core.datastore.ReaderPreferencesDataStore
import com.reader.feature.bookshelf.data.DefaultBookshelfRepository
import com.reader.feature.bookshelf.data.SampleBookInitializer
import com.reader.feature.bookshelf.viewmodel.BookshelfViewModel
import com.reader.feature.reader.ReaderViewModel
import com.reader.feature.reader.RoomReaderDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 全局 Application 实例。
 * 负责全局离线数据库与用户偏好持久化管理，并在初次启动时异步安全预装经典公版书籍。
 */
class ReaderApplication : Application() {

    lateinit var database: ReaderDatabase
        private set

    lateinit var preferencesDataStore: ReaderPreferencesDataStore
        private set

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化离线 SQLite 数据库
        database = ReaderDatabase.build(this)

        // 初始化用户排版与主题偏好存储
        preferencesDataStore = ReaderPreferencesDataStore(this)

        // 显式绑定生产级数据仓库提供者，彻底消除反射与内存假数据隐患
        BookshelfViewModel.defaultRepositoryProvider = {
            DefaultBookshelfRepository(
                database = database,
                context = this
            )
        }

        ReaderViewModel.defaultDataSourceProvider = { bookId ->
            RoomReaderDataSource(
                database = database,
                preferencesDataStore = preferencesDataStore
            )
        }

        // 异步确保初次安装时预置合规示例经典书籍
        applicationScope.launch {
            try {
                SampleBookInitializer.ensureSampleBooks(this@ReaderApplication, database)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        @JvmStatic
        lateinit var instance: ReaderApplication
            private set
    }
}
