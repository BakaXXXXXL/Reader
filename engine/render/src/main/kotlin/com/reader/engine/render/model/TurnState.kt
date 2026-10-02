package com.reader.engine.render.model

/**
 * 翻页动效状态机状态枚举。
 */
enum class TurnState {
    /** 空闲静止状态 */
    IDLE,

    /** 手势拖拽中 */
    DRAGGING,

    /** 弹性复位或翻页惯性/平滑动画执行中 */
    ANIMATING
}
