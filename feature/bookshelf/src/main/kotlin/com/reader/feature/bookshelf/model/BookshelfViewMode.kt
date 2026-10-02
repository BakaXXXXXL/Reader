package com.reader.feature.bookshelf.model

/**
 * 书架视图展示模式。
 */
enum class BookshelfViewMode(val title: String) {
    /** 经典九宫格卡片网格模式 */
    GRID("网格视图"),

    /** 纵向详细信息列表模式 */
    LIST("列表视图");

    companion object {
        val default = GRID
    }
}
