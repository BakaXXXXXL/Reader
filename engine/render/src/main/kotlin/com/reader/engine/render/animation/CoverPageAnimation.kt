package com.reader.engine.render.animation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import com.reader.engine.render.model.PageRenderState
import com.reader.engine.render.model.TurnDirection
import com.reader.engine.render.model.TurnState
import kotlin.math.abs
import kotlin.math.max

/**
 * 平滑横向覆盖翻页模式【出厂默认】。
 *
 * 交互与视觉规范：
 * - 翻向下一页 (NEXT)：底层静止展示下一页，顶层当前页随手势向左平移滑出，当前页右边缘投射高拟真深度阴影；
 * - 翻向上一页 (PREVIOUS)：底层静止展示当前页，顶层上一页自左侧 (-width) 向右平移滑入覆盖，上一页右侧边缘带平滑边缘投影；
 * - 支持惯性初速度判定、拖拽距离阈值 (25%) 以及三次缓动 (Cubic Ease-Out) 平滑弹性复位动画。
 */
class CoverPageAnimation : PageAnimation() {

    /** 手指落下初始 X 坐标 */
    private var startX: Float = 0f

    /** 手指当前水平位移 (相对于 startX) */
    var displacementX: Float = 0f
        private set

    /** 动画起始位移量 */
    private var animStartDisplacement: Float = 0f

    /** 动画目标位移量 */
    private var animTargetDisplacement: Float = 0f

    /** 动画总时长 (毫秒) */
    private var animDurationMs: Long = 280L

    /** 动画已流逝时间 (毫秒) */
    private var animElapsedMs: Long = 0L

    /** 是否决定完成本次翻页（若 false 则为取消回弹） */
    var isCommitTurn: Boolean = false
        private set

    /** 边缘阴影绘制画笔 */
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 默认位图绘制画笔 */
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    /** 边缘投影像素宽度 */
    var shadowWidth: Float = 36f

    /** 位图绘制矩形复用 */
    private val srcRect = Rect()
    private val dstRect = Rect()

    init {
        turnDirection = TurnDirection.NONE
        turnState = TurnState.IDLE
    }

    override fun setup(viewportWidth: Int, viewportHeight: Int) {
        super.setup(viewportWidth, viewportHeight)
        // 根据屏幕宽度动态适配阴影宽度 (约占屏幕宽度的 5%，最小 24px，最大 56px)
        if (viewportWidth > 0) {
            shadowWidth = (viewportWidth * 0.05f).coerceIn(24f, 56f)
        }
    }

    override fun onTouchDown(x: Float, y: Float) {
        if (turnState == TurnState.ANIMATING) {
            abortAnimation()
        }
        startX = x
        displacementX = 0f
        progress = 0f
        turnDirection = TurnDirection.NONE
        turnState = TurnState.DRAGGING
        isCommitTurn = false
    }

    override fun onTouchMove(x: Float, y: Float): Boolean {
        if (turnState != TurnState.DRAGGING) return false

        val dx = x - startX
        displacementX = dx

        // 判定翻页方向
        if (turnDirection == TurnDirection.NONE && abs(dx) > 8f) {
            turnDirection = if (dx < 0) TurnDirection.NEXT else TurnDirection.PREVIOUS
        }

        // 约束拖拽位移范围
        val safeWidth = max(1, width).toFloat()
        when (turnDirection) {
            TurnDirection.NEXT -> {
                // 翻下一页：位移应为负数，限制在 [-width, 0]
                displacementX = displacementX.coerceIn(-safeWidth, 0f)
                progress = (abs(displacementX) / safeWidth).coerceIn(0f, 1f)
            }
            TurnDirection.PREVIOUS -> {
                // 翻上一页：位移应为正数，限制在 [0, width]
                displacementX = displacementX.coerceIn(0f, safeWidth)
                progress = (displacementX / safeWidth).coerceIn(0f, 1f)
            }
            TurnDirection.NONE -> {
                progress = 0f
            }
        }

        return true
    }

    override fun onTouchUp(velocityX: Float): Boolean {
        if (turnState != TurnState.DRAGGING) return false

        val safeWidth = max(1, width).toFloat()
        val thresholdDistance = safeWidth * 0.22f
        val velocityThreshold = 800f

        when (turnDirection) {
            TurnDirection.NEXT -> {
                // 向左滑动位移超过阈值，或向左快速甩动
                isCommitTurn = abs(displacementX) > thresholdDistance || velocityX < -velocityThreshold
                animStartDisplacement = displacementX
                animTargetDisplacement = if (isCommitTurn) -safeWidth else 0f
            }
            TurnDirection.PREVIOUS -> {
                // 向右滑动位移超过阈值，或向右快速甩动
                isCommitTurn = displacementX > thresholdDistance || velocityX > velocityThreshold
                animStartDisplacement = displacementX
                animTargetDisplacement = if (isCommitTurn) safeWidth else 0f
            }
            TurnDirection.NONE -> {
                isCommitTurn = false
                reset()
                return false
            }
        }

        turnState = TurnState.ANIMATING
        animDurationMs = 260L
        animElapsedMs = 0L
        return true
    }

    override fun startAutoTurn(direction: TurnDirection, durationMs: Long) {
        if (turnState == TurnState.ANIMATING) {
            abortAnimation()
        }
        val safeWidth = max(1, width).toFloat()
        turnDirection = direction
        turnState = TurnState.ANIMATING
        isCommitTurn = true
        animDurationMs = durationMs
        animElapsedMs = 0L

        when (direction) {
            TurnDirection.NEXT -> {
                animStartDisplacement = 0f
                animTargetDisplacement = -safeWidth
                displacementX = 0f
            }
            TurnDirection.PREVIOUS -> {
                animStartDisplacement = 0f
                animTargetDisplacement = safeWidth
                displacementX = 0f
            }
            TurnDirection.NONE -> {
                reset()
            }
        }
    }

    override fun stepAnimation(deltaMs: Long): Boolean {
        if (turnState != TurnState.ANIMATING) return false

        animElapsedMs += deltaMs
        val rawFraction = (animElapsedMs.toFloat() / animDurationMs.toFloat()).coerceIn(0f, 1f)

        // 三次减速缓动: f(t) = 1 - (1 - t)^3
        val easedFraction = 1f - (1f - rawFraction) * (1f - rawFraction) * (1f - rawFraction)
        displacementX = animStartDisplacement + (animTargetDisplacement - animStartDisplacement) * easedFraction

        val safeWidth = max(1, width).toFloat()
        progress = (abs(displacementX) / safeWidth).coerceIn(0f, 1f)

        if (rawFraction >= 1f) {
            displacementX = animTargetDisplacement
            turnState = TurnState.IDLE
            return false // 动画结束
        }

        return true // 动画仍在进行中
    }

    override fun draw(canvas: Canvas, renderState: PageRenderState) {
        if (width <= 0 || height <= 0) return

        when (turnDirection) {
            TurnDirection.NEXT -> {
                // 1. 底层：绘制下一页（静止在 (0, 0)）
                renderState.nextBitmap?.let { nextBmp ->
                    if (!nextBmp.isRecycled) {
                        srcRect.set(0, 0, nextBmp.width, nextBmp.height)
                        dstRect.set(0, 0, width, height)
                        canvas.drawBitmap(nextBmp, srcRect, dstRect, bitmapPaint)
                    }
                }

                // 2. 顶层：绘制当前页（向左平移 displacementX）
                renderState.currentBitmap?.let { curBmp ->
                    if (!curBmp.isRecycled) {
                        val currentLeft = displacementX.toInt()
                        srcRect.set(0, 0, curBmp.width, curBmp.height)
                        dstRect.set(currentLeft, 0, currentLeft + width, height)
                        canvas.drawBitmap(curBmp, srcRect, dstRect, bitmapPaint)

                        // 3. 绘制当前页右边缘的深度投影
                        drawEdgeShadow(
                            canvas = canvas,
                            shadowLeft = (currentLeft + width).toFloat(),
                            shadowRight = (currentLeft + width + shadowWidth),
                            isLeftToRight = true
                        )
                    }
                }
            }

            TurnDirection.PREVIOUS -> {
                // 1. 底层：绘制当前页（静止在 (0, 0)）
                renderState.currentBitmap?.let { curBmp ->
                    if (!curBmp.isRecycled) {
                        srcRect.set(0, 0, curBmp.width, curBmp.height)
                        dstRect.set(0, 0, width, height)
                        canvas.drawBitmap(curBmp, srcRect, dstRect, bitmapPaint)
                    }
                }

                // 2. 顶层：绘制上一页（从 -width + displacementX 滑入）
                renderState.prevBitmap?.let { prevBmp ->
                    if (!prevBmp.isRecycled) {
                        val prevLeft = (-width + displacementX).toInt()
                        srcRect.set(0, 0, prevBmp.width, prevBmp.height)
                        dstRect.set(prevLeft, 0, prevLeft + width, height)
                        canvas.drawBitmap(prevBmp, srcRect, dstRect, bitmapPaint)

                        // 3. 绘制上一页右侧边缘投影
                        drawEdgeShadow(
                            canvas = canvas,
                            shadowLeft = (prevLeft + width).toFloat(),
                            shadowRight = (prevLeft + width + shadowWidth),
                            isLeftToRight = true
                        )
                    }
                }
            }

            TurnDirection.NONE -> {
                // 静止无翻页状态：仅绘制当前页
                renderState.currentBitmap?.let { curBmp ->
                    if (!curBmp.isRecycled) {
                        srcRect.set(0, 0, curBmp.width, curBmp.height)
                        dstRect.set(0, 0, width, height)
                        canvas.drawBitmap(curBmp, srcRect, dstRect, bitmapPaint)
                    }
                }
            }
        }
    }

    /**
     * 绘制纵向线性渐变投影。
     */
    private fun drawEdgeShadow(
        canvas: Canvas,
        shadowLeft: Float,
        shadowRight: Float,
        isLeftToRight: Boolean
    ) {
        val startColor = Color.argb(120, 0, 0, 0)
        val endColor = Color.argb(0, 0, 0, 0)

        shadowPaint.shader = LinearGradient(
            shadowLeft, 0f,
            shadowRight, 0f,
            if (isLeftToRight) startColor else endColor,
            if (isLeftToRight) endColor else startColor,
            Shader.TileMode.CLAMP
        )

        canvas.drawRect(
            shadowLeft, 0f,
            shadowRight, height.toFloat(),
            shadowPaint
        )
    }

    override fun abortAnimation() {
        if (turnState == TurnState.ANIMATING) {
            displacementX = animTargetDisplacement
            turnState = TurnState.IDLE
        }
    }

    override fun reset() {
        super.reset()
        startX = 0f
        displacementX = 0f
        animStartDisplacement = 0f
        animTargetDisplacement = 0f
        animElapsedMs = 0L
        isCommitTurn = false
    }
}
