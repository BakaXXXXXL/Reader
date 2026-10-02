package com.reader.core.model

/**
 * 阅读界面主题预设枚举。
 * 内置五套经典护眼与场景化配色：日间纸白、羊皮纸、豆沙绿、水墨屏与纯黑夜间。
 *
 * 颜色值以 32-bit ARGB 格式 (如 0xFFF8F6F1L) 跨平台存储，与 Android / Compose Color 保持完全兼容。
 *
 * @property id 唯一识别标识
 * @property displayName 用户可见显示名称
 * @property backgroundColor 页面背景色 ARGB
 * @property textColor 正文主文本颜色 ARGB
 * @property secondaryTextColor 页眉页脚及辅助信息颜色 ARGB
 * @property isDark 是否为暗色夜间模式
 */
enum class ReaderThemePreset(
    val id: String,
    val displayName: String,
    val backgroundColor: Long,
    val textColor: Long,
    val secondaryTextColor: Long,
    val isDark: Boolean
) {
    DEFAULT_LIGHT(
        id = "default_light",
        displayName = "日间纸白",
        backgroundColor = 0xFFF8F6F1L,
        textColor = 0xFF2B2824L,
        secondaryTextColor = 0xFF8A857AL,
        isDark = false
    ),
    PARCHMENT(
        id = "parchment",
        displayName = "复古羊皮",
        backgroundColor = 0xFFF0E5D0L,
        textColor = 0xFF3D2F1FL,
        secondaryTextColor = 0xFF8C7B67L,
        isDark = false
    ),
    GREEN_TEA(
        id = "green_tea",
        displayName = "护眼豆沙",
        backgroundColor = 0xFFD8E5D4L,
        textColor = 0xFF1C2D1FL,
        secondaryTextColor = 0xFF657867L,
        isDark = false
    ),
    E_INK(
        id = "e_ink",
        displayName = "黑白水墨",
        backgroundColor = 0xFFFFFFFFL,
        textColor = 0xFF000000L,
        secondaryTextColor = 0xFF666666L,
        isDark = false
    ),
    DARK_NIGHT(
        id = "dark_night",
        displayName = "纯黑夜间",
        backgroundColor = 0xFF121212L,
        textColor = 0xFFB0B0B0L,
        secondaryTextColor = 0xFF606060L,
        isDark = true
    );

    companion object {
        val DEFAULT = DEFAULT_LIGHT

        fun fromId(id: String?): ReaderThemePreset {
            if (id.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
