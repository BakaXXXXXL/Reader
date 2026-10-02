package com.reader.engine.render.animation

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.reader.engine.render.model.PageRenderState
import com.reader.engine.render.model.TurnDirection
import com.reader.engine.render.model.TurnState
import kotlin.math.abs
import kotlin.math.max

/**
 * 瞬间无动画翻页模式 (None)。
 * 适用于墨水屏 (E-Ink) 设备或追求零延迟翻页的读者。手势释放后瞬间切换页面，不产生中间过渡帧。
 */
class NonePageAnimation : PageAnimation() {

    private var startX: Float = 0f
    private var totalDeltaX: Float = 0f

    var isCommitTurn: Boolean = false
        private set

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val srcRect = Rect()
    private val dstRect = Rect()

    init {
        turnDirection = TurnDirection.NONE
        turnState = TurnState.IDLE
    }

    override fun onTouchDown(x: Float, y: Float) {
        startX = x
        totalDeltaX = 0f
        progress = 0f
        turnDirection = TurnDirection.NONE
        turnState = TurnState.DRAGGING
        isCommitTurn = false
    }

    override fun onTouchMove(x: Float, y: Float): Boolean {
        if (turnState != TurnState.DRAGGING) return false
        val dx = x - startX
        totalDeltaX = dx

        if (turnDirection == TurnDirection.NONE && abs(dx) > 10f) {
            turnDirection = if (dx < 0) TurnDirection.NEXT else TurnDirection.PREVIOUS
        }

        val safeW = max(1, width).toFloat()
        progress = (abs(totalDeltaX) / safeW).coerceIn(0f, 1f)
        return false // 无动画模式移动中不主动请求逐帧重绘
    }

    override fun onTouchUp(velocityX: Float): Boolean {
        if (turnState != TurnState.DRAGGING) return false

        val safeW = max(1, width).toFloat()
        val thresholdDistance = safeW * 0.15f

        when (turnDirection) {
            TurnDirection.NEXT -> {
                isCommitTurn = abs(totalDeltaX) > thresholdDistance || velocityX < -600f
            }
            TurnDirection.PREVIOUS -> {
                isCommitTurn = totalDeltaX > thresholdDistance || velocityX > 600f
            }
            TurnDirection.NONE -> {
                isCommitTurn = false
                reset()
                return false
            }
        }

        // 瞬间完成，直接进入 IDLE 并指示已决断
        turnState = TurnState.IDLE
        progress = if (isCommitTurn) 1f else 0f
        return isCommitTurn
    }

    override fun startAutoTurn(direction: TurnDirection, durationMs: Long) {
        turnDirection = direction
        isCommitTurn = true
        progress = 1f
        turnState = TurnState.IDLE
    }

    override fun stepAnimation(deltaMs: Long): Boolean {
        // 无动画模式瞬间完成
        turnState = TurnState.IDLE
        return false
    }

    override fun draw(canvas: Canvas, renderState: PageRenderState) {
        if (width <= 0 || height <= 0) return

        // 无论何种情况均直接全屏绘制当前位图
        renderState.currentBitmap?.let { bmp ->
            if (!bmp.isRecycled) {
                srcRect.set(0, 0, bmp.width, bmp.height)
                dstRect.set(0, 0, width, height)
                canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
            }
        }
    }

    override fun abortAnimation() {
        turnState = TurnState.IDLE
    }

    override fun reset() {
        super.reset()
        startX = 0f
        totalDeltaX = 0f
        isCommitTurn = false
    }
}
