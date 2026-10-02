package com.reader.engine.render

import com.google.common.truth.Truth.assertThat
import com.reader.engine.render.animation.SimulationPageAnimation
import com.reader.engine.render.model.TouchCorner
import com.reader.engine.render.model.TurnDirection
import com.reader.engine.render.model.TurnState
import org.junit.Before
import org.junit.Test

/**
 * 3D 仿真翻页动画器状态与步进测试。
 */
class SimulationPageAnimationTest {

    private lateinit var simAnim: SimulationPageAnimation
    private val width = 1000
    private val height = 2000

    @Before
    fun setUp() {
        simAnim = SimulationPageAnimation()
        simAnim.setup(width, height)
    }

    @Test
    fun testInitialState() {
        assertThat(simAnim.turnState).isEqualTo(TurnState.IDLE)
        assertThat(simAnim.turnDirection).isEqualTo(TurnDirection.NONE)
        assertThat(simAnim.progress).isEqualTo(0f)
    }

    @Test
    fun testTouchDragSimulation() {
        simAnim.onTouchDown(950f, 1800f)
        assertThat(simAnim.turnState).isEqualTo(TurnState.DRAGGING)
        assertThat(simAnim.touchCorner).isEqualTo(TouchCorner.BOTTOM_RIGHT)

        val changed = simAnim.onTouchMove(700f, 1850f)
        assertThat(changed).isTrue()
        assertThat(simAnim.turnDirection).isEqualTo(TurnDirection.NEXT)
        assertThat(simAnim.progress).isGreaterThan(0.2f)
        assertThat(simAnim.currentCurlPoints.isValid).isTrue()
    }

    @Test
    fun testStartAutoTurnAndStep() {
        simAnim.startAutoTurn(TurnDirection.NEXT, durationMs = 300L)
        assertThat(simAnim.turnState).isEqualTo(TurnState.ANIMATING)
        assertThat(simAnim.isCommitTurn).isTrue()

        // 步进 150ms
        val running = simAnim.stepAnimation(150L)
        assertThat(running).isTrue()
        assertThat(simAnim.currentCurlPoints.isValid).isTrue()

        // 步进剩余 200ms
        val completed = simAnim.stepAnimation(200L)
        assertThat(completed).isFalse()
        assertThat(simAnim.turnState).isEqualTo(TurnState.IDLE)
    }
}
