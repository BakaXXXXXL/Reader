package com.reader.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReaderConfig
import com.reader.core.model.ReaderThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// 顶层委托，避免多实例竞争
val Context.readerPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_preferences")

/**
 * 基于 Jetpack DataStore (Preferences) 的排版与阅读交互偏好持久化管理器。
 * 遵循非阻塞异步协程与 Flow 单向数据流，严禁使用过时的 SharedPreferences。
 */
class ReaderPreferencesDataStore(
    private val dataStore: DataStore<Preferences>
) {

    /**
     * 辅助构造函数，便捷通过 Context 构建。
     */
    constructor(context: Context) : this(context.readerPreferencesDataStore)

    /**
     * 全局阅读排版配置 [Flow]，初始或缺省状态下严格遵守默认出厂配置
     * (默认覆盖翻页 [PageTurnAnimation.COVER], 日间纸白 [ReaderThemePreset.DEFAULT_LIGHT], 18sp 字号)。
     */
    val readerConfigFlow: Flow<ReaderConfig> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val fontSize = preferences[KEY_FONT_SIZE_SP] ?: ReaderConfig.DEFAULT_FONT_SIZE_SP
            val lineHeight = preferences[KEY_LINE_HEIGHT_MULTIPLIER] ?: ReaderConfig.DEFAULT_LINE_HEIGHT_MULTIPLIER
            val paragraphSpacing = preferences[KEY_PARAGRAPH_SPACING_DP] ?: ReaderConfig.DEFAULT_PARAGRAPH_SPACING_DP
            val letterSpacing = preferences[KEY_LETTER_SPACING_EM] ?: ReaderConfig.DEFAULT_LETTER_SPACING_EM
            val firstLineIndent = preferences[KEY_FIRST_LINE_INDENT] ?: ReaderConfig.DEFAULT_FIRST_LINE_INDENT
            val paddingHorizontal = preferences[KEY_PADDING_HORIZONTAL_DP] ?: ReaderConfig.DEFAULT_PADDING_HORIZONTAL_DP
            val paddingVertical = preferences[KEY_PADDING_VERTICAL_DP] ?: ReaderConfig.DEFAULT_PADDING_VERTICAL_DP
            val customFontPath = preferences[KEY_CUSTOM_FONT_PATH]

            val pageTurnAnimId = preferences[KEY_PAGE_TURN_ANIMATION]
            val pageTurnAnim = PageTurnAnimation.fromId(pageTurnAnimId)

            val themeId = preferences[KEY_THEME_PRESET]
            val themePreset = ReaderThemePreset.fromId(themeId)

            val keepScreenOn = preferences[KEY_KEEP_SCREEN_ON] ?: true
            val volumeKeyPageTurn = preferences[KEY_VOLUME_KEY_PAGE_TURN] ?: true

            ReaderConfig(
                fontSizeSp = fontSize.coerceIn(ReaderConfig.MIN_FONT_SIZE_SP, ReaderConfig.MAX_FONT_SIZE_SP),
                lineHeightMultiplier = lineHeight.coerceIn(ReaderConfig.MIN_LINE_HEIGHT, ReaderConfig.MAX_LINE_HEIGHT),
                paragraphSpacingDp = paragraphSpacing.coerceIn(0f, ReaderConfig.MAX_PARAGRAPH_SPACING_DP),
                letterSpacingEm = letterSpacing,
                firstLineIndentSpaces = firstLineIndent.coerceAtLeast(0),
                pagePaddingHorizontalDp = paddingHorizontal.coerceAtLeast(0f),
                pagePaddingVerticalDp = paddingVertical.coerceAtLeast(0f),
                customFontPath = customFontPath,
                pageTurnAnimation = pageTurnAnim,
                themePreset = themePreset,
                keepScreenOn = keepScreenOn,
                volumeKeyPageTurn = volumeKeyPageTurn
            )
        }

    /**
     * 原子批量保存完整阅读配置。
     */
    suspend fun updateConfig(config: ReaderConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_FONT_SIZE_SP] = config.fontSizeSp
            prefs[KEY_LINE_HEIGHT_MULTIPLIER] = config.lineHeightMultiplier
            prefs[KEY_PARAGRAPH_SPACING_DP] = config.paragraphSpacingDp
            prefs[KEY_LETTER_SPACING_EM] = config.letterSpacingEm
            prefs[KEY_FIRST_LINE_INDENT] = config.firstLineIndentSpaces
            prefs[KEY_PADDING_HORIZONTAL_DP] = config.pagePaddingHorizontalDp
            prefs[KEY_PADDING_VERTICAL_DP] = config.pagePaddingVerticalDp
            val customFont = config.customFontPath
            if (customFont != null) {
                prefs[KEY_CUSTOM_FONT_PATH] = customFont
            } else {
                prefs.remove(KEY_CUSTOM_FONT_PATH)
            }
            prefs[KEY_PAGE_TURN_ANIMATION] = config.pageTurnAnimation.id
            prefs[KEY_THEME_PRESET] = config.themePreset.id
            prefs[KEY_KEEP_SCREEN_ON] = config.keepScreenOn
            prefs[KEY_VOLUME_KEY_PAGE_TURN] = config.volumeKeyPageTurn
        }
    }

    suspend fun updateFontSize(fontSizeSp: Float) {
        val safe = fontSizeSp.coerceIn(ReaderConfig.MIN_FONT_SIZE_SP, ReaderConfig.MAX_FONT_SIZE_SP)
        dataStore.edit { it[KEY_FONT_SIZE_SP] = safe }
    }

    suspend fun updateLineHeight(lineHeightMultiplier: Float) {
        val safe = lineHeightMultiplier.coerceIn(ReaderConfig.MIN_LINE_HEIGHT, ReaderConfig.MAX_LINE_HEIGHT)
        dataStore.edit { it[KEY_LINE_HEIGHT_MULTIPLIER] = safe }
    }

    suspend fun updateParagraphSpacing(paragraphSpacingDp: Float) {
        val safe = paragraphSpacingDp.coerceIn(0f, ReaderConfig.MAX_PARAGRAPH_SPACING_DP)
        dataStore.edit { it[KEY_PARAGRAPH_SPACING_DP] = safe }
    }

    suspend fun updateLetterSpacing(letterSpacingEm: Float) {
        dataStore.edit { it[KEY_LETTER_SPACING_EM] = letterSpacingEm }
    }

    suspend fun updateFirstLineIndent(spaces: Int) {
        val safe = spaces.coerceAtLeast(0)
        dataStore.edit { it[KEY_FIRST_LINE_INDENT] = safe }
    }

    suspend fun updatePadding(horizontalDp: Float, verticalDp: Float) {
        dataStore.edit {
            it[KEY_PADDING_HORIZONTAL_DP] = horizontalDp.coerceAtLeast(0f)
            it[KEY_PADDING_VERTICAL_DP] = verticalDp.coerceAtLeast(0f)
        }
    }

    suspend fun updateCustomFontPath(path: String?) {
        dataStore.edit {
            if (path.isNullOrBlank()) {
                it.remove(KEY_CUSTOM_FONT_PATH)
            } else {
                it[KEY_CUSTOM_FONT_PATH] = path
            }
        }
    }

    suspend fun updatePageTurnAnimation(animation: PageTurnAnimation) {
        dataStore.edit { it[KEY_PAGE_TURN_ANIMATION] = animation.id }
    }

    suspend fun updateThemePreset(preset: ReaderThemePreset) {
        dataStore.edit { it[KEY_THEME_PRESET] = preset.id }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun updateVolumeKeyPageTurn(enabled: Boolean) {
        dataStore.edit { it[KEY_VOLUME_KEY_PAGE_TURN] = enabled }
    }

    /**
     * 重置所有排版与阅读设置回默认值。
     */
    suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }

    companion object {
        val KEY_FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val KEY_LINE_HEIGHT_MULTIPLIER = floatPreferencesKey("line_height_multiplier")
        val KEY_PARAGRAPH_SPACING_DP = floatPreferencesKey("paragraph_spacing_dp")
        val KEY_LETTER_SPACING_EM = floatPreferencesKey("letter_spacing_em")
        val KEY_FIRST_LINE_INDENT = intPreferencesKey("first_line_indent")
        val KEY_PADDING_HORIZONTAL_DP = floatPreferencesKey("padding_horizontal_dp")
        val KEY_PADDING_VERTICAL_DP = floatPreferencesKey("padding_vertical_dp")
        val KEY_CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val KEY_PAGE_TURN_ANIMATION = stringPreferencesKey("page_turn_animation")
        val KEY_THEME_PRESET = stringPreferencesKey("theme_preset")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_VOLUME_KEY_PAGE_TURN = booleanPreferencesKey("volume_key_page_turn")
    }
}
