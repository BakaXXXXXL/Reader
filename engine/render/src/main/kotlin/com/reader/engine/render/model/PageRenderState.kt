package com.reader.engine.render.model

import android.graphics.Bitmap
import com.reader.core.model.ReaderConfig
import com.reader.core.model.ReaderThemePreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 阅读渲染状态数据模型。
 * 承载当前视口尺寸、前后页位图双缓冲引用以及排版元信息，为 Canvas 绘制与翻页动效提供完整状态驱动。
 *
 * @property currentBitmap 当前显示页离屏位图缓冲
 * @property nextBitmap 下一页预渲染位图缓冲（翻向下一页时使用）
 * @property prevBitmap 上一页预渲染位图缓冲（翻向上一页时使用）
 * @property bookTitle 书籍标题
 * @property chapterTitle 当前章节名称
 * @property pageIndexInChapter 当前页在章节内的索引 (0-based)
 * @property totalPagesInChapter 当前章节总页数 (>= 1)
 * @property chapterIndex 当前章节序号 (0-based)
 * @property totalChapters 全书总章节数 (>= 1)
 * @property progressionRate 全书或章节阅读进度比例 (0.0f .. 1.0f)
 * @property batteryStatus 电池状态（电量百分比与充电标识）
 * @property currentTimeMillis 当前系统时间戳
 * @property themePreset 当前阅读背景与文字主题预设
 * @property readerConfig 全局阅读排版配置
 * @property headerFooterConfig 页眉页脚布局绘制配置
 * @property viewportWidth 视口渲染区域像素宽度
 * @property viewportHeight 视口渲染区域像素高度
 */
data class PageRenderState(
    val currentBitmap: Bitmap? = null,
    val nextBitmap: Bitmap? = null,
    val prevBitmap: Bitmap? = null,
    val bookTitle: String = "",
    val chapterTitle: String = "",
    val pageIndexInChapter: Int = 0,
    val totalPagesInChapter: Int = 1,
    val chapterIndex: Int = 0,
    val totalChapters: Int = 1,
    val progressionRate: Float = 0f,
    val batteryStatus: BatteryStatus = BatteryStatus.FULL,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val themePreset: ReaderThemePreset = ReaderThemePreset.DEFAULT_LIGHT,
    val readerConfig: ReaderConfig = ReaderConfig.DEFAULT,
    val headerFooterConfig: HeaderFooterConfig = HeaderFooterConfig.DEFAULT,
    val viewportWidth: Int = 0,
    val viewportHeight: Int = 0
) {
    /**
     * 当前是否存在上一页可供翻阅。
     */
    val hasPreviousPage: Boolean
        get() = pageIndexInChapter > 0 || chapterIndex > 0

    /**
     * 当前是否存在下一页可供翻阅。
     */
    val hasNextPage: Boolean
        get() = (pageIndexInChapter + 1) < totalPagesInChapter || (chapterIndex + 1) < totalChapters

    /**
     * 校验当前主位图是否就绪且尺寸匹配。
     */
    val isCurrentBitmapValid: Boolean
        get() = currentBitmap != null &&
                !currentBitmap.isRecycled &&
                (viewportWidth <= 0 || currentBitmap.width == viewportWidth) &&
                (viewportHeight <= 0 || currentBitmap.height == viewportHeight)

    /**
     * 根据翻页方向获取目标页位图。
     */
    fun getTargetBitmap(direction: TurnDirection): Bitmap? {
        return when (direction) {
            TurnDirection.NEXT -> nextBitmap
            TurnDirection.PREVIOUS -> prevBitmap
            TurnDirection.NONE -> null
        }
    }

    /**
     * 格式化系统时钟时间字符串（默认 "HH:mm"）。
     */
    fun formatTime(): String {
        return timeFormatter.format(Date(currentTimeMillis))
    }

    /**
     * 格式化页码与进度文本（例如："第 5/24 页  20.8%"）。
     */
    fun formatPageProgress(): String {
        val currentPageNumber = pageIndexInChapter + 1
        val safeTotal = totalPagesInChapter.coerceAtLeast(1)
        val percent = (progressionRate * 100f).coerceIn(0f, 100f)
        return "第 $currentPageNumber/$safeTotal 页  ${String.format(Locale.getDefault(), "%.1f", percent)}%"
    }

    companion object {
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        val EMPTY = PageRenderState()
    }
}
