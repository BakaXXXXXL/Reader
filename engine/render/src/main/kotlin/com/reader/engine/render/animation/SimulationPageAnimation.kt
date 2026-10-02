package com.reader.engine.render.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import com.reader.engine.render.model.CurlPoints
import com.reader.engine.render.model.PageRenderState
import com.reader.engine.render.model.TouchCorner
import com.reader.engine.render.model.TouchPoint
import com.reader.engine.render.model.TurnDirection
import com.reader.engine.render.model.TurnState
import kotlin.math.abs
import kotlin.math.max

/**
 * 拟真 3D 仿真书页折角翻页动画模式。
 *
 * 核心技术实现：
 * 1. 数学建模：基于触控点 A 与角落 F 构建中垂线，求解交点 E 与 H，通过二次贝塞尔曲线 (Quadratic Bezier) 平滑拟合纸张折痕；
 * 2. 图层三区域分离：
 *    - 区域 A (Path A)：当前页正面保留区域，采用 Path 差集裁剪绘制当前页位图；
 *    - 区域 B (Path B)：翻折显露的下一页底层可视区域，裁剪后绘制目标页位图与底层边缘深邃阴影；
 *    - 区域 C (Path C)：当前页背部翻折区域，绘制微透明反面纸质底色与折线渐变阴影；
 * 3. 稳健容错：遇到极端边界值自动平滑钳位，杜绝 Android Canvas 崩溃与黑边丢帧。
 */
class SimulationPageAnimation : PageAnimation() {

    /** 触控按下时的角点锚点 */
    var touchCorner: TouchCorner = TouchCorner.BOTTOM_RIGHT
        private set

    /** 当前触控点 A 坐标 */
    var touchPointA: TouchPoint = TouchPoint.ZERO
        private set

    /** 初始触控点坐标 */
    private var startPoint: TouchPoint = TouchPoint.ZERO

    /** 动画起始触控点 */
    private var animStartA: TouchPoint = TouchPoint.ZERO

    /** 动画目标触控点 */
    private var animTargetA: TouchPoint = TouchPoint.ZERO

    /** 动画总时长 */
    private var animDurationMs: Long = 320L

    /** 动画已流逝时间 */
    private var animElapsedMs: Long = 0L

    /** 是否决定完成翻页 */
    var isCommitTurn: Boolean = false
        private set

    /** 贝塞尔控制点计算缓存 */
    var currentCurlPoints: CurlPoints = CurlPoints()
        private set

    // ==================== 绘制路径与复用对象 ====================
    private val pathCurrentPage = Path()
    private val pathNextPage = Path()
    private val pathBackPage = Path()

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val backPagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(220, 245, 240, 230) // 仿纸张背部微黄反光质感
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val backShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val srcRect = Rect()
    private val dstRect = Rect()
    private val backMatrix = Matrix()

    init {
        turnDirection = TurnDirection.NONE
        turnState = TurnState.IDLE
    }

    override fun onTouchDown(x: Float, y: Float) {
        if (turnState == TurnState.ANIMATING) {
            abortAnimation()
        }
        val safeW = max(1, width).toFloat()
        val safeH = max(1, height).toFloat()

        startPoint = TouchPoint(x, y)
        touchCorner = CurlMathHelper.determineCorner(x, y, safeW, safeH)
        touchPointA = TouchPoint(x, y)

        progress = 0f
        turnDirection = TurnDirection.NONE
        turnState = TurnState.DRAGGING
        isCommitTurn = false
    }

    override fun onTouchMove(x: Float, y: Float): Boolean {
        if (turnState != TurnState.DRAGGING) return false

        val safeW = max(1, width).toFloat()
        val safeH = max(1, height).toFloat()
        val dx = x - startPoint.x

        if (turnDirection == TurnDirection.NONE && abs(dx) > 6f) {
            turnDirection = if (dx < 0) TurnDirection.NEXT else TurnDirection.PREVIOUS
            // 根据方向校准角点
            if (turnDirection == TurnDirection.NEXT && touchCorner.isLeftSide) {
                touchCorner = if (startPoint.y < safeH / 2f) TouchCorner.TOP_RIGHT else TouchCorner.BOTTOM_RIGHT
            } else if (turnDirection == TurnDirection.PREVIOUS && touchCorner.isRightSide) {
                touchCorner = if (startPoint.y < safeH / 2f) TouchCorner.TOP_LEFT else TouchCorner.BOTTOM_LEFT
            }
        }

        touchPointA = TouchPoint(x, y)
        currentCurlPoints = CurlMathHelper.calculateCurlPoints(
            touchX = touchPointA.x,
            touchY = touchPointA.y,
            corner = touchCorner,
            width = safeW,
            height = safeH
        )

        progress = CurlMathHelper.calculateTurnProgress(currentCurlPoints.a, touchCorner, safeW)
        return true
    }

    override fun onTouchUp(velocityX: Float): Boolean {
        if (turnState != TurnState.DRAGGING) return false

        val safeW = max(1, width).toFloat()
        val safeH = max(1, height).toFloat()
        val cornerPoint = CurlMathHelper.getCornerPoint(touchCorner, safeW, safeH)

        val thresholdDistance = safeW * 0.22f
        val velocityThreshold = 800f

        when (turnDirection) {
            TurnDirection.NEXT -> {
                val dragDistance = safeW - touchPointA.x
                isCommitTurn = dragDistance > thresholdDistance || velocityX < -velocityThreshold
                animStartA = currentCurlPoints.a
                animTargetA = if (isCommitTurn) {
                    // 翻页成功：折角完全翻到对侧屏幕外
                    TouchPoint(-safeW * 0.4f, if (touchCorner.isTopSide) -safeH * 0.1f else safeH * 1.1f)
                } else {
                    // 取消翻页：折角弹性回弹到角点 F
                    cornerPoint
                }
            }
            TurnDirection.PREVIOUS -> {
                val dragDistance = touchPointA.x
                isCommitTurn = dragDistance > thresholdDistance || velocityX > velocityThreshold
                animStartA = currentCurlPoints.a
                animTargetA = if (isCommitTurn) {
                    TouchPoint(safeW * 1.4f, if (touchCorner.isTopSide) -safeH * 0.1f else safeH * 1.1f)
                } else {
                    cornerPoint
                }
            }
            TurnDirection.NONE -> {
                reset()
                return false
            }
        }

        turnState = TurnState.ANIMATING
        animDurationMs = 300L
        animElapsedMs = 0L
        return true
    }

    override fun startAutoTurn(direction: TurnDirection, durationMs: Long) {
        if (turnState == TurnState.ANIMATING) {
            abortAnimation()
        }
        val safeW = max(1, width).toFloat()
        val safeH = max(1, height).toFloat()

        turnDirection = direction
        turnState = TurnState.ANIMATING
        isCommitTurn = true
        animDurationMs = durationMs
        animElapsedMs = 0L

        when (direction) {
            TurnDirection.NEXT -> {
                touchCorner = TouchCorner.BOTTOM_RIGHT
                animStartA = TouchPoint(safeW - 1f, safeH - 1f)
                animTargetA = TouchPoint(-safeW * 0.4f, safeH * 1.1f)
            }
            TurnDirection.PREVIOUS -> {
                touchCorner = TouchCorner.BOTTOM_LEFT
                animStartA = TouchPoint(1f, safeH - 1f)
                animTargetA = TouchPoint(safeW * 1.4f, safeH * 1.1f)
            }
            TurnDirection.NONE -> {
                reset()
                return
            }
        }

        currentCurlPoints = CurlMathHelper.calculateCurlPoints(
            animStartA.x, animStartA.y, touchCorner, safeW, safeH
        )
    }

    override fun stepAnimation(deltaMs: Long): Boolean {
        if (turnState != TurnState.ANIMATING) return false

        animElapsedMs += deltaMs
        val rawFraction = (animElapsedMs.toFloat() / animDurationMs.toFloat()).coerceIn(0f, 1f)

        // 三次平滑减速缓动
        val easedFraction = 1f - (1f - rawFraction) * (1f - rawFraction) * (1f - rawFraction)

        val curX = animStartA.x + (animTargetA.x - animStartA.x) * easedFraction
        val curY = animStartA.y + (animTargetA.y - animStartA.y) * easedFraction
        touchPointA = TouchPoint(curX, curY)

        val safeW = max(1, width).toFloat()
        val safeH = max(1, height).toFloat()

        currentCurlPoints = CurlMathHelper.calculateCurlPoints(
            touchX = curX,
            touchY = curY,
            corner = touchCorner,
            width = safeW,
            height = safeH
        )
        progress = CurlMathHelper.calculateTurnProgress(currentCurlPoints.a, touchCorner, safeW)

        if (rawFraction >= 1f) {
            turnState = TurnState.IDLE
            return false
        }
        return true
    }

    override fun draw(canvas: Canvas, renderState: PageRenderState) {
        if (width <= 0 || height <= 0) return

        // 若处于静止状态且无翻页，直接绘制当前主页
        if (turnDirection == TurnDirection.NONE || !currentCurlPoints.isValid) {
            drawFullBitmap(canvas, renderState.currentBitmap)
            return
        }

        val targetBitmap = when (turnDirection) {
            TurnDirection.NEXT -> renderState.nextBitmap
            TurnDirection.PREVIOUS -> renderState.prevBitmap
            TurnDirection.NONE -> null
        }

        buildPaths(currentCurlPoints)

        // 1. 绘制底层：下一页可见区域 (Path B)
        canvas.save()
        canvas.clipPath(pathNextPage)
        drawFullBitmap(canvas, targetBitmap)
        drawNextPageShadow(canvas, currentCurlPoints)
        canvas.restore()

        // 2. 绘制顶层：当前页正面未翻起区域 (Path A)
        canvas.save()
        canvas.clipPath(pathCurrentPage)
        drawFullBitmap(canvas, renderState.currentBitmap)
        canvas.restore()

        // 3. 绘制折角：当前页背面折起部分 (Path C)
        canvas.save()
        canvas.clipPath(pathBackPage)
        canvas.drawPath(pathBackPage, backPagePaint)
        drawBackPageShadow(canvas, currentCurlPoints)
        canvas.restore()
    }

    /**
     * 构建拟真折页所需的三组贝塞尔裁剪路径。
     */
    private fun buildPaths(p: CurlPoints) {
        pathCurrentPage.reset()
        pathNextPage.reset()
        pathBackPage.reset()

        val safeW = width.toFloat()
        val safeH = height.toFloat()

        // ------------------ 路径 A：当前页正面 ------------------
        // 起始沿屏幕外框，但在折角处沿贝塞尔折痕 (C -> B -> A -> K -> J) 凹陷闭合
        pathCurrentPage.moveTo(0f, 0f)
        if (p.corner == TouchCorner.BOTTOM_RIGHT) {
            pathCurrentPage.lineTo(safeW, 0f)
            pathCurrentPage.lineTo(p.j.x, p.j.y)
            pathCurrentPage.quadTo(p.h.x, p.h.y, p.k.x, p.k.y)
            pathCurrentPage.quadTo(p.a.x, p.a.y, p.b.x, p.b.y)
            pathCurrentPage.quadTo(p.e.x, p.e.y, p.c.x, p.c.y)
            pathCurrentPage.lineTo(0f, safeH)
        } else if (p.corner == TouchCorner.TOP_RIGHT) {
            pathCurrentPage.lineTo(p.c.x, p.c.y)
            pathCurrentPage.quadTo(p.e.x, p.e.y, p.b.x, p.b.y)
            pathCurrentPage.quadTo(p.a.x, p.a.y, p.k.x, p.k.y)
            pathCurrentPage.quadTo(p.h.x, p.h.y, p.j.x, p.j.y)
            pathCurrentPage.lineTo(safeW, safeH)
            pathCurrentPage.lineTo(0f, safeH)
        } else { // 左侧角点
            pathCurrentPage.lineTo(safeW, 0f)
            pathCurrentPage.lineTo(safeW, safeH)
            pathCurrentPage.lineTo(p.c.x, p.c.y)
            pathCurrentPage.quadTo(p.e.x, p.e.y, p.b.x, p.b.y)
            pathCurrentPage.quadTo(p.a.x, p.a.y, p.k.x, p.k.y)
            pathCurrentPage.quadTo(p.h.x, p.h.y, p.j.x, p.j.y)
        }
        pathCurrentPage.close()

        // ------------------ 路径 B：下一页可见底区域 ------------------
        // 由 A -> B -> C -> F -> J -> K -> A 围成的被掀开区域
        pathNextPage.moveTo(p.c.x, p.c.y)
        pathNextPage.quadTo(p.e.x, p.e.y, p.b.x, p.b.y)
        pathNextPage.quadTo(p.a.x, p.a.y, p.k.x, p.k.y)
        pathNextPage.quadTo(p.h.x, p.h.y, p.j.x, p.j.y)
        pathNextPage.lineTo(p.f.x, p.f.y)
        pathNextPage.close()

        // ------------------ 路径 C：当前页折叠背面 ------------------
        // 由 A -> D -> C -> E -> H -> J -> I -> A 构成的翻折多边形区域
        pathBackPage.moveTo(p.d.x, p.d.y)
        pathBackPage.lineTo(p.a.x, p.a.y)
        pathBackPage.lineTo(p.i.x, p.i.y)
        pathBackPage.lineTo(p.j.x, p.j.y)
        pathBackPage.lineTo(p.h.x, p.h.y)
        pathBackPage.lineTo(p.e.x, p.e.y)
        pathBackPage.lineTo(p.c.x, p.c.y)
        pathBackPage.close()
    }

    /**
     * 绘制下一页底层所受的折角投影阴影。
     */
    private fun drawNextPageShadow(canvas: Canvas, p: CurlPoints) {
        val shadowColors = intArrayOf(
            Color.argb(90, 0, 0, 0),
            Color.argb(30, 0, 0, 0),
            Color.argb(0, 0, 0, 0)
        )
        val shadowPositions = floatArrayOf(0f, 0.4f, 1f)

        shadowPaint.shader = LinearGradient(
            p.a.x, p.a.y,
            p.f.x, p.f.y,
            shadowColors,
            shadowPositions,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(pathNextPage, shadowPaint)
    }

    /**
     * 绘制书页翻起背面的折光与边缘微渐变阴影。
     */
    private fun drawBackPageShadow(canvas: Canvas, p: CurlPoints) {
        val backShadowColors = intArrayOf(
            Color.argb(40, 0, 0, 0),
            Color.argb(10, 0, 0, 0),
            Color.argb(0, 0, 0, 0)
        )
        val backPositions = floatArrayOf(0f, 0.6f, 1f)

        backShadowPaint.shader = LinearGradient(
            p.a.x, p.a.y,
            p.g.x, p.g.y,
            backShadowColors,
            backPositions,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(pathBackPage, backShadowPaint)
    }

    private fun drawFullBitmap(canvas: Canvas, bitmap: Bitmap?) {
        bitmap?.let { bmp ->
            if (!bmp.isRecycled) {
                srcRect.set(0, 0, bmp.width, bmp.height)
                dstRect.set(0, 0, width, height)
                canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
            }
        }
    }

    override fun abortAnimation() {
        if (turnState == TurnState.ANIMATING) {
            touchPointA = animTargetA
            turnState = TurnState.IDLE
        }
    }

    override fun reset() {
        super.reset()
        startPoint = TouchPoint.ZERO
        touchPointA = TouchPoint.ZERO
        animStartA = TouchPoint.ZERO
        animTargetA = TouchPoint.ZERO
        animElapsedMs = 0L
        isCommitTurn = false
        currentCurlPoints = CurlPoints()
    }
}
