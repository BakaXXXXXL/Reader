package com.reader.engine.render.animation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.reader.engine.render.model.PageRenderState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * 可见页面信息（用于连续滚动时多页跨屏显示与预加载）。
 *
 * @property pageIndex 页面索引
 * @property topY 页面顶部在视口中的绝对 Y 坐标
 * @property bottomY 页面底部在视口中的绝对 Y 坐标
 * @property height 页面高度
 */
data class VisiblePageInfo(
    val pageIndex: Int,
    val topY: Float,
    val bottomY: Float,
    val height: Float
)

/**
 * 垂直连续滚动适配器接口与状态机实现。
 * 为现代小说读者提供如网页/长图文般的无级顺滑纵向滚动阅读体验。
 */
interface ScrollPageAdapter {

    /** 当前垂直滚动总偏移量 (>= 0) */
    val scrollY: Float

    /** 当前内容总高度 */
    val totalContentHeight: Float

    /** 页面与页面之间的间隙像素 */
    var pageGap: Int

    /** 当前主阅读页面索引 */
    val activePageIndex: Int

    /**
     * 更新视口与页面配置。
     */
    fun setup(viewportWidth: Int, viewportHeight: Int, pageCount: Int)

    /**
     * 处理手势拖拽滚动。
     *
     * @param deltaY 纵向位移增量 (向下拖动为正，向上滑动内容为负)
     */
    fun onScroll(deltaY: Float): Boolean

    /**
     * 触发惯性滑动 (Fling)。
     *
     * @param velocityY 初始垂直速度 (px/s)
     */
    fun fling(velocityY: Float)

    /**
     * 步进惯性滑动帧。
     *
     * @param deltaMs 流逝毫秒数
     * @return true 表示惯性滑动仍在进行；false 表示静止
     */
    fun stepFling(deltaMs: Long): Boolean

    /**
     * 获取当前视口中可见的页面信息列表。
     */
    fun getVisiblePages(): List<VisiblePageInfo>

    /**
     * 在 Canvas 上绘制跨视口的连续页面图层。
     */
    fun draw(canvas: Canvas, renderState: PageRenderState)

    /**
     * 停止正在进行的惯性滑动。
     */
    fun stopScroll()

    /**
     * 跳转至指定页。
     */
    fun scrollToPage(pageIndex: Int)
}

/**
 * 垂直连续滚动默认高性能实现类。
 */
class DefaultScrollPageAdapter : ScrollPageAdapter {

    var width: Int = 0
        private set
    var height: Int = 0
        private set

    override var scrollY: Float = 0f
        private set

    override var totalContentHeight: Float = 0f
        private set

    override var pageGap: Int = 32

    override var activePageIndex: Int = 0
        private set

    private var pageCount: Int = 1

    /** 惯性当前垂直速度 (px/s) */
    private var currentVelocityY: Float = 0f

    /** 是否处于惯性滑动状态 */
    var isFlinging: Boolean = false
        private set

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val gapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(30, 0, 0, 0)
    }

    private val srcRect = Rect()
    private val dstRect = Rect()

    override fun setup(viewportWidth: Int, viewportHeight: Int, pageCount: Int) {
        this.width = viewportWidth
        this.height = viewportHeight
        this.pageCount = max(1, pageCount)
        recomputeTotalHeight()
    }

    private fun recomputeTotalHeight() {
        val pageH = max(1, height)
        totalContentHeight = pageCount * pageH + (pageCount - 1) * pageGap.toFloat()
        clampScroll()
    }

    private fun clampScroll() {
        val maxScroll = max(0f, totalContentHeight - height)
        scrollY = scrollY.coerceIn(0f, maxScroll)
        updateActivePage()
    }

    private fun updateActivePage() {
        if (height <= 0) return
        val itemHeight = height + pageGap
        val currentCenterY = scrollY + height / 2f
        activePageIndex = (currentCenterY / itemHeight).toInt().coerceIn(0, pageCount - 1)
    }

    override fun onScroll(deltaY: Float): Boolean {
        stopScroll()
        val oldScroll = scrollY
        scrollY -= deltaY // 向上推内容，scrollY 增大
        clampScroll()
        return abs(scrollY - oldScroll) > 0.01f
    }

    override fun fling(velocityY: Float) {
        if (abs(velocityY) < 100f) {
            stopScroll()
            return
        }
        currentVelocityY = -velocityY // 速度方向与位移方向换算
        isFlinging = true
    }

    override fun stepFling(deltaMs: Long): Boolean {
        if (!isFlinging) return false

        val dt = deltaMs / 1000f
        // 摩擦阻尼系数
        val friction = 0.94f
        val displacement = currentVelocityY * dt
        scrollY += displacement

        // 模拟物理衰减
        currentVelocityY *= friction.toDouble().pow(deltaMs / 16.0).toFloat()

        val maxScroll = max(0f, totalContentHeight - height)
        if (scrollY <= 0f || scrollY >= maxScroll || abs(currentVelocityY) < 40f) {
            clampScroll()
            stopScroll()
            return false
        }

        clampScroll()
        return true
    }

    override fun stopScroll() {
        isFlinging = false
        currentVelocityY = 0f
    }

    override fun scrollToPage(pageIndex: Int) {
        stopScroll()
        val safeIndex = pageIndex.coerceIn(0, pageCount - 1)
        val itemHeight = height + pageGap
        scrollY = safeIndex * itemHeight.toFloat()
        clampScroll()
    }

    override fun getVisiblePages(): List<VisiblePageInfo> {
        if (height <= 0) return emptyList()

        val list = mutableListOf<VisiblePageInfo>()
        val itemHeight = height + pageGap

        for (i in 0 until pageCount) {
            val pageTop = i * itemHeight - scrollY
            val pageBottom = pageTop + height

            // 判断是否在当前视口范围内可见 (包含上下微冗余)
            if (pageBottom >= -10f && pageTop <= height + 10f) {
                list.add(VisiblePageInfo(pageIndex = i, topY = pageTop, bottomY = pageBottom, height = height.toFloat()))
            }
        }
        return list
    }

    override fun draw(canvas: Canvas, renderState: PageRenderState) {
        if (width <= 0 || height <= 0) return

        val visiblePages = getVisiblePages()
        val currentBitmap = renderState.currentBitmap

        for (pageInfo in visiblePages) {
            val pageTopInt = pageInfo.topY.toInt()
            val pageBottomInt = pageInfo.bottomY.toInt()

            // 绘制页面正文位图
            if (currentBitmap != null && !currentBitmap.isRecycled) {
                srcRect.set(0, 0, currentBitmap.width, currentBitmap.height)
                dstRect.set(0, pageTopInt, width, pageBottomInt)
                canvas.drawBitmap(currentBitmap, srcRect, dstRect, bitmapPaint)
            }

            // 绘制页间分割带
            if (pageInfo.pageIndex < pageCount - 1 && pageGap > 0) {
                val gapTop = pageBottomInt.toFloat()
                val gapBottom = gapTop + pageGap
                canvas.drawRect(0f, gapTop, width.toFloat(), gapBottom, gapPaint)
            }
        }
    }
}
