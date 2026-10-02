package com.reader.engine.typography.model

/**
 * 字体度量参数。
 * 记录字体的基线、上边界 (ascent)、下边界 (descent) 等关键度量。
 *
 * @property ascent 基线以上的推荐距离 (为负值)
 * @property descent 基线以下的推荐距离 (为正值)
 * @property leading 行间距额外预留 (可为 0)
 * @property top 最大上边界 (负值)
 * @property bottom 最大下边界 (正值)
 */
data class FontMetricsInfo(
    val ascent: Float,
    val descent: Float,
    val leading: Float = 0f,
    val top: Float = ascent,
    val bottom: Float = descent
) {
    /** 字符净高度 (descent - ascent) */
    val textHeight: Float
        get() = descent - ascent

    /** 包含 leading 的完整单行物理高度 */
    val fullLineHeight: Float
        get() = (descent - ascent + leading).coerceAtLeast(0f)
}
