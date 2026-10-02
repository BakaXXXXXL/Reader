package com.reader.engine.render.headerfooter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.reader.engine.render.model.BatteryStatus
import com.reader.engine.render.model.HeaderFooterConfig
import com.reader.engine.render.model.PageRenderState
import kotlin.math.max
import kotlin.math.min

/**
 * 页眉与页脚实时 Canvas 绘制器。
 *
 * 绘制规范：
 * 1. 严格绘制在页面边距内 (Margin)，绝不侵占正文排版网格；
 * 2. 页眉：绘制当前章节名称（自动避让与单行截断省略号）；
 * 3. 页脚：
 *    - 左侧：微型硬件电池图标（外壳圆角矩形、正极端点凸起、电量动态填充、低电量红色告警、充电闪电指示）；
 *    - 中间：当前系统时钟 (HH:mm)；
 *    - 右侧：本章进度百分比与精确物理页码（如 "第 3/18 页  16.7%"）；
 * 4. 极致性能：全量复用 Paint/RectF/Path，杜绝每帧动态分配对象。
 */
class HeaderFooterRenderer(
    private var density: Float = 2.5f,
    private var scaledDensity: Float = 2.5f
) {

    /** 辅助文本画笔 */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
    }

    /** 电池外壳与描边画笔 */
    private val batteryStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    /** 电池电量填充画笔 */
    private val batteryFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /** 闪电图标画笔 */
    private val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 复用矩形与路径缓存
    private val textBounds = Rect()
    private val batteryBodyRect = RectF()
    private val batteryCapRect = RectF()
    private val batteryLevelRect = RectF()
    private val boltPath = Path()

    /**
     * 更新屏幕显示密度（用于 dp/sp 转像素）。
     */
    fun updateDensity(density: Float, scaledDensity: Float) {
        this.density = max(1f, density)
        this.scaledDensity = max(1f, scaledDensity)
    }

    /**
     * 绘制页眉与页脚。
     *
     * @param canvas 目标 Canvas
     * @param state 渲染状态
     */
    fun draw(canvas: Canvas, state: PageRenderState) {
        val width = state.viewportWidth
        val height = state.viewportHeight
        if (width <= 0 || height <= 0) return

        val config = state.headerFooterConfig
        val colorInt = (state.themePreset.secondaryTextColor and 0xFFFFFFFFL).toInt()

        val textPixelSize = config.fontSizeSp * scaledDensity
        textPaint.textSize = textPixelSize
        textPaint.color = colorInt

        val marginH = config.marginHorizontalDp * density
        val marginV = config.marginVerticalDp * density

        // 1. 绘制顶部页眉
        if (config.showHeader) {
            drawHeader(
                canvas = canvas,
                state = state,
                width = width,
                marginH = marginH,
                marginV = marginV,
                textPixelSize = textPixelSize
            )
        }

        // 2. 绘制底部页脚
        if (config.showFooter) {
            drawFooter(
                canvas = canvas,
                state = state,
                width = width,
                height = height,
                marginH = marginH,
                marginV = marginV,
                textPixelSize = textPixelSize,
                colorInt = colorInt,
                config = config
            )
        }
    }

    /**
     * 绘制顶部页眉（章节名）。
     */
    private fun drawHeader(
        canvas: Canvas,
        state: PageRenderState,
        width: Int,
        marginH: Float,
        marginV: Float,
        textPixelSize: Float
    ) {
        val chapterTitle = state.chapterTitle.ifBlank { state.bookTitle }
        if (chapterTitle.isBlank()) return

        val maxTextWidth = width - marginH * 2f
        val truncatedText = truncateTextIfNeeded(chapterTitle, maxTextWidth, textPaint)

        val fontMetrics = textPaint.fontMetrics
        // 居中于 marginV 区域
        val baselineY = marginV + textPixelSize - fontMetrics.descent

        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(truncatedText, marginH, baselineY, textPaint)
    }

    /**
     * 绘制底部页脚（电池、时间、页码进度）。
     */
    private fun drawFooter(
        canvas: Canvas,
        state: PageRenderState,
        width: Int,
        height: Int,
        marginH: Float,
        marginV: Float,
        textPixelSize: Float,
        colorInt: Int,
        config: HeaderFooterConfig
    ) {
        val fontMetrics = textPaint.fontMetrics
        val footerCenterY = height - marginV - textPixelSize / 2f
        val baselineY = footerCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2f

        // 1. 左侧：电池图标
        var currentLeftX = marginH
        if (config.showBattery) {
            val batteryW = config.batteryIconWidthDp * density
            val batteryH = config.batteryIconHeightDp * density
            drawBatteryIcon(
                canvas = canvas,
                left = currentLeftX,
                centerY = footerCenterY,
                width = batteryW,
                height = batteryH,
                batteryStatus = state.batteryStatus,
                colorInt = colorInt
            )
            currentLeftX += batteryW + 8f * density
        }

        // 2. 中间：系统时间 (HH:mm)
        if (config.showTime) {
            val timeText = state.formatTime()
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(timeText, width / 2f, baselineY, textPaint)
        }

        // 3. 右侧：页码与进度
        if (config.showPageNumber || config.showProgress) {
            val progressText = state.formatPageProgress()
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(progressText, width - marginH, baselineY, textPaint)
        }
    }

    /**
     * 绘制拟真电池图标。
     */
    private fun drawBatteryIcon(
        canvas: Canvas,
        left: Float,
        centerY: Float,
        width: Float,
        height: Float,
        batteryStatus: BatteryStatus,
        colorInt: Int
    ) {
        val strokeWidth = max(1f, 1.2f * density)
        val capWidth = max(1.5f, 2f * density)
        val bodyWidth = width - capWidth - 1f
        val bodyHeight = height
        val cornerRadius = 2.5f * density

        val top = centerY - bodyHeight / 2f
        val bottom = centerY + bodyHeight / 2f

        // 1. 电池主体外壳描边
        batteryStrokePaint.strokeWidth = strokeWidth
        batteryStrokePaint.color = colorInt
        batteryBodyRect.set(left, top, left + bodyWidth, bottom)
        canvas.drawRoundRect(batteryBodyRect, cornerRadius, cornerRadius, batteryStrokePaint)

        // 2. 正极端点凸起
        val capHeight = bodyHeight * 0.44f
        val capTop = centerY - capHeight / 2f
        val capBottom = centerY + capHeight / 2f
        val capLeft = left + bodyWidth
        val capRight = left + width
        batteryCapRect.set(capLeft, capTop, capRight, capBottom)
        batteryFillPaint.color = colorInt
        canvas.drawRoundRect(batteryCapRect, 1.5f * density, 1.5f * density, batteryFillPaint)

        // 3. 内部电量填充
        val innerPadding = strokeWidth + 1.2f * density
        val fillMaxW = bodyWidth - innerPadding * 2f
        val fillH = bodyHeight - innerPadding * 2f
        val fillPercent = (batteryStatus.level / 100f).coerceIn(0f, 1f)
        val currentFillW = fillMaxW * fillPercent

        val fillColor = when {
            batteryStatus.isCharging -> Color.rgb(67, 160, 71) // 充电绿
            batteryStatus.isLowBattery -> Color.rgb(229, 57, 53) // 低电量警示红
            else -> colorInt
        }

        batteryFillPaint.color = fillColor
        if (currentFillW > 0.5f) {
            batteryLevelRect.set(
                left + innerPadding,
                top + innerPadding,
                left + innerPadding + currentFillW,
                top + innerPadding + fillH
            )
            canvas.drawRoundRect(batteryLevelRect, 1.5f * density, 1.5f * density, batteryFillPaint)
        }

        // 4. 充电中绘制微型闪电符号
        if (batteryStatus.isCharging) {
            drawChargingBolt(
                canvas = canvas,
                centerX = left + bodyWidth / 2f,
                centerY = centerY,
                size = bodyHeight * 0.75f
            )
        }
    }

    /**
     * 绘制充电闪电指示图标。
     */
    private fun drawChargingBolt(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        val halfW = size * 0.35f
        val halfH = size * 0.5f

        boltPath.reset()
        boltPath.moveTo(centerX + halfW * 0.2f, centerY - halfH)
        boltPath.lineTo(centerX - halfW, centerY + halfH * 0.1f)
        boltPath.lineTo(centerX, centerY + halfH * 0.1f)
        boltPath.lineTo(centerX - halfW * 0.2f, centerY + halfH)
        boltPath.lineTo(centerX + halfW, centerY - halfH * 0.1f)
        boltPath.lineTo(centerX, centerY - halfH * 0.1f)
        boltPath.close()

        boltPaint.color = Color.WHITE
        canvas.drawPath(boltPath, boltPaint)
    }

    /**
     * 文本超出最大宽度时自动进行末尾省略号截断处理。
     */
    fun truncateTextIfNeeded(text: String, maxWidth: Float, paint: Paint): String {
        if (maxWidth <= 0f || text.isEmpty()) return text
        if (paint.measureText(text) <= maxWidth) return text

        val ellipsis = "…"
        val ellipsisWidth = paint.measureText(ellipsis)
        val availableWidth = maxWidth - ellipsisWidth
        if (availableWidth <= 0f) return ellipsis

        var low = 0
        var high = text.length
        var bestIndex = 0

        while (low <= high) {
            val mid = (low + high) ushr 1
            val sub = text.substring(0, mid)
            if (paint.measureText(sub) <= availableWidth) {
                bestIndex = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return text.substring(0, bestIndex) + ellipsis
    }
}
