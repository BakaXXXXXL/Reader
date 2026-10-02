package com.reader.engine.render.controller

import com.reader.engine.render.model.TurnDirection

/**
 * 翻页动效与手势事件回调监听器。
 */
interface OnPageTurnListener {

    /**
     * 手势或程序触发翻页开始。
     */
    fun onPageTurnStarted(direction: TurnDirection) {}

    /**
     * 翻页进度更新 (0.0f .. 1.0f)。
     */
    fun onPageTurnProgress(direction: TurnDirection, progress: Float) {}

    /**
     * 翻页动画成功完成，并已完成页面切换。
     */
    fun onPageTurnCompleted(direction: TurnDirection) {}

    /**
     * 翻页动作被取消或弹性回弹至原位。
     */
    fun onPageTurnCanceled() {}

    /**
     * 读者轻触屏幕中央阅读控制唤醒热区。
     */
    fun onCenterClicked() {}

    /**
     * 请求宿主 View 重绘当前帧。
     */
    fun onInvalidateRequest() {}
}
