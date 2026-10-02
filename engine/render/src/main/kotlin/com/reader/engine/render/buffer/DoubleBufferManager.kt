package com.reader.engine.render.buffer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.reader.engine.render.model.TurnDirection

/**
 * 离屏缓冲目标枚举。
 */
enum class BufferType {
    CURRENT,
    NEXT,
    PREVIOUS
}

/**
 * 原生双缓冲/三缓冲离屏位图管理器。
 *
 * 核心技术规范：
 * 1. 离屏预渲染：前后页正文排版只在换页时向离屏位图绘制一次；
 * 2. 杜绝拖拽丢帧：用户在屏幕上手指拖拽、动画插值过程中，仅进行 Matrix 变换与 Path 裁剪，绝不触发排版测算；
 * 3. 零内存抖动 (Zero GC Alloc)：翻页成功后通过指针三向交换复用 Bitmap，不重复销毁与创建大位图；
 * 4. 稳健生命周期：妥善处理屏幕旋转与配置变更时的内存回收 (recycle)。
 */
class DoubleBufferManager {

    var width: Int = 0
        private set
    var height: Int = 0
        private set

    var currentBitmap: Bitmap? = null
        private set

    var nextBitmap: Bitmap? = null
        private set

    var prevBitmap: Bitmap? = null
        private set

    /** 是否已分配就绪 */
    val isReady: Boolean
        get() = currentBitmap != null && !currentBitmap!!.isRecycled &&
                nextBitmap != null && !nextBitmap!!.isRecycled &&
                prevBitmap != null && !prevBitmap!!.isRecycled

    /**
     * 分配或调整离屏缓冲尺寸。
     *
     * @param newWidth 视口像素宽
     * @param newHeight 视口像素高
     * @param config 位图色彩配置（默认 RGB_565 降低内存，或 ARGB_8888 保证最高画质）
     */
    fun setup(
        newWidth: Int,
        newHeight: Int,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888
    ) {
        if (newWidth <= 0 || newHeight <= 0) return
        if (newWidth == width && newHeight == height && isReady) return

        recycle()

        width = newWidth
        height = newHeight

        try {
            currentBitmap = Bitmap.createBitmap(width, height, config)
            nextBitmap = Bitmap.createBitmap(width, height, config)
            prevBitmap = Bitmap.createBitmap(width, height, config)
        } catch (e: OutOfMemoryError) {
            // 内存不足时降级为 RGB_565
            recycle()
            currentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            nextBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            prevBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        }
    }

    /**
     * 获取指定缓冲的 Bitmap 实例。
     */
    fun getBitmap(type: BufferType): Bitmap? {
        return when (type) {
            BufferType.CURRENT -> currentBitmap
            BufferType.NEXT -> nextBitmap
            BufferType.PREVIOUS -> prevBitmap
        }
    }

    /**
     * 向目标离屏位图执行绘制闭包。
     */
    fun renderTo(type: BufferType, block: (Canvas) -> Unit) {
        val bmp = getBitmap(type) ?: return
        if (bmp.isRecycled) return

        val canvas = Canvas(bmp)
        block(canvas)
    }

    /**
     * 使用指定背景色清空目标位图。
     */
    fun clear(type: BufferType, color: Int = Color.WHITE) {
        val bmp = getBitmap(type) ?: return
        if (bmp.isRecycled) return
        bmp.eraseColor(color)
    }

    /**
     * 翻向下一页成功后触发：指针交换复用位图缓冲。
     *
     * 交换逻辑：
     * - 原 prevBitmap 移出（成为旧历史）；
     * - 原 currentBitmap 成为新的 prevBitmap；
     * - 原 nextBitmap 成为新的 currentBitmap；
     * - 原 prevBitmap 的内存直接复用为新的 nextBitmap，无需重新分配！
     */
    fun swapNext() {
        val oldPrev = prevBitmap
        val oldCur = currentBitmap
        val oldNext = nextBitmap

        prevBitmap = oldCur
        currentBitmap = oldNext
        nextBitmap = oldPrev // 复用内存供预加载下一页
    }

    /**
     * 翻向上一页成功后触发：指针交换复用位图缓冲。
     *
     * 交换逻辑：
     * - 原 nextBitmap 移出；
     * - 原 currentBitmap 成为新的 nextBitmap；
     * - 原 prevBitmap 成为新的 currentBitmap；
     * - 原 nextBitmap 的内存直接复用为新的 prevBitmap！
     */
    fun swapPrevious() {
        val oldNext = nextBitmap
        val oldCur = currentBitmap
        val oldPrev = prevBitmap

        nextBitmap = oldCur
        currentBitmap = oldPrev
        prevBitmap = oldNext // 复用内存供预加载上一页
    }

    /**
     * 根据翻页方向执行对应的位图指针交换。
     */
    fun swapForDirection(direction: TurnDirection) {
        when (direction) {
            TurnDirection.NEXT -> swapNext()
            TurnDirection.PREVIOUS -> swapPrevious()
            TurnDirection.NONE -> Unit
        }
    }

    /**
     * 回收所有位图内存，防止泄漏。
     */
    fun recycle() {
        currentBitmap?.let { if (!it.isRecycled) it.recycle() }
        nextBitmap?.let { if (!it.isRecycled) it.recycle() }
        prevBitmap?.let { if (!it.isRecycled) it.recycle() }

        currentBitmap = null
        nextBitmap = null
        prevBitmap = null
    }
}
