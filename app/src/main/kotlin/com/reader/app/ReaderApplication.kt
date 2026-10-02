package com.reader.app

import android.app.Application
import com.reader.core.database.ReaderDatabase
import com.reader.core.datastore.ReaderPreferencesDataStore

/**
 * 全局 Application 实例。
 * 负责全局离线数据库与用户偏好持久化管理。
 */
class ReaderApplication : Application() {

    lateinit var database: ReaderDatabase
        private set

    lateinit var preferencesDataStore: ReaderPreferencesDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化离线 SQLite 数据库
        database = ReaderDatabase.getInstance(this)

        // 初始化用户排版与主题偏好存储
        preferencesDataStore = ReaderPreferencesDataStore(this)
    }

    companion object {
        lateinit var instance: ReaderApplication
            private set
    }
}
