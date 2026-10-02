package com.reader.engine.typography.model

/**
 * 页面可视矩形区域与内边距尺寸。
 *
 * @property viewWidth 视口/屏幕总宽度 (px)
 * @property viewHeight 视口/屏幕总高度 (px)
 * @property paddingLeft 左边距 (px)
 * @property paddingTop 上边距 (px)
 * @property paddingRight 右边距 (px)
 * @property paddingBottom 下边距 (px)
 */
data class PageDimensions(
    val viewWidth: Float,
    val viewHeight: Float,
    val paddingLeft: Float = 0f,
    val paddingTop: Float = 0f,
    val paddingRight: Float = 0f,
    val paddingBottom: Float = 0f
) {
    /** 可用于排版正文文本的净有效宽度 */
    val contentWidth: Float
        get() = (viewWidth - paddingLeft - paddingRight).coerceAtLeast(0f)

    /** 可用于排版正文文本的净有效高度 */
    val contentHeight: Float
        get() = (viewHeight - paddingTop - paddingBottom).coerceAtLeast(0f)

    init {
        require(viewWidth >= 0f) { "viewWidth must be non-negative: $viewWidth" }
        require(viewHeight >= 0f) { "viewHeight must be non-negative: $viewHeight" }
        require(paddingLeft >= 0f) { "paddingLeft must be non-negative: $paddingLeft" }
        require(paddingTop >= 0f) { "paddingTop must be non-negative: $paddingTop" }
        require(paddingRight >= 0f) { "paddingRight must be non-negative: $paddingRight" }
        require(paddingBottom >= 0f) { "paddingBottom must be non-negative: $paddingBottom" }
    }
}
