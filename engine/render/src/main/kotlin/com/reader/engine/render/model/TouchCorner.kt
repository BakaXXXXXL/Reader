package com.reader.engine.render.model

/**
 * 触控翻页触发的屏幕对角/边角锚点。
 * 用于 3D 拟真仿真翻页时确定卷曲原点 F 及垂直/水平对边。
 */
enum class TouchCorner {
    /** 右上角 (x = width, y = 0) */
    TOP_RIGHT,

    /** 右下角 (x = width, y = height) */
    BOTTOM_RIGHT,

    /** 右侧边缘中部 (x = width, y = height / 2) */
    RIGHT,

    /** 左上角 (x = 0, y = 0) */
    TOP_LEFT,

    /** 左下角 (x = 0, y = height) */
    BOTTOM_LEFT,

    /** 左侧边缘中部 (x = 0, y = height / 2) */
    LEFT,

    /** 无特定角点 */
    NONE;

    /**
     * 判断是否为右侧角点（通常用于翻向下一页）。
     */
    val isRightSide: Boolean
        get() = this == TOP_RIGHT || this == BOTTOM_RIGHT || this == RIGHT

    /**
     * 判断是否为左侧角点（通常用于翻向上一页）。
     */
    val isLeftSide: Boolean
        get() = this == TOP_LEFT || this == BOTTOM_LEFT || this == LEFT

    /**
     * 判断是否为上半部角点。
     */
    val isTopSide: Boolean
        get() = this == TOP_RIGHT || this == TOP_LEFT
}
