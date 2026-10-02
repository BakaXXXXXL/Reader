package com.reader.feature.bookshelf.model

/**
 * 书架书籍排序维度。
 */
enum class BookshelfSortOrder(val title: String) {
    /** 按最近一次阅读时间倒序 (默认推荐) */
    RECENT_READ("最近阅读"),

    /** 按书名首字母/拼音 A-Z 顺序 */
    TITLE("书名 (A-Z)"),

    /** 按加入书架时间倒序 (最新添加置顶) */
    ADD_TIME("最新添加"),

    /** 按阅读完成进度倒序 */
    PROGRESS("阅读进度"),

    /** 用户拖拽自定义排序 */
    MANUAL("自定义排序");

    companion object {
        val default = RECENT_READ
    }
}
