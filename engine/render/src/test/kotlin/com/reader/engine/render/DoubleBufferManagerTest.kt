package com.reader.engine.render

import com.google.common.truth.Truth.assertThat
import com.reader.engine.render.buffer.BufferType
import com.reader.engine.render.buffer.DoubleBufferManager
import com.reader.engine.render.model.TurnDirection
import org.junit.Test

/**
 * 离屏双缓冲/三缓冲指针流转单元测试。
 */
class DoubleBufferManagerTest {

    @Test
    fun testBufferTypeEnum() {
        assertThat(BufferType.entries).containsExactly(
            BufferType.CURRENT,
            BufferType.NEXT,
            BufferType.PREVIOUS
        )
    }

    @Test
    fun testInitialBufferState() {
        val manager = DoubleBufferManager()
        assertThat(manager.width).isEqualTo(0)
        assertThat(manager.height).isEqualTo(0)
        assertThat(manager.isReady).isFalse()
        assertThat(manager.currentBitmap).isNull()
        assertThat(manager.nextBitmap).isNull()
        assertThat(manager.prevBitmap).isNull()
    }

    @Test
    fun testSwapPointersCycle() {
        val manager = DoubleBufferManager()
        // 模拟指针交换，测试三向循环轮转
        manager.swapNext()
        assertThat(manager.width).isEqualTo(0)

        manager.swapPrevious()
        assertThat(manager.width).isEqualTo(0)

        manager.swapForDirection(TurnDirection.NEXT)
        manager.swapForDirection(TurnDirection.PREVIOUS)
        manager.swapForDirection(TurnDirection.NONE)
    }
}
