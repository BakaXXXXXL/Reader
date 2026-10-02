package com.reader.engine.render.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import com.reader.core.model.PageTurnAnimation
import com.reader.engine.render.buffer.DoubleBufferManager
import com.reader.engine.render.controller.OnPageTurnListener
import com.reader.engine.render.controller.PageTurnController
import com.reader.engine.render.headerfooter.HeaderFooterRenderer
import com.reader.engine.render.model.PageRenderState
import com.reader.engine.render.model.TurnDirection

/**
 * 原生双缓冲 Canvas 阅读渲染与多模式翻页主视图。
 *
 * 核心架构特性：
 * 1. 硬件加速 + 离屏双缓冲：避免重复测量排版造成丢帧；
 * 2. 多模式翻页支持：默认平滑覆盖 (Cover)、拟真 3D 仿真 (Simulation)、平移 (Slide)、垂直连续滚动 (Scroll) 与无动画 (None)；
 * 3. 动态页眉页脚实时绘制：电量、时间、章节名、页码进度；
 * 4. 触控热区智能分发与惯性物理速度追踪 (VelocityTracker)。
 */
class ReaderCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), OnPageTurnListener {

    /** 双缓冲离屏管理器 */
    val bufferManager = DoubleBufferManager()

    /** 翻页动效控制器 */
    val turnController = PageTurnController(listener = this)

    /** 页眉页脚实时绘制器 */
    val headerFooterRenderer = HeaderFooterRenderer()

    /** 当前阅读渲染状态 */
    var renderState: PageRenderState = PageRenderState.EMPTY
        private set

    /** 外部业务层翻页事件监听器 */
    var pageTurnListener: OnPageTurnListener? = null

    /** 速度追踪器 */
    private var velocityTracker: VelocityTracker? = null

    /** 上一帧渲染时间戳 (毫秒) */
    private var lastFrameTimeMs: Long = 0L

    init {
        // 获取当前屏幕像素密度
        val metrics = resources.displayMetrics
        headerFooterRenderer.updateDensity(metrics.density, metrics.scaledDensity)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        bufferManager.setup(w, h)
        turnController.setup(w, h, renderState.totalPagesInChapter)

        renderState = renderState.copy(
            viewportWidth = w,
            viewportHeight = h,
            currentBitmap = bufferManager.currentBitmap,
            nextBitmap = bufferManager.nextBitmap,
            prevBitmap = bufferManager.prevBitmap
        )
    }

    /**
     * 绑定最新的阅读渲染状态并重绘。
     */
    fun updateRenderState(state: PageRenderState) {
        this.renderState = state.copy(
            viewportWidth = width,
            viewportHeight = height,
            currentBitmap = state.currentBitmap ?: bufferManager.currentBitmap,
            nextBitmap = state.nextBitmap ?: bufferManager.nextBitmap,
            prevBitmap = state.prevBitmap ?: bufferManager.prevBitmap
        )
        invalidate()
    }

    /**
     * 动态设置翻页动效模式。
     */
    fun setPageTurnAnimation(mode: PageTurnAnimation) {
        turnController.setAnimationMode(mode)
    }

    /**
     * 主动触发下一页。
     */
    fun turnNext() {
        turnController.turnNext()
    }

    /**
     * 主动触发上一页。
     */
    fun turnPrevious() {
        turnController.turnPrevious()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastFrameTimeMs = System.currentTimeMillis()
                turnController.onTouchDown(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                turnController.onTouchMove(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_UP -> {
                velocityTracker?.computeCurrentVelocity(1000)
                val vx = velocityTracker?.xVelocity ?: 0f
                val vy = velocityTracker?.yVelocity ?: 0f

                turnController.onTouchUp(vx, vy)

                velocityTracker?.recycle()
                velocityTracker = null
                lastFrameTimeMs = System.currentTimeMillis()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                turnController.onTouchCancel()
                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        // 1. 绘制背景纯色
        val bgColor = (renderState.themePreset.backgroundColor and 0xFFFFFFFFL).toInt()
        canvas.drawColor(bgColor)

        // 2. 调度动效控制器绘制翻页正文图层
        turnController.draw(canvas, renderState)

        // 3. 在最顶层边距内实时绘制页眉与页脚
        headerFooterRenderer.draw(canvas, renderState)

        // 4. 驱动逐帧动画
        val now = System.currentTimeMillis()
        val deltaMs = if (lastFrameTimeMs > 0L) (now - lastFrameTimeMs).coerceIn(1L, 50L) else 16L
        lastFrameTimeMs = now

        val isStillRunning = turnController.stepAnimation(deltaMs)
        if (isStillRunning) {
            postInvalidateOnAnimation()
        }
    }

    // ==================== OnPageTurnListener 事件转发 ====================

    override fun onPageTurnStarted(direction: TurnDirection) {
        pageTurnListener?.onPageTurnStarted(direction)
    }

    override fun onPageTurnProgress(direction: TurnDirection, progress: Float) {
        pageTurnListener?.onPageTurnProgress(direction, progress)
    }

    override fun onPageTurnCompleted(direction: TurnDirection) {
        // 翻页成功后交换底层位图缓冲指针
        bufferManager.swapForDirection(direction)
        renderState = renderState.copy(
            currentBitmap = bufferManager.currentBitmap,
            nextBitmap = bufferManager.nextBitmap,
            prevBitmap = bufferManager.prevBitmap
        )
        pageTurnListener?.onPageTurnCompleted(direction)
    }

    override fun onPageTurnCanceled() {
        pageTurnListener?.onPageTurnCanceled()
    }

    override fun onCenterClicked() {
        pageTurnListener?.onCenterClicked()
    }

    override fun onInvalidateRequest() {
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bufferManager.recycle()
        velocityTracker?.recycle()
        velocityTracker = null
    }
}
