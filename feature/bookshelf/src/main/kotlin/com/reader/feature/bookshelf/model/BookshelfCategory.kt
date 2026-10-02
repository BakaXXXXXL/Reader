package com.reader.feature.bookshelf.model

/**
 * 书架分类枚举。
 * 用于书架顶部 Tab 栏快速筛选不同阅读状态的书籍。
 */
enum class BookshelfCategory(val title: String) {
    /** 全部书籍 */
    ALL("全部"),

    /** 正在阅读中的书籍 (进度 > 0% 且未读完) */
    READING("在读"),

    /** 已读完的书籍 (进度 >= 99% 或已标记完成) */
    COMPLETED("已读"),

    /** 用户收藏星标书籍 */
    FAVORITE("收藏");

    companion object {
        val default = ALL
    }
}
