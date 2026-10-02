package com.reader.engine.render.controller

import android.graphics.Canvas
import com.reader.core.model.PageTurnAnimation
import com.reader.engine.render.animation.CoverPageAnimation
import com.reader.engine.render.animation.DefaultScrollPageAdapter
import com.reader.engine.render.animation.NonePageAnimation
import com.reader.engine.render.animation.PageAnimation
import com.reader.engine.render.animation.ScrollPageAdapter
import com.reader.engine.render.animation.SimulationPageAnimation
import com.reader.engine.render.animation.SlidePageAnimation
import com.reader.engine.render.model.PageRenderState
import com.reader.engine.render.model.TurnDirection
import com.reader.engine.render.model.TurnState
import kotlin.math.abs
import kotlin.math.hypot

/**
 * 翻页动效总控与手势识别状态机。
 *
 * 核心功能：
 * 1. 动效模式无缝热插拔：支持出厂默认平滑覆盖 (Cover)、3D 拟真仿真 (Simulation)、平移 (Slide)、垂直连续滚动 (Scroll) 与无动画 (None)；
 * 2. 交互热区智能识别：
 *    - 屏幕中央区域 (25%..75% 宽, 25%..75% 高)：轻触唤醒/隐藏沉浸阅读菜单；
 *    - 屏幕左侧区域 (< 25% 宽)：轻触翻向上一页；
 *    - 屏幕右侧区域 (> 75% 宽)：轻触翻向下一页；
 * 3. 拖拽与点击精确防抖识别 (TouchSlop 机制)；
 * 4. 弹性复位与逐帧动画驱动。
 */
class PageTurnController(
    var listener: OnPageTurnListener? = null
) {

    /** 视口宽度 */
    var width: Int = 0
        private set

    /** 视口高度 */
    var height: Int = 0
        private set

    /** 当前激活的翻页动效模式枚举 */
    var animationMode: PageTurnAnimation = PageTurnAnimation.COVER
        private set

    /** 当前生效的横向翻页动画实例 */
    var currentAnimation: PageAnimation = CoverPageAnimation()
        private set

    /** 垂直连续滚动模式适配器 */
    val scrollAdapter: ScrollPageAdapter = DefaultScrollPageAdapter()

    // 触控防抖与点击识别参数
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downTime: Long = 0L
    private var hasMovedPastSlop: Boolean = false
    private val touchSlop: Float = 14f

    /**
     * 初始化或变更视口分辨率。
     */
    fun setup(viewportWidth: Int, viewportHeight: Int, pageCount: Int = 1) {
        this.width = viewportWidth
        this.height = viewportHeight
        currentAnimation.setup(viewportWidth, viewportHeight)
        scrollAdapter.setup(viewportWidth, viewportHeight, pageCount)
    }

    /**
     * 动态切换翻页动效模式。
     */
    fun setAnimationMode(mode: PageTurnAnimation) {
        if (this.animationMode == mode) return

        currentAnimation.abortAnimation()
        this.animationMode = mode

        currentAnimation = when (mode) {
            PageTurnAnimation.COVER -> CoverPageAnimation()
            PageTurnAnimation.SIMULATION -> SimulationPageAnimation()
            PageTurnAnimation.SLIDE -> SlidePageAnimation()
            PageTurnAnimation.NONE -> NonePageAnimation()
            PageTurnAnimation.CONTINUOUS_SCROLL -> CoverPageAnimation() // 垂直模式由 scrollAdapter 专职处理
        }

        currentAnimation.setup(width, height)
        listener?.onInvalidateRequest()
    }

    /**
     * 响应手指按下。
     */
    fun onTouchDown(x: Float, y: Float) {
        downX = x
        downY = y
        downTime = System.currentTimeMillis()
        hasMovedPastSlop = false

        if (animationMode == PageTurnAnimation.CONTINUOUS_SCROLL) {
            scrollAdapter.stopScroll()
            listener?.onInvalidateRequest()
            return
        }

        currentAnimation.onTouchDown(x, y)
    }

    /**
     * 响应手指移动。
     */
    fun onTouchMove(x: Float, y: Float): Boolean {
        val dist = hypot(x - downX, y - downY)
        if (dist > touchSlop) {
            hasMovedPastSlop = true
        }

        if (animationMode == PageTurnAnimation.CONTINUOUS_SCROLL) {
            val deltaY = y - downY
            downY = y // 相对累加
            val changed = scrollAdapter.onScroll(deltaY)
            if (changed) {
                listener?.onInvalidateRequest()
            }
            return changed
        }

        val changed = currentAnimation.onTouchMove(x, y)
        if (changed) {
            listener?.onPageTurnProgress(currentAnimation.turnDirection, currentAnimation.progress)
            listener?.onInvalidateRequest()
        }
        return changed
    }

    /**
     * 响应手指抬起。
     */
    fun onTouchUp(velocityX: Float = 0f, velocityY: Float = 0f): Boolean {
        val clickDuration = System.currentTimeMillis() - downTime

        // 1. 判断是否判定为点击手势 (Movement < touchSlop 且耗时 < 400ms)
        if (!hasMovedPastSlop && clickDuration < 400L) {
            handleTap(downX, downY)
            return true
        }

        // 2. 垂直连续滚动模式抬起
        if (animationMode == PageTurnAnimation.CONTINUOUS_SCROLL) {
            scrollAdapter.fling(velocityY)
            listener?.onInvalidateRequest()
            return true
        }

        // 3. 横向分页动效抬起
        val triggered = currentAnimation.onTouchUp(velocityX)
        if (triggered) {
            listener?.onInvalidateRequest()
        } else {
            listener?.onPageTurnCanceled()
            listener?.onInvalidateRequest()
        }
        return triggered
    }

    /**
     * 响应手势被外部取消 (如弹出系统对话框)。
     */
    fun onTouchCancel() {
        if (animationMode == PageTurnAnimation.CONTINUOUS_SCROLL) {
            scrollAdapter.stopScroll()
        } else {
            currentAnimation.abortAnimation()
            currentAnimation.reset()
        }
        listener?.onPageTurnCanceled()
        listener?.onInvalidateRequest()
    }

    /**
     * 处理点击热区判定。
     */
    private fun handleTap(x: Float, y: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        when {
            isCenterTap(x, y, w, h) -> {
                listener?.onCenterClicked()
            }
            isLeftTap(x, y, w, h) -> {
                turnPrevious()
            }
            isRightTap(x, y, w, h) -> {
                turnNext()
            }
        }
    }

    /**
     * 判断是否点击在中央控制菜单热区 (25%..75% 宽, 25%..75% 高)。
     */
    fun isCenterTap(x: Float, y: Float, w: Float, h: Float): Boolean {
        val left = w * 0.25f
        val right = w * 0.75f
        val top = h * 0.25f
        val bottom = h * 0.75f
        return x in left..right && y in top..bottom
    }

    /**
     * 判断是否点击在左侧翻向上一页热区 (< 25% 宽)。
     */
    fun isLeftTap(x: Float, y: Float, w: Float, h: Float): Boolean {
        return x < w * 0.25f
    }

    /**
     * 判断是否点击在右侧翻向下一页热区 (> 75% 宽)。
     */
    fun isRightTap(x: Float, y: Float, w: Float, h: Float): Boolean {
        return x > w * 0.75f
    }

    /**
     * 程序触发翻向下一页。
     */
    fun turnNext(durationMs: Long = 260L) {
        if (animationMode == PageTurnAnimation.CONTINUOUS_SCROLL) {
            scrollAdapter.scrollToPage(scrollAdapter.activePageIndex + 1)
            listener?.onInvalidateRequest()
            return
        }

        listener?.onPageTurnStarted(TurnDirection.NEXT)
        currentAnimation.startAutoTurn(TurnDirection.NEXT, durationMs)
        listener?.onInvalidateRequest()
    }

    /**
     * 程序触发翻向上一页。
     */
    fun turnPrevious(durationMs: Long = 260L) {
        if (animationMode == PageTurnAnimation.CONTINUOUS_SCROLL) {
            scrollAdapter.scrollToPage((scrollAdapter.activePageIndex - 1).coerceAtLeast(0))
            listener?.onInvalidateRequest()
            return
        }

        listener?.onPageTurnStarted(TurnDirection.PREVIOUS)
        currentAnimation.startAutoTurn(TurnDirection.PREVIOUS, durationMs)
        listener?.onInvalidateRequest()
    }

    /**
     * 逐帧推移动画状态。
     *
     * @param deltaMs 帧间隔流逝时间
     * @return true 表示仍在动画中需要继续调度下一帧 invalidate；false 表示动画完全结束
     */
    fun stepAnimation(deltaMs: Long): Boolean {
        if (animationMode == PageTurnAnimation.CONTINUOUS_SCROLL) {
            val keepGoing = scrollAdapter.stepFling(deltaMs)
            if (keepGoing) {
                listener?.onInvalidateRequest()
            }
            return keepGoing
        }

        val wasAnimating = currentAnimation.turnState == TurnState.ANIMATING
        val keepGoing = currentAnimation.stepAnimation(deltaMs)

        if (keepGoing) {
            listener?.onPageTurnProgress(currentAnimation.turnDirection, currentAnimation.progress)
            listener?.onInvalidateRequest()
        } else if (wasAnimating) {
            // 动画刚从 ANIMATING 转入 IDLE
            val direction = currentAnimation.turnDirection
            val isCommit = isAnimationCommitted()
            if (isCommit && direction != TurnDirection.NONE) {
                listener?.onPageTurnCompleted(direction)
            } else {
                listener?.onPageTurnCanceled()
            }
            currentAnimation.reset()
            listener?.onInvalidateRequest()
        }
        return keepGoing
    }

    /**
     * 检查当前动画是否判定为翻页成功。
     */
    private fun isAnimationCommitted(): Boolean {
        return when (val anim = currentAnimation) {
            is CoverPageAnimation -> anim.isCommitTurn
            is SimulationPageAnimation -> anim.isCommitTurn
            is SlidePageAnimation -> anim.isCommitTurn
            is NonePageAnimation -> anim.isCommitTurn
            else -> false
        }
    }

    /**
     * 绘制当前帧所有页面图层。
     */
    fun draw(canvas: Canvas, renderState: PageRenderState) {
        if (animationMode == PageTurnAnimation.CONTINUOUS_SCROLL) {
            scrollAdapter.draw(canvas, renderState)
        } else {
            currentAnimation.draw(canvas, renderState)
        }
    }

    /**
     * 中断任何正在执行的动画并立即复位。
     */
    fun abortAnimation() {
        if (animationMode == PageTurnAnimation.CONTINUOUS_SCROLL) {
            scrollAdapter.stopScroll()
        } else {
            currentAnimation.abortAnimation()
            currentAnimation.reset()
        }
    }
}
