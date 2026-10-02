package com.reader.app.navigation

/**
 * 全局导航路由定义。
 */
sealed interface AppRoute {
    /** 书架主页 */
    data object Bookshelf : AppRoute

    /**
     * 沉浸式阅读器主界面
     * @param bookId 当前正在阅读的书籍 ID
     * @param initialChapterIndex 可选的起始章节序号 (从 0 开始)
     */
    data class Reader(
        val bookId: Long,
        val initialChapterIndex: Int = 0
    ) : AppRoute
}
