package com.reader.engine.render

import android.graphics.Paint
import com.google.common.truth.Truth.assertThat
import com.reader.engine.render.headerfooter.HeaderFooterRenderer
import com.reader.engine.render.model.BatteryStatus
import com.reader.engine.render.model.PageRenderState
import org.junit.Test
import java.util.Calendar

/**
 * 页眉页脚文本与电量格式化单元测试。
 */
class HeaderFooterRendererTest {

    @Test
    fun testBatteryStatus_properties() {
        val full = BatteryStatus.FULL
        assertThat(full.level).isEqualTo(100)
        assertThat(full.isLowBattery).isFalse()

        val low = BatteryStatus(level = 12, isCharging = false)
        assertThat(low.isLowBattery).isTrue()

        val chargingLow = BatteryStatus(level = 10, isCharging = true)
        assertThat(chargingLow.isLowBattery).isFalse() // 充电中不视为告警低电
    }

    @Test
    fun testBatteryStatus_boundsEnforcement() {
        try {
            BatteryStatus(level = 105)
            error("Should have failed on level > 100")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Battery level must be in 0..100")
        }

        try {
            BatteryStatus(level = -5)
            error("Should have failed on level < 0")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Battery level must be in 0..100")
        }
    }

    @Test
    fun testPageRenderState_formatPageProgress() {
        val state = PageRenderState(
            pageIndexInChapter = 4,
            totalPagesInChapter = 20,
            progressionRate = 0.25f
        )
        // 第 5/20 页  25.0%
        val text = state.formatPageProgress()
        assertThat(text).contains("第 5/20 页")
        assertThat(text).contains("25.0%")
    }

    @Test
    fun testPageRenderState_formatTime() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        val state = PageRenderState(currentTimeMillis = calendar.timeInMillis)
        val formatted = state.formatTime()
        assertThat(formatted).isEqualTo("14:30")
    }

    @Test
    fun testTruncateTextIfNeeded_shortText() {
        val renderer = HeaderFooterRenderer()
        val paint = Paint()
        val text = "第一章 序幕"
        val result = renderer.truncateTextIfNeeded(text, maxWidth = 500f, paint = paint)
        assertThat(result).isEqualTo(text)
    }

    @Test
    fun testTruncateTextIfNeeded_emptyText() {
        val renderer = HeaderFooterRenderer()
        val paint = Paint()
        val result = renderer.truncateTextIfNeeded("", maxWidth = 100f, paint = paint)
        assertThat(result).isEmpty()
    }
}
