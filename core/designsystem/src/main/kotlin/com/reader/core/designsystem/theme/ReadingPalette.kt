package com.reader.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Built-in reading theme types.
 */
enum class ReadingThemeType {
    /** 日间白纸：纯净暖白背景、墨黑文字 */
    DEFAULT_LIGHT,

    /** 复古羊皮纸：经典护眼暖黄米色、深棕文字 */
    PARCHMENT,

    /** 护眼豆沙绿：柔和低饱和淡绿、深绿文字 */
    EYE_CARE_GREEN,

    /** 水墨屏极简：高对比纯黑白、无彩度、锐利清晰 */
    E_INK,

    /** OLED 纯黑夜间：极致省电纯黑 #000000 背景、柔和灰白文字 */
    OLED_BLACK;

    companion object {
        fun fromName(name: String, default: ReadingThemeType = DEFAULT_LIGHT): ReadingThemeType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default
        }
    }
}

/**
 * Complete specification for a reading palette, containing both Compose UI colors
 * and ARGB integer representations for Native Canvas text/background rendering.
 */
@Immutable
data class ReadingPalette(
    val type: ReadingThemeType,
    val displayName: String,
    val isDark: Boolean,
    val isEInk: Boolean = false,
    // Core canvas colors (used by Canvas rendering engine & Reader UI)
    val canvasBackground: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val surfaceColor: Color,
    val onSurfaceColor: Color,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val headerFooterColor: Color,
    val dividerColor: Color,
    val highlightColor: Color,
    // Material 3 ColorScheme mapping
    val colorScheme: ColorScheme
) {
    // ARGB integers for high-performance Native Canvas Paint & drawing operations
    val canvasBackgroundArgb: Int get() = canvasBackground.toArgb()
    val textColorArgb: Int get() = textColor.toArgb()
    val secondaryTextColorArgb: Int get() = secondaryTextColor.toArgb()
    val headerFooterColorArgb: Int get() = headerFooterColor.toArgb()
    val dividerColorArgb: Int get() = dividerColor.toArgb()
    val highlightColorArgb: Int get() = highlightColor.toArgb()
}

/**
 * Registry of pre-configured reading themes.
 */
object ReadingPalettes {

    /**
     * 1. 日间白纸 (DEFAULT_LIGHT)
     * 纯净暖白背景、墨黑文字，提供清晰明朗的纸张阅读体验。
     */
    val DefaultLight: ReadingPalette = ReadingPalette(
        type = ReadingThemeType.DEFAULT_LIGHT,
        displayName = "日间白纸",
        isDark = false,
        isEInk = false,
        canvasBackground = ReaderColors.DefaultLightCanvas,
        textColor = ReaderColors.DefaultLightText,
        secondaryTextColor = ReaderColors.DefaultLightSecondary,
        surfaceColor = ReaderColors.DefaultLightSurface,
        onSurfaceColor = ReaderColors.DefaultLightText,
        primaryAccent = ReaderColors.DefaultLightPrimary,
        onPrimaryAccent = Color.White,
        headerFooterColor = ReaderColors.DefaultLightSecondary,
        dividerColor = ReaderColors.DefaultLightDivider,
        highlightColor = ReaderColors.DefaultLightHighlight,
        colorScheme = lightColorScheme(
            primary = ReaderColors.DefaultLightPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCBE6FF),
            onPrimaryContainer = Color(0xFF001E30),
            secondary = Color(0xFF4F606E),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD2E5F5),
            onSecondaryContainer = Color(0xFF0B1D29),
            background = ReaderColors.DefaultLightCanvas,
            onBackground = ReaderColors.DefaultLightText,
            surface = ReaderColors.DefaultLightSurface,
            onSurface = ReaderColors.DefaultLightText,
            surfaceVariant = Color(0xFFE2E4E8),
            onSurfaceVariant = Color(0xFF43474E),
            outline = ReaderColors.DefaultLightDivider,
            outlineVariant = Color(0xFFC2C7CF)
        )
    )

    /**
     * 2. 复古羊皮纸 (PARCHMENT)
     * 经典暖黄米色背景、深棕文字，色调温润不刺眼，适合长时间持续阅读。
     */
    val Parchment: ReadingPalette = ReadingPalette(
        type = ReadingThemeType.PARCHMENT,
        displayName = "复古羊皮纸",
        isDark = false,
        isEInk = false,
        canvasBackground = ReaderColors.ParchmentCanvas,
        textColor = ReaderColors.ParchmentText,
        secondaryTextColor = ReaderColors.ParchmentSecondary,
        surfaceColor = ReaderColors.ParchmentSurface,
        onSurfaceColor = ReaderColors.ParchmentText,
        primaryAccent = ReaderColors.ParchmentPrimary,
        onPrimaryAccent = Color.White,
        headerFooterColor = ReaderColors.ParchmentSecondary,
        dividerColor = ReaderColors.ParchmentDivider,
        highlightColor = ReaderColors.ParchmentHighlight,
        colorScheme = lightColorScheme(
            primary = ReaderColors.ParchmentPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDDB8),
            onPrimaryContainer = Color(0xFF2E1500),
            secondary = Color(0xFF705B40),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFCE0BE),
            onSecondaryContainer = Color(0xFF281805),
            background = ReaderColors.ParchmentCanvas,
            onBackground = ReaderColors.ParchmentText,
            surface = ReaderColors.ParchmentSurface,
            onSurface = ReaderColors.ParchmentText,
            surfaceVariant = Color(0xFFE2D6BE),
            onSurfaceVariant = Color(0xFF504532),
            outline = ReaderColors.ParchmentDivider,
            outlineVariant = Color(0xFFC8BCA6)
        )
    )

    /**
     * 3. 护眼豆沙绿 (EYE_CARE_GREEN)
     * 柔和低饱和淡绿、深绿文字，有效降低视觉疲劳与屏幕反光感。
     */
    val EyeCareGreen: ReadingPalette = ReadingPalette(
        type = ReadingThemeType.EYE_CARE_GREEN,
        displayName = "护眼豆沙绿",
        isDark = false,
        isEInk = false,
        canvasBackground = ReaderColors.EyeCareGreenCanvas,
        textColor = ReaderColors.EyeCareGreenText,
        secondaryTextColor = ReaderColors.EyeCareGreenSecondary,
        surfaceColor = ReaderColors.EyeCareGreenSurface,
        onSurfaceColor = ReaderColors.EyeCareGreenText,
        primaryAccent = ReaderColors.EyeCareGreenPrimary,
        onPrimaryAccent = Color.White,
        headerFooterColor = ReaderColors.EyeCareGreenSecondary,
        dividerColor = ReaderColors.EyeCareGreenDivider,
        highlightColor = ReaderColors.EyeCareGreenHighlight,
        colorScheme = lightColorScheme(
            primary = ReaderColors.EyeCareGreenPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFAEF3C5),
            onPrimaryContainer = Color(0xFF00210E),
            secondary = Color(0xFF4F6352),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD1E8D3),
            onSecondaryContainer = Color(0xFF0C1F12),
            background = ReaderColors.EyeCareGreenCanvas,
            onBackground = ReaderColors.EyeCareGreenText,
            surface = ReaderColors.EyeCareGreenSurface,
            onSurface = ReaderColors.EyeCareGreenText,
            surfaceVariant = Color(0xFFBDD3BC),
            onSurfaceVariant = Color(0xFF3F493E),
            outline = ReaderColors.EyeCareGreenDivider,
            outlineVariant = Color(0xFFA6BAA5)
        )
    )

    /**
     * 4. 水墨屏极简 (E_INK)
     * 高对比黑白、锐利清晰、无彩度。专为电子墨水屏设计，禁用多余动画与色彩过渡。
     */
    val EInk: ReadingPalette = ReadingPalette(
        type = ReadingThemeType.E_INK,
        displayName = "水墨屏极简",
        isDark = false,
        isEInk = true,
        canvasBackground = ReaderColors.EInkCanvas,
        textColor = ReaderColors.EInkText,
        secondaryTextColor = ReaderColors.EInkSecondary,
        surfaceColor = ReaderColors.EInkSurface,
        onSurfaceColor = ReaderColors.EInkText,
        primaryAccent = ReaderColors.EInkPrimary,
        onPrimaryAccent = Color.White,
        headerFooterColor = ReaderColors.EInkSecondary,
        dividerColor = ReaderColors.EInkDivider,
        highlightColor = ReaderColors.EInkHighlight,
        colorScheme = lightColorScheme(
            primary = ReaderColors.EInkPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDDDDDD),
            onPrimaryContainer = Color.Black,
            secondary = Color(0xFF333333),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFEEEEEE),
            onSecondaryContainer = Color.Black,
            background = ReaderColors.EInkCanvas,
            onBackground = ReaderColors.EInkText,
            surface = ReaderColors.EInkSurface,
            onSurface = ReaderColors.EInkText,
            surfaceVariant = Color(0xFFE0E0E0),
            onSurfaceVariant = Color(0xFF222222),
            outline = ReaderColors.EInkDivider,
            outlineVariant = Color(0xFFAAAAAA)
        )
    )

    /**
     * 5. OLED 纯黑夜间 (OLED_BLACK)
     * 极致省电纯黑 #000000 背景、柔和灰白文字，防止夜间暗室刺眼并发挥 OLED 零功耗优势。
     */
    val OledBlack: ReadingPalette = ReadingPalette(
        type = ReadingThemeType.OLED_BLACK,
        displayName = "OLED纯黑夜间",
        isDark = true,
        isEInk = false,
        canvasBackground = ReaderColors.OledBlackCanvas,
        textColor = ReaderColors.OledBlackText,
        secondaryTextColor = ReaderColors.OledBlackSecondary,
        surfaceColor = ReaderColors.OledBlackSurface,
        onSurfaceColor = ReaderColors.OledBlackText,
        primaryAccent = ReaderColors.OledBlackPrimary,
        onPrimaryAccent = Color.Black,
        headerFooterColor = ReaderColors.OledBlackSecondary,
        dividerColor = ReaderColors.OledBlackDivider,
        highlightColor = ReaderColors.OledBlackHighlight,
        colorScheme = darkColorScheme(
            primary = ReaderColors.OledBlackPrimary,
            onPrimary = Color(0xFF00223D),
            primaryContainer = Color(0xFF1B3D5E),
            onPrimaryContainer = Color(0xFFCBE6FF),
            secondary = Color(0xFF90A4AE),
            onSecondary = Color(0xFF101C22),
            secondaryContainer = Color(0xFF263238),
            onSecondaryContainer = Color(0xFFCFD8DC),
            background = ReaderColors.OledBlackCanvas,
            onBackground = ReaderColors.OledBlackText,
            surface = ReaderColors.OledBlackSurface,
            onSurface = ReaderColors.OledBlackText,
            surfaceVariant = Color(0xFF1E1E1E),
            onSurfaceVariant = ReaderColors.OledBlackText,
            outline = ReaderColors.OledBlackDivider,
            outlineVariant = Color(0xFF333333)
        )
    )

    /** All 5 predefined palettes in standard order */
    val all: List<ReadingPalette> = listOf(
        DefaultLight,
        Parchment,
        EyeCareGreen,
        EInk,
        OledBlack
    )

    /**
     * Get palette by type, returning [DefaultLight] if not found.
     */
    fun fromType(type: ReadingThemeType): ReadingPalette {
        return when (type) {
            ReadingThemeType.DEFAULT_LIGHT -> DefaultLight
            ReadingThemeType.PARCHMENT -> Parchment
            ReadingThemeType.EYE_CARE_GREEN -> EyeCareGreen
            ReadingThemeType.E_INK -> EInk
            ReadingThemeType.OLED_BLACK -> OledBlack
        }
    }
}
