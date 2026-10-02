package com.reader.engine.render.model

/**
 * 设备电量与充电状态模型。
 *
 * @property level 电池剩余百分比 (0..100)
 * @property isCharging 是否正在充电
 */
data class BatteryStatus(
    val level: Int = 100,
    val isCharging: Boolean = false
) {
    init {
        require(level in 0..100) { "Battery level must be in 0..100: $level" }
    }

    /**
     * 是否为低电量状态（<= 15% 且未充电）。
     */
    val isLowBattery: Boolean
        get() = level <= 15 && !isCharging

    companion object {
        val FULL = BatteryStatus(level = 100, isCharging = false)
        val CHARGING_DEFAULT = BatteryStatus(level = 80, isCharging = true)
    }
}
