package com.reader.engine.render

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.PageTurnAnimation
import com.reader.engine.render.animation.CoverPageAnimation
import com.reader.engine.render.animation.NonePageAnimation
import com.reader.engine.render.animation.SimulationPageAnimation
import com.reader.engine.render.animation.SlidePageAnimation
import com.reader.engine.render.controller.OnPageTurnListener
import com.reader.engine.render.controller.PageTurnController
import com.reader.engine.render.model.TurnDirection
import org.junit.Before
import org.junit.Test

/**
 * 翻页动效总控状态机与手势区域识别单元测试。
 */
class PageTurnControllerTest {

    private lateinit var controller: PageTurnController
    private val width = 1000
    private val height = 2000

    @Before
    fun setUp() {
        controller = PageTurnController()
        controller.setup(width, height)
    }

    @Test
    fun testTapZoneDetection() {
        val w = width.toFloat()
        val h = height.toFloat()

        // 1. 中央区域 (25%..75% 宽, 25%..75% 高)
        assertThat(controller.isCenterTap(500f, 1000f, w, h)).isTrue()
        assertThat(controller.isCenterTap(300f, 500f, w, h)).isTrue()
        assertThat(controller.isCenterTap(100f, 1000f, w, h)).isFalse()

        // 2. 左侧翻上一页区域 (< 25% 宽)
        assertThat(controller.isLeftTap(100f, 1000f, w, h)).isTrue()
        assertThat(controller.isLeftTap(240f, 500f, w, h)).isTrue()
        assertThat(controller.isLeftTap(300f, 1000f, w, h)).isFalse()

        // 3. 右侧翻下一页区域 (> 75% 宽)
        assertThat(controller.isRightTap(800f, 1000f, w, h)).isTrue()
        assertThat(controller.isRightTap(950f, 200f, w, h)).isTrue()
        assertThat(controller.isRightTap(700f, 1000f, w, h)).isFalse()
    }

    @Test
    fun testModeSwitching() {
        // 初始为默认 COVER
        assertThat(controller.animationMode).isEqualTo(PageTurnAnimation.COVER)
        assertThat(controller.currentAnimation).isInstanceOf(CoverPageAnimation::class.java)

        // 切换为 SIMULATION
        controller.setAnimationMode(PageTurnAnimation.SIMULATION)
        assertThat(controller.animationMode).isEqualTo(PageTurnAnimation.SIMULATION)
        assertThat(controller.currentAnimation).isInstanceOf(SimulationPageAnimation::class.java)

        // 切换为 SLIDE
        controller.setAnimationMode(PageTurnAnimation.SLIDE)
        assertThat(controller.animationMode).isEqualTo(PageTurnAnimation.SLIDE)
        assertThat(controller.currentAnimation).isInstanceOf(SlidePageAnimation::class.java)

        // 切换为 NONE
        controller.setAnimationMode(PageTurnAnimation.NONE)
        assertThat(controller.animationMode).isEqualTo(PageTurnAnimation.NONE)
        assertThat(controller.currentAnimation).isInstanceOf(NonePageAnimation::class.java)
    }

    @Test
    fun testAutoTurnAndCallbackLifecycle() {
        var startDirection: TurnDirection? = null
        var completedDirection: TurnDirection? = null

        controller.listener = object : OnPageTurnListener {
            override fun onPageTurnStarted(direction: TurnDirection) {
                startDirection = direction
            }

            override fun onPageTurnCompleted(direction: TurnDirection) {
                completedDirection = direction
            }
        }

        // 触发下一页自动翻页
        controller.turnNext(durationMs = 200L)
        assertThat(startDirection).isEqualTo(TurnDirection.NEXT)

        // 步进至动画结束
        val running = controller.stepAnimation(250L)
        assertThat(running).isFalse()
        assertThat(completedDirection).isEqualTo(TurnDirection.NEXT)
    }

    @Test
    fun testCenterTapCallback() {
        var centerClicked = false
        controller.listener = object : OnPageTurnListener {
            override fun onCenterClicked() {
                centerClicked = true
            }
        }

        // 模拟在中央按下并无移动抬起
        controller.onTouchDown(500f, 1000f)
        controller.onTouchUp()

        assertThat(centerClicked).isTrue()
    }
}
