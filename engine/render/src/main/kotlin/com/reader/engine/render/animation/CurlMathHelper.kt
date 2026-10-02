package com.reader.engine.render.animation

import com.reader.engine.render.model.CurlPoints
import com.reader.engine.render.model.TouchCorner
import com.reader.engine.render.model.TouchPoint
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 拟真 3D 仿真翻页几何算法助手。
 * 负责手势触控点约束计算、屏幕角点判定、中垂线交点求解以及二次贝塞尔曲线控制点建模。
 * 纯 Kotlin 实现，杜绝平台绑定，支持极速单元测试。
 */
object CurlMathHelper {

    /** 触控点与边界的最小容差，防止除以零 */
    private const val EPSILON = 0.001f

    /**
     * 根据初始触控点坐标和视口尺寸判定翻页锚点角落。
     *
     * @param downX 触控落下 X 坐标
     * @param downY 触控落下 Y 坐标
     * @param width 视口宽度
     * @param height 视口高度
     * @return 识别出的屏幕角点
     */
    fun determineCorner(downX: Float, downY: Float, width: Float, height: Float): TouchCorner {
        if (width <= 0f || height <= 0f) return TouchCorner.BOTTOM_RIGHT

        val isRight = downX >= width / 2f
        val isTop = downY < height / 2f

        return when {
            isRight && isTop -> TouchCorner.TOP_RIGHT
            isRight && !isTop -> TouchCorner.BOTTOM_RIGHT
            !isRight && isTop -> TouchCorner.TOP_LEFT
            else -> TouchCorner.BOTTOM_LEFT
        }
    }

    /**
     * 获取角点在屏幕坐标系中的固定锚点物理坐标 F。
     *
     * @param corner 屏幕角点
     * @param width 视口宽度
     * @param height 视口高度
     */
    fun getCornerPoint(corner: TouchCorner, width: Float, height: Float): TouchPoint {
        return when (corner) {
            TouchCorner.TOP_RIGHT -> TouchPoint(width, 0f)
            TouchCorner.BOTTOM_RIGHT -> TouchPoint(width, height)
            TouchCorner.RIGHT -> TouchPoint(width, height)
            TouchCorner.TOP_LEFT -> TouchPoint(0f, 0f)
            TouchCorner.BOTTOM_LEFT -> TouchPoint(0f, height)
            TouchCorner.LEFT -> TouchPoint(0f, height)
            TouchCorner.NONE -> TouchPoint(width, height)
        }
    }

    /**
     * 约束触控点 A 的位置，防止超出屏幕边界、折角向内翻卷过深或导致中垂线与边界无交点。
     *
     * 几何约束原理：
     * 1. 触控点 A 到角落 F 的距离不能大于屏幕对角线长度；
     * 2. 中垂线与水平边交点 E 必须在 [0, width] 范围内；
     * 3. 避免 Ax 与 Fx、Ay 与 Fy 完全重合导致斜率无穷大或除以零。
     *
     * @param rawA 原始触控点
     * @param f 角点锚点 F
     * @param width 屏幕宽度
     * @param height 屏幕高度
     * @return 约束后的安全触控点 A
     */
    fun constrainTouchPoint(
        rawA: TouchPoint,
        f: TouchPoint,
        width: Float,
        height: Float
    ): TouchPoint {
        var ax = rawA.x
        var ay = rawA.y

        // 1. 防重叠防同轴（微调偏移，保证除法分母不为 0）
        if (kotlin.math.abs(ax - f.x) < EPSILON) {
            ax = if (f.x > 0f) f.x - EPSILON else f.x + EPSILON
        }
        if (kotlin.math.abs(ay - f.y) < EPSILON) {
            ay = if (f.y > 0f) f.y - EPSILON else f.y + EPSILON
        }

        // 2. 避免拖拽穿透反向边界
        if (f.x > 0f) {
            ax = min(ax, f.x - EPSILON)
        } else {
            ax = max(ax, f.x + EPSILON)
        }

        if (f.y > 0f) {
            ay = min(ay, f.y - EPSILON)
        } else {
            ay = max(ay, f.y + EPSILON)
        }

        // 3. 约束 E 点不超出横向边界 (保证 Ex 在 0..width 内)
        // 水平交点公式: Ex = (ax + fx)/2 + (ay - fy)^2 / (2 * (ax - fx))
        // 若 f 为右侧角 (fx = width)，则 Ex 随 ax 减小而减小，若 Ex < 0 则超出左边界
        val dy = ay - f.y
        val w2 = width * width
        val dy2 = dy * dy

        if (f.x > 0f) { // 右侧翻页
            // 当 dy^2 > w^2 时，中垂线在极端角度下可能无法交于底边
            val maxDy = width * 0.99f
            if (kotlin.math.abs(dy) > maxDy) {
                ay = if (f.y > 0f) f.y - maxDy else f.y + maxDy
            }
            val safeDy = ay - f.y
            val minAx = sqrt(max(0.1f, w2 - safeDy * safeDy))
            if (ax < width - minAx) {
                ax = width - minAx
            }
        } else { // 左侧翻页
            val maxDy = width * 0.99f
            if (kotlin.math.abs(dy) > maxDy) {
                ay = if (f.y > 0f) f.y - maxDy else f.y + maxDy
            }
            val safeDy = ay - f.y
            val maxAx = sqrt(max(0.1f, w2 - safeDy * safeDy))
            if (ax > maxAx) {
                ax = maxAx
            }
        }

        // 4. 最大对角线拉伸限制（纸张不可拉长）
        val distToF = TouchPoint(ax, ay).distanceTo(f)
        val maxDiag = hypot(width, height)
        if (distToF > maxDiag) {
            val scale = maxDiag / distToF
            ax = f.x + (ax - f.x) * scale
            ay = f.y + (ay - f.y) * scale
        }

        return TouchPoint(ax, ay)
    }

    /**
     * 根据受控触控点 A 和屏幕锚点 F 计算完整的 3D 仿真贝塞尔曲线控制点集 [CurlPoints]。
     *
     * @param touchX 当前拖拽点 X
     * @param touchY 当前拖拽点 Y
     * @param corner 翻页角点
     * @param width 视口宽度
     * @param height 视口高度
     * @return 完整的贝塞尔控制点集合
     */
    fun calculateCurlPoints(
        touchX: Float,
        touchY: Float,
        corner: TouchCorner,
        width: Float,
        height: Float
    ): CurlPoints {
        val f = getCornerPoint(corner, width, height)
        val a = constrainTouchPoint(TouchPoint(touchX, touchY), f, width, height)

        // 1. 中点 G
        val g = a.midpoint(f)

        // 2. 中垂线与水平边界交点 E
        // 垂直距离与水平距离增量
        val deltaX = a.x - f.x
        val deltaY = a.y - f.y

        val safeDeltaX = if (kotlin.math.abs(deltaX) < EPSILON) {
            if (deltaX < 0) -EPSILON else EPSILON
        } else deltaX

        val safeDeltaY = if (kotlin.math.abs(deltaY) < EPSILON) {
            if (deltaY < 0) -EPSILON else EPSILON
        } else deltaY

        val ex = g.x + (deltaY * deltaY) / (2f * safeDeltaX)
        val ey = f.y
        val e = TouchPoint(ex, ey)

        // 3. 中垂线与垂直边界交点 H
        val hx = f.x
        val hy = g.y + (deltaX * deltaX) / (2f * safeDeltaY)
        val h = TouchPoint(hx, hy)

        // 4. 水平切点 C 与 垂直切点 J
        // Cx = Ex - (Fx - Ex) / 2
        // Jy = Hy - (Fy - Hy) / 2
        val cx = e.x - (f.x - e.x) / 2f
        val cy = f.y
        val c = TouchPoint(cx, cy)

        val jx = f.x
        val jy = h.y - (f.y - h.y) / 2f
        val j = TouchPoint(jx, jy)

        // 5. 贝塞尔折线控制顶点 B 和 K
        // B 为 C 到 A 折线的平滑过渡锚点 (以 E, A 为导向)
        val bx = (2f * e.x + a.x) / 3f
        val by = (2f * e.y + a.y) / 3f
        val b = TouchPoint(bx, by)

        // K 为 J 到 A 折线的平滑过渡锚点 (以 H, A 为导向)
        val kx = (2f * h.x + a.x) / 3f
        val ky = (2f * h.y + a.y) / 3f
        val k = TouchPoint(kx, ky)

        // 6. 背面折角过渡点 D 和 I
        val dx = (c.x + b.x) / 2f
        val dy = (c.y + b.y) / 2f
        val d = TouchPoint(dx, dy)

        val ix = (j.x + k.x) / 2f
        val iy = (j.y + k.y) / 2f
        val i = TouchPoint(ix, iy)

        return CurlPoints(
            a = a,
            f = f,
            g = g,
            e = e,
            h = h,
            c = c,
            j = j,
            b = b,
            k = k,
            d = d,
            i = i,
            corner = corner
        )
    }

    /**
     * 计算翻页完成度进度（0.0f .. 1.0f）。
     *
     * @param touchPoint 当前触控点
     * @param corner 翻页角点
     * @param width 屏幕宽度
     * @return 翻页完成度百分比
     */
    fun calculateTurnProgress(
        touchPoint: TouchPoint,
        corner: TouchCorner,
        width: Float
    ): Float {
        if (width <= 0f) return 0f
        return when (corner) {
            TouchCorner.TOP_RIGHT, TouchCorner.BOTTOM_RIGHT, TouchCorner.RIGHT -> {
                ((width - touchPoint.x) / width).coerceIn(0f, 1f)
            }
            TouchCorner.TOP_LEFT, TouchCorner.BOTTOM_LEFT, TouchCorner.LEFT -> {
                (touchPoint.x / width).coerceIn(0f, 1f)
            }
            TouchCorner.NONE -> 0f
        }
    }
}
