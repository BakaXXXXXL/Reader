package com.reader.core.model

/**
 * 全局与当前书籍阅读排版配置模型。
 *
 * @property fontSizeSp 正文字号 (sp)
 * @property lineHeightMultiplier 行高倍数 (例如 1.6f 表示 160% 行距)
 * @property paragraphSpacingDp 段落后额外间距 (dp)
 * @property letterSpacingEm 字间距 (em，相对字宽比例)
 * @property firstLineIndentSpaces 中文段落首行缩进字数 (默认 2)
 * @property pagePaddingHorizontalDp 页面左右边距 (dp)
 * @property pagePaddingVerticalDp 页面上下边距 (dp)
 * @property customFontPath 用户自定义字体绝对路径 (null 表示跟随系统字体)
 * @property pageTurnAnimation 翻页动效，默认 [PageTurnAnimation.COVER]
 * @property themePreset 阅读背景与文字配色主题，默认 [ReaderThemePreset.DEFAULT_LIGHT]
 * @property keepScreenOn 阅读时保持屏幕常亮
 * @property volumeKeyPageTurn 是否允许音量键翻页
 */
data class ReaderConfig(
    val fontSizeSp: Float = DEFAULT_FONT_SIZE_SP,
    val lineHeightMultiplier: Float = DEFAULT_LINE_HEIGHT_MULTIPLIER,
    val paragraphSpacingDp: Float = DEFAULT_PARAGRAPH_SPACING_DP,
    val letterSpacingEm: Float = DEFAULT_LETTER_SPACING_EM,
    val firstLineIndentSpaces: Int = DEFAULT_FIRST_LINE_INDENT,
    val pagePaddingHorizontalDp: Float = DEFAULT_PADDING_HORIZONTAL_DP,
    val pagePaddingVerticalDp: Float = DEFAULT_PADDING_VERTICAL_DP,
    val customFontPath: String? = null,
    val pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.COVER,
    val themePreset: ReaderThemePreset = ReaderThemePreset.DEFAULT_LIGHT,
    val keepScreenOn: Boolean = true,
    val volumeKeyPageTurn: Boolean = true
) {
    init {
        require(fontSizeSp in MIN_FONT_SIZE_SP..MAX_FONT_SIZE_SP) {
            "fontSizeSp must be in $MIN_FONT_SIZE_SP..$MAX_FONT_SIZE_SP: $fontSizeSp"
        }
        require(lineHeightMultiplier in MIN_LINE_HEIGHT..MAX_LINE_HEIGHT) {
            "lineHeightMultiplier must be in $MIN_LINE_HEIGHT..$MAX_LINE_HEIGHT: $lineHeightMultiplier"
        }
        require(paragraphSpacingDp in 0f..MAX_PARAGRAPH_SPACING_DP) {
            "paragraphSpacingDp must be in 0..$MAX_PARAGRAPH_SPACING_DP: $paragraphSpacingDp"
        }
        require(firstLineIndentSpaces >= 0) {
            "firstLineIndentSpaces must be non-negative: $firstLineIndentSpaces"
        }
        require(pagePaddingHorizontalDp >= 0f) {
            "pagePaddingHorizontalDp must be non-negative: $pagePaddingHorizontalDp"
        }
        require(pagePaddingVerticalDp >= 0f) {
            "pagePaddingVerticalDp must be non-negative: $pagePaddingVerticalDp"
        }
    }

    companion object {
        const val MIN_FONT_SIZE_SP = 12f
        const val MAX_FONT_SIZE_SP = 36f
        const val DEFAULT_FONT_SIZE_SP = 18f

        const val MIN_LINE_HEIGHT = 1.0f
        const val MAX_LINE_HEIGHT = 3.0f
        const val DEFAULT_LINE_HEIGHT_MULTIPLIER = 1.6f

        const val DEFAULT_PARAGRAPH_SPACING_DP = 12f
        const val MAX_PARAGRAPH_SPACING_DP = 48f

        const val DEFAULT_LETTER_SPACING_EM = 0.05f

        const val DEFAULT_FIRST_LINE_INDENT = 2

        const val DEFAULT_PADDING_HORIZONTAL_DP = 16f
        const val DEFAULT_PADDING_VERTICAL_DP = 24f

        val DEFAULT = ReaderConfig()
    }
}
