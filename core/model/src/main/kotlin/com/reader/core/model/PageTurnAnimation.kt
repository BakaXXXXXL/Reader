package com.reader.core.model

/**
 * 翻页动效模式枚举。
 * 默认交互为平滑横向覆盖 [COVER]，同时提供 3D 仿真、平滑滑动、垂直连续滚动及无动画。
 */
enum class PageTurnAnimation(val id: String, val displayName: String) {
    COVER("cover", "平滑覆盖"),
    SIMULATION("simulation", "3D 仿真"),
    SLIDE("slide", "平滑滑动"),
    CONTINUOUS_SCROLL("scroll", "垂直滚动"),
    NONE("none", "无动画");

    companion object {
        val DEFAULT = COVER

        fun fromId(id: String?): PageTurnAnimation {
            if (id.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
