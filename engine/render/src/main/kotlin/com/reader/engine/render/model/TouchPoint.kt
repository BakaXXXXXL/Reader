package com.reader.engine.render.model

import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * 屏幕二维触控/几何计算点坐标。
 * 纯 Kotlin 建模，不依赖特定 Android 运行时，便于单元测试与算法移植。
 */
data class TouchPoint(
    val x: Float = 0f,
    val y: Float = 0f
) {
    /**
     * 计算当前点与另一个点之间的欧式几何距离。
     */
    fun distanceTo(other: TouchPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * 计算两点间中点。
     */
    fun midpoint(other: TouchPoint): TouchPoint {
        return TouchPoint((x + other.x) / 2f, (y + other.y) / 2f)
    }

    /**
     * 向量平移。
     */
    fun offset(dx: Float, dy: Float): TouchPoint {
        return TouchPoint(x + dx, y + dy)
    }

    /**
     * 校验点坐标是否为有效数字（非 NaN 且非无穷大）。
     */
    val isValid: Boolean
        get() = !x.isNaN() && !y.isNaN() && !x.isInfinite() && !y.isInfinite()

    companion object {
        val ZERO = TouchPoint(0f, 0f)

        fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            return hypot(x1 - x2, y1 - y2)
        }
    }
}
