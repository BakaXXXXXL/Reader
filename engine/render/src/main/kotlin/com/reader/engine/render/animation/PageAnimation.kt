package com.reader.engine.render.animation

import android.graphics.Canvas
import com.reader.engine.render.model.PageRenderState
import com.reader.engine.render.model.TurnDirection
import com.reader.engine.render.model.TurnState

/**
 * 翻页动效抽象基类。
 * 规范各类翻页模式（覆盖、仿真、平移、垂直滚动、无动画）的状态流转、手势拦截与 Canvas 绘制管道。
 */
abstract class PageAnimation {

    /** 视口像素宽度 */
    var width: Int = 0
        protected set

    /** 视口像素高度 */
    var height: Int = 0
        protected set

    /** 当前翻页方向 */
    var turnDirection: TurnDirection = TurnDirection.NONE
        protected set

    /** 当前动效状态机状态 */
    var turnState: TurnState = TurnState.IDLE
        protected set

    /** 当前翻页进度 (0.0f .. 1.0f) */
    var progress: Float = 0f
        protected set

    /** 是否正处于动画或拖拽流转中 */
    val isRunning: Boolean
        get() = turnState != TurnState.IDLE

    /**
     * 初始化或更新视口尺寸。
     */
    open fun setup(viewportWidth: Int, viewportHeight: Int) {
        this.width = viewportWidth
        this.height = viewportHeight
    }

    /**
     * 响应手指按下手势。
     *
     * @param x 按下 X 坐标
     * @param y 按下 Y 坐标
     */
    abstract fun onTouchDown(x: Float, y: Float)

    /**
     * 响应手指拖拽移动手势。
     *
     * @param x 当前手指 X 坐标
     * @param y 当前手指 Y 坐标
     * @return 是否消费并引起画面变化
     */
    abstract fun onTouchMove(x: Float, y: Float): Boolean

    /**
     * 响应手指抬起手势。
     * 根据拖动距离与释放初速度决定是完成翻页还是弹性回弹复位。
     *
     * @param velocityX 手指抬起时的水平初速度 (px/s)
     * @return 是否触发了回弹或翻页动画
     */
    abstract fun onTouchUp(velocityX: Float = 0f): Boolean

    /**
     * 编程式启动自动翻页（如点击屏幕左右两侧翻页区或音量键翻页）。
     *
     * @param direction 翻页方向 [TurnDirection.NEXT] 或 [TurnDirection.PREVIOUS]
     * @param durationMs 动画持续时长 (毫秒)
     */
    abstract fun startAutoTurn(direction: TurnDirection, durationMs: Long = 280L)

    /**
     * 推进动画帧状态（步进 deltaMs 毫秒）。
     *
     * @param deltaMs 距上一帧的流逝毫秒数
     * @return true 表示动画仍在进行需持续重绘；false 表示动画已完全结束并复位 IDLE
     */
    abstract fun stepAnimation(deltaMs: Long): Boolean

    /**
     * 在目标 Canvas 上绘制当前帧的翻页图层。
     *
     * @param canvas 绘制画布
     * @param renderState 当前阅读渲染与位图状态
     */
    abstract fun draw(canvas: Canvas, renderState: PageRenderState)

    /**
     * 中断并强制结束当前执行的动画。
     */
    abstract fun abortAnimation()

    /**
     * 重置内部所有手势与几何计算状态至初始 IDLE。
     */
    open fun reset() {
        turnState = TurnState.IDLE
        turnDirection = TurnDirection.NONE
        progress = 0f
    }
}
