package com.reader.engine.render

import com.google.common.truth.Truth.assertThat
import com.reader.engine.render.animation.CoverPageAnimation
import com.reader.engine.render.model.TurnDirection
import com.reader.engine.render.model.TurnState
import org.junit.Before
import org.junit.Test

/**
 * 默认平滑横向覆盖模式状态流转与动画插值单元测试。
 */
class CoverPageAnimationTest {

    private lateinit var coverAnim: CoverPageAnimation
    private val width = 1000
    private val height = 2000

    @Before
    fun setUp() {
        coverAnim = CoverPageAnimation()
        coverAnim.setup(width, height)
    }

    @Test
    fun testInitialState() {
        assertThat(coverAnim.turnState).isEqualTo(TurnState.IDLE)
        assertThat(coverAnim.turnDirection).isEqualTo(TurnDirection.NONE)
        assertThat(coverAnim.progress).isEqualTo(0f)
        assertThat(coverAnim.displacementX).isEqualTo(0f)
    }

    @Test
    fun testTouchDragNext() {
        coverAnim.onTouchDown(900f, 1000f)
        assertThat(coverAnim.turnState).isEqualTo(TurnState.DRAGGING)

        // 向左拖动 300px
        val changed = coverAnim.onTouchMove(600f, 1000f)
        assertThat(changed).isTrue()
        assertThat(coverAnim.turnDirection).isEqualTo(TurnDirection.NEXT)
        assertThat(coverAnim.displacementX).isEqualTo(-300f)
        assertThat(coverAnim.progress).isWithin(0.001f).of(0.3f)
    }

    @Test
    fun testTouchDragPrevious() {
        coverAnim.onTouchDown(100f, 1000f)
        assertThat(coverAnim.turnState).isEqualTo(TurnState.DRAGGING)

        // 向右拖动 250px
        val changed = coverAnim.onTouchMove(350f, 1000f)
        assertThat(changed).isTrue()
        assertThat(coverAnim.turnDirection).isEqualTo(TurnDirection.PREVIOUS)
        assertThat(coverAnim.displacementX).isEqualTo(250f)
        assertThat(coverAnim.progress).isWithin(0.001f).of(0.25f)
    }

    @Test
    fun testTouchUp_commitWhenOverThreshold() {
        coverAnim.onTouchDown(900f, 1000f)
        // 拖动 300px (30% > 22% 阈值)
        coverAnim.onTouchMove(600f, 1000f)
        val triggered = coverAnim.onTouchUp(velocityX = 0f)

        assertThat(triggered).isTrue()
        assertThat(coverAnim.isCommitTurn).isTrue()
        assertThat(coverAnim.turnState).isEqualTo(TurnState.ANIMATING)
    }

    @Test
    fun testTouchUp_cancelWhenBelowThreshold() {
        coverAnim.onTouchDown(900f, 1000f)
        // 仅轻微拖动 50px (5% < 22%)
        coverAnim.onTouchMove(850f, 1000f)
        val triggered = coverAnim.onTouchUp(velocityX = 0f)

        assertThat(triggered).isTrue()
        assertThat(coverAnim.isCommitTurn).isFalse() // 弹性回弹复位
        assertThat(coverAnim.turnState).isEqualTo(TurnState.ANIMATING)
    }

    @Test
    fun testTouchUp_commitOnHighVelocity() {
        coverAnim.onTouchDown(900f, 1000f)
        // 虽仅拖动 80px，但快速甩手速度 -1200px/s
        coverAnim.onTouchMove(820f, 1000f)
        val triggered = coverAnim.onTouchUp(velocityX = -1200f)

        assertThat(triggered).isTrue()
        assertThat(coverAnim.isCommitTurn).isTrue()
    }

    @Test
    fun testStepAnimationCompletion() {
        coverAnim.startAutoTurn(TurnDirection.NEXT, durationMs = 200L)
        assertThat(coverAnim.turnState).isEqualTo(TurnState.ANIMATING)

        // 步进 100ms (50%)
        val runningHalf = coverAnim.stepAnimation(100L)
        assertThat(runningHalf).isTrue()
        assertThat(coverAnim.progress).isGreaterThan(0.5f) // 三次缓动前期速度更快

        // 步进剩余 150ms (超过总时长 200ms)
        val runningComplete = coverAnim.stepAnimation(150L)
        assertThat(runningComplete).isFalse()
        assertThat(coverAnim.turnState).isEqualTo(TurnState.IDLE)
        assertThat(coverAnim.displacementX).isEqualTo(-width.toFloat())
    }
}
