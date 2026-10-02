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
 * 水平左右平移滑动翻页模式 (Slide)。
 *
 * 视觉与交互机制：
 * - 两个页面并排呈水平胶片式连续平移：当前页与目标页完全同步位移；
 * - 翻向下一页 (NEXT)：当前页向左滑出 [0 -> -width]，下一页自右侧滑入 [width -> 0]；
 * - 翻向上一页 (PREVIOUS)：当前页向右滑出 [0 -> width]，上一页自左侧滑入 [-width -> 0]；
 * - 页面拼缝处渲染微弱纵向书脊缝隙暗影，赋予立体交界感。
 */
class SlidePageAnimation : PageAnimation() {

    private var startX: Float = 0f
    var displacementX: Float = 0f
        private set

    private var animStartDisplacement: Float = 0f
    private var animTargetDisplacement: Float = 0f
    private var animDurationMs: Long = 260L
    private var animElapsedMs: Long = 0L

    var isCommitTurn: Boolean = false
        private set

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val srcRect = Rect()
    private val dstRect = Rect()

    private val seamShadowWidth = 16f

    init {
        turnDirection = TurnDirection.NONE
        turnState = TurnState.IDLE
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

        if (turnDirection == TurnDirection.NONE && abs(dx) > 8f) {
            turnDirection = if (dx < 0) TurnDirection.NEXT else TurnDirection.PREVIOUS
        }

        val safeW = max(1, width).toFloat()
        when (turnDirection) {
            TurnDirection.NEXT -> {
                displacementX = displacementX.coerceIn(-safeW, 0f)
                progress = (abs(displacementX) / safeW).coerceIn(0f, 1f)
            }
            TurnDirection.PREVIOUS -> {
                displacementX = displacementX.coerceIn(0f, safeW)
                progress = (displacementX / safeW).coerceIn(0f, 1f)
            }
            TurnDirection.NONE -> {
                progress = 0f
            }
        }
        return true
    }

    override fun onTouchUp(velocityX: Float): Boolean {
        if (turnState != TurnState.DRAGGING) return false

        val safeW = max(1, width).toFloat()
        val thresholdDistance = safeW * 0.22f
        val velocityThreshold = 800f

        when (turnDirection) {
            TurnDirection.NEXT -> {
                isCommitTurn = abs(displacementX) > thresholdDistance || velocityX < -velocityThreshold
                animStartDisplacement = displacementX
                animTargetDisplacement = if (isCommitTurn) -safeW else 0f
            }
            TurnDirection.PREVIOUS -> {
                isCommitTurn = displacementX > thresholdDistance || velocityX > velocityThreshold
                animStartDisplacement = displacementX
                animTargetDisplacement = if (isCommitTurn) safeW else 0f
            }
            TurnDirection.NONE -> {
                reset()
                return false
            }
        }

        turnState = TurnState.ANIMATING
        animDurationMs = 240L
        animElapsedMs = 0L
        return true
    }

    override fun startAutoTurn(direction: TurnDirection, durationMs: Long) {
        if (turnState == TurnState.ANIMATING) {
            abortAnimation()
        }
        val safeW = max(1, width).toFloat()
        turnDirection = direction
        turnState = TurnState.ANIMATING
        isCommitTurn = true
        animDurationMs = durationMs
        animElapsedMs = 0L

        when (direction) {
            TurnDirection.NEXT -> {
                animStartDisplacement = 0f
                animTargetDisplacement = -safeW
                displacementX = 0f
            }
            TurnDirection.PREVIOUS -> {
                animStartDisplacement = 0f
                animTargetDisplacement = safeW
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
        val easedFraction = 1f - (1f - rawFraction) * (1f - rawFraction) * (1f - rawFraction)

        displacementX = animStartDisplacement + (animTargetDisplacement - animStartDisplacement) * easedFraction
        val safeW = max(1, width).toFloat()
        progress = (abs(displacementX) / safeW).coerceIn(0f, 1f)

        if (rawFraction >= 1f) {
            displacementX = animTargetDisplacement
            turnState = TurnState.IDLE
            return false
        }
        return true
    }

    override fun draw(canvas: Canvas, renderState: PageRenderState) {
        if (width <= 0 || height <= 0) return

        when (turnDirection) {
            TurnDirection.NEXT -> {
                val curLeft = displacementX.toInt()
                val nextLeft = curLeft + width

                // 1. 绘制当前页
                renderState.currentBitmap?.let { curBmp ->
                    if (!curBmp.isRecycled) {
                        srcRect.set(0, 0, curBmp.width, curBmp.height)
                        dstRect.set(curLeft, 0, curLeft + width, height)
                        canvas.drawBitmap(curBmp, srcRect, dstRect, bitmapPaint)
                    }
                }

                // 2. 绘制下一页
                renderState.nextBitmap?.let { nextBmp ->
                    if (!nextBmp.isRecycled) {
                        srcRect.set(0, 0, nextBmp.width, nextBmp.height)
                        dstRect.set(nextLeft, 0, nextLeft + width, height)
                        canvas.drawBitmap(nextBmp, srcRect, dstRect, bitmapPaint)
                    }
                }

                // 3. 绘制两页交界缝隙暗影
                drawSeamShadow(canvas, nextLeft.toFloat())
            }

            TurnDirection.PREVIOUS -> {
                val curLeft = displacementX.toInt()
                val prevLeft = curLeft - width

                // 1. 绘制上一页
                renderState.prevBitmap?.let { prevBmp ->
                    if (!prevBmp.isRecycled) {
                        srcRect.set(0, 0, prevBmp.width, prevBmp.height)
                        dstRect.set(prevLeft, 0, prevLeft + width, height)
                        canvas.drawBitmap(prevBmp, srcRect, dstRect, bitmapPaint)
                    }
                }

                // 2. 绘制当前页
                renderState.currentBitmap?.let { curBmp ->
                    if (!curBmp.isRecycled) {
                        srcRect.set(0, 0, curBmp.width, curBmp.height)
                        dstRect.set(curLeft, 0, curLeft + width, height)
                        canvas.drawBitmap(curBmp, srcRect, dstRect, bitmapPaint)
                    }
                }

                // 3. 绘制两页交界缝隙暗影
                drawSeamShadow(canvas, curLeft.toFloat())
            }

            TurnDirection.NONE -> {
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

    private fun drawSeamShadow(canvas: Canvas, seamX: Float) {
        val halfW = seamShadowWidth / 2f
        seamPaint.shader = LinearGradient(
            seamX - halfW, 0f,
            seamX + halfW, 0f,
            intArrayOf(
                Color.argb(0, 0, 0, 0),
                Color.argb(80, 0, 0, 0),
                Color.argb(0, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(
            seamX - halfW, 0f,
            seamX + halfW, height.toFloat(),
            seamPaint
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
