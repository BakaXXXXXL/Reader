package com.reader.engine.render

import com.reader.engine.render.animation.CurlMathHelper
import com.reader.engine.render.model.TouchCorner
import com.reader.engine.render.model.TouchPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * 3D 仿真翻页贝塞尔曲线与几何建模算法单元测试。
 */
class CurlMathHelperTest {

    private val screenWidth = 1080f
    private val screenHeight = 2400f

    @Test
    fun testDetermineCorner() {
        // 右下角
        val cornerBR = CurlMathHelper.determineCorner(900f, 2000f, screenWidth, screenHeight)
        assertThat(cornerBR).isEqualTo(TouchCorner.BOTTOM_RIGHT)

        // 右上角
        val cornerTR = CurlMathHelper.determineCorner(900f, 300f, screenWidth, screenHeight)
        assertThat(cornerTR).isEqualTo(TouchCorner.TOP_RIGHT)

        // 左下角
        val cornerBL = CurlMathHelper.determineCorner(100f, 2000f, screenWidth, screenHeight)
        assertThat(cornerBL).isEqualTo(TouchCorner.BOTTOM_LEFT)

        // 左上角
        val cornerTL = CurlMathHelper.determineCorner(100f, 300f, screenWidth, screenHeight)
        assertThat(cornerTL).isEqualTo(TouchCorner.TOP_LEFT)
    }

    @Test
    fun testGetCornerPoint() {
        val ptBR = CurlMathHelper.getCornerPoint(TouchCorner.BOTTOM_RIGHT, screenWidth, screenHeight)
        assertThat(ptBR.x).isEqualTo(screenWidth)
        assertThat(ptBR.y).isEqualTo(screenHeight)

        val ptTR = CurlMathHelper.getCornerPoint(TouchCorner.TOP_RIGHT, screenWidth, screenHeight)
        assertThat(ptTR.x).isEqualTo(screenWidth)
        assertThat(ptTR.y).isEqualTo(0f)

        val ptBL = CurlMathHelper.getCornerPoint(TouchCorner.BOTTOM_LEFT, screenWidth, screenHeight)
        assertThat(ptBL.x).isEqualTo(0f)
        assertThat(ptBL.y).isEqualTo(screenHeight)

        val ptTL = CurlMathHelper.getCornerPoint(TouchCorner.TOP_LEFT, screenWidth, screenHeight)
        assertThat(ptTL.x).isEqualTo(0f)
        assertThat(ptTL.y).isEqualTo(0f)
    }

    @Test
    fun testConstrainTouchPoint_preventsCoaxialDivisionByZero() {
        val f = TouchPoint(screenWidth, screenHeight)
        // 触控点与 F 点完全重合
        val constrained = CurlMathHelper.constrainTouchPoint(
            rawA = TouchPoint(screenWidth, screenHeight),
            f = f,
            width = screenWidth,
            height = screenHeight
        )

        // 必须产生微小扰动偏移，不能完全相等以防止除以零
        assertThat(constrained.x).isLessThan(f.x)
        assertThat(constrained.y).isLessThan(f.y)
        assertThat(constrained.isValid).isTrue()
    }

    @Test
    fun testConstrainTouchPoint_clampsOverExtension() {
        val f = TouchPoint(screenWidth, screenHeight)
        // 超出极远距离
        val raw = TouchPoint(-5000f, -5000f)
        val constrained = CurlMathHelper.constrainTouchPoint(
            rawA = raw,
            f = f,
            width = screenWidth,
            height = screenHeight
        )

        assertThat(constrained.isValid).isTrue()
        // 距离 F 的长度不得超过屏幕对角线
        val maxDiag = kotlin.math.hypot(screenWidth, screenHeight)
        val actualDist = constrained.distanceTo(f)
        assertThat(actualDist).isAtMost(maxDiag + 1f)
    }

    @Test
    fun testCalculateCurlPoints_bottomRight() {
        val touchX = 700f
        val touchY = 2100f
        val points = CurlMathHelper.calculateCurlPoints(
            touchX = touchX,
            touchY = touchY,
            corner = TouchCorner.BOTTOM_RIGHT,
            width = screenWidth,
            height = screenHeight
        )

        // 1. 验证所有控制点有效性
        assertThat(points.isValid).isTrue()
        assertThat(points.corner).isEqualTo(TouchCorner.BOTTOM_RIGHT)

        // 2. 验证 G 为 AF 中点
        val expectedGx = (points.a.x + points.f.x) / 2f
        val expectedGy = (points.a.y + points.f.y) / 2f
        assertThat(points.g.x).isWithin(0.01f).of(expectedGx)
        assertThat(points.g.y).isWithin(0.01f).of(expectedGy)

        // 3. 验证 E 位于水平底边 (y = screenHeight)
        assertThat(points.e.y).isEqualTo(screenHeight)
        assertThat(points.e.x).isAtMost(screenWidth)

        // 4. 验证 H 位于垂直右边 (x = screenWidth)
        assertThat(points.h.x).isEqualTo(screenWidth)
        assertThat(points.h.y).isAtMost(screenHeight)

        // 5. 验证切点 C 和 J
        assertThat(points.c.y).isEqualTo(screenHeight)
        assertThat(points.j.x).isEqualTo(screenWidth)

        // 6. 验证折角弧度与卷曲深度
        assertThat(points.curlDepth).isGreaterThan(0f)
    }

    @Test
    fun testCalculateCurlPoints_topRight() {
        val touchX = 650f
        val touchY = 400f
        val points = CurlMathHelper.calculateCurlPoints(
            touchX = touchX,
            touchY = touchY,
            corner = TouchCorner.TOP_RIGHT,
            width = screenWidth,
            height = screenHeight
        )

        assertThat(points.isValid).isTrue()
        assertThat(points.corner).isEqualTo(TouchCorner.TOP_RIGHT)
        assertThat(points.f.y).isEqualTo(0f)
        assertThat(points.e.y).isEqualTo(0f)
        assertThat(points.h.x).isEqualTo(screenWidth)
    }

    @Test
    fun testCalculateTurnProgress() {
        // 右下角：从右 (1080) 拖到左 (0)
        val p0 = CurlMathHelper.calculateTurnProgress(TouchPoint(1080f, 2000f), TouchCorner.BOTTOM_RIGHT, screenWidth)
        assertThat(p0).isEqualTo(0f)

        val pHalf = CurlMathHelper.calculateTurnProgress(TouchPoint(540f, 2000f), TouchCorner.BOTTOM_RIGHT, screenWidth)
        assertThat(pHalf).isWithin(0.001f).of(0.5f)

        val p1 = CurlMathHelper.calculateTurnProgress(TouchPoint(0f, 2000f), TouchCorner.BOTTOM_RIGHT, screenWidth)
        assertThat(p1).isEqualTo(1f)

        // 左下角：从左 (0) 拖到右 (1080)
        val pLeft0 = CurlMathHelper.calculateTurnProgress(TouchPoint(0f, 2000f), TouchCorner.BOTTOM_LEFT, screenWidth)
        assertThat(pLeft0).isEqualTo(0f)

        val pLeft1 = CurlMathHelper.calculateTurnProgress(TouchPoint(1080f, 2000f), TouchCorner.BOTTOM_LEFT, screenWidth)
        assertThat(pLeft1).isEqualTo(1f)
    }
}
