package com.reader.core.designsystem.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReaderTapActionTest {

    private val containerSize = Size(1000f, 2000f)

    @Test
    fun `left region resolves to previous page`() {
        val tap = Offset(150f, 1000f) // 15% from left
        val action = ReaderTapAction.resolve(tap, containerSize)
        assertThat(action).isEqualTo(ReaderTapAction.PREVIOUS_PAGE)
    }

    @Test
    fun `center region resolves to toggle menu`() {
        val tap = Offset(500f, 1000f) // 50% center
        val action = ReaderTapAction.resolve(tap, containerSize)
        assertThat(action).isEqualTo(ReaderTapAction.TOGGLE_MENU)
    }

    @Test
    fun `right region resolves to next page`() {
        val tap = Offset(850f, 1000f) // 85% from left
        val action = ReaderTapAction.resolve(tap, containerSize)
        assertThat(action).isEqualTo(ReaderTapAction.NEXT_PAGE)
    }

    @Test
    fun `empty or zero width returns none`() {
        val tap = Offset(100f, 100f)
        val action = ReaderTapAction.resolve(tap, Size.Zero)
        assertThat(action).isEqualTo(ReaderTapAction.NONE)
    }

    @Test
    fun `custom ratios work properly`() {
        // Custom 20% left, 80% right
        val tapLeft = Offset(180f, 500f)
        val tapMiddle = Offset(250f, 500f)
        val tapRight = Offset(820f, 500f)

        assertThat(ReaderTapAction.resolve(tapLeft, containerSize, 0.2f, 0.8f))
            .isEqualTo(ReaderTapAction.PREVIOUS_PAGE)
        assertThat(ReaderTapAction.resolve(tapMiddle, containerSize, 0.2f, 0.8f))
            .isEqualTo(ReaderTapAction.TOGGLE_MENU)
        assertThat(ReaderTapAction.resolve(tapRight, containerSize, 0.2f, 0.8f))
            .isEqualTo(ReaderTapAction.NEXT_PAGE)
    }
}
