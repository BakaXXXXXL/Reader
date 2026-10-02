package com.reader.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReaderConfig
import com.reader.core.model.ReaderThemePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderPreferencesDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var readerPreferencesDataStore: ReaderPreferencesDataStore

    @Before
    fun setup() {
        dataStoreScope = CoroutineScope(testDispatcher + Job())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tempFolder.newFile("test_prefs_${System.nanoTime()}.preferences_pb") }
        )
        readerPreferencesDataStore = ReaderPreferencesDataStore(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun testDefaultConfigOutOfTheBox() = runTest(testDispatcher) {
        val initialConfig = readerPreferencesDataStore.readerConfigFlow.first()

        assertEquals(PageTurnAnimation.COVER, initialConfig.pageTurnAnimation)
        assertEquals(ReaderThemePreset.DEFAULT_LIGHT, initialConfig.themePreset)
        assertEquals(ReaderConfig.DEFAULT_FONT_SIZE_SP, initialConfig.fontSizeSp, 0.01f)
        assertEquals(ReaderConfig.DEFAULT_LINE_HEIGHT_MULTIPLIER, initialConfig.lineHeightMultiplier, 0.01f)
        assertEquals(2, initialConfig.firstLineIndentSpaces)
        assertTrue(initialConfig.keepScreenOn)
        assertTrue(initialConfig.volumeKeyPageTurn)
        assertNull(initialConfig.customFontPath)
    }

    @Test
    fun testUpdateIndividualPreferences() = runTest(testDispatcher) {
        readerPreferencesDataStore.updateFontSize(22f)
        assertEquals(22f, readerPreferencesDataStore.readerConfigFlow.first().fontSizeSp, 0.01f)

        readerPreferencesDataStore.updatePageTurnAnimation(PageTurnAnimation.SIMULATION)
        assertEquals(PageTurnAnimation.SIMULATION, readerPreferencesDataStore.readerConfigFlow.first().pageTurnAnimation)

        readerPreferencesDataStore.updateThemePreset(ReaderThemePreset.PARCHMENT)
        assertEquals(ReaderThemePreset.PARCHMENT, readerPreferencesDataStore.readerConfigFlow.first().themePreset)

        readerPreferencesDataStore.updateLineHeight(1.8f)
        assertEquals(1.8f, readerPreferencesDataStore.readerConfigFlow.first().lineHeightMultiplier, 0.01f)

        readerPreferencesDataStore.updateFirstLineIndent(4)
        assertEquals(4, readerPreferencesDataStore.readerConfigFlow.first().firstLineIndentSpaces)

        readerPreferencesDataStore.updatePadding(20f, 30f)
        assertEquals(20f, readerPreferencesDataStore.readerConfigFlow.first().pagePaddingHorizontalDp, 0.01f)
        assertEquals(30f, readerPreferencesDataStore.readerConfigFlow.first().pagePaddingVerticalDp, 0.01f)

        readerPreferencesDataStore.updateCustomFontPath("/fonts/my_font.ttf")
        assertEquals("/fonts/my_font.ttf", readerPreferencesDataStore.readerConfigFlow.first().customFontPath)

        readerPreferencesDataStore.updateKeepScreenOn(false)
        assertFalse(readerPreferencesDataStore.readerConfigFlow.first().keepScreenOn)

        readerPreferencesDataStore.updateVolumeKeyPageTurn(false)
        assertFalse(readerPreferencesDataStore.readerConfigFlow.first().volumeKeyPageTurn)
    }

    @Test
    fun testUpdateEntireConfigAndReset() = runTest(testDispatcher) {
        val customConfig = ReaderConfig(
            fontSizeSp = 24f,
            lineHeightMultiplier = 2.0f,
            paragraphSpacingDp = 16f,
            letterSpacingEm = 0.1f,
            firstLineIndentSpaces = 2,
            pagePaddingHorizontalDp = 18f,
            pagePaddingVerticalDp = 28f,
            customFontPath = "/fonts/book.otf",
            pageTurnAnimation = PageTurnAnimation.CONTINUOUS_SCROLL,
            themePreset = ReaderThemePreset.DARK_NIGHT,
            keepScreenOn = false,
            volumeKeyPageTurn = false
        )

        readerPreferencesDataStore.updateConfig(customConfig)
        val loaded = readerPreferencesDataStore.readerConfigFlow.first()
        assertEquals(customConfig, loaded)

        // Reset
        readerPreferencesDataStore.resetToDefaults()
        val resetConfig = readerPreferencesDataStore.readerConfigFlow.first()
        assertEquals(PageTurnAnimation.COVER, resetConfig.pageTurnAnimation)
        assertEquals(ReaderConfig.DEFAULT_FONT_SIZE_SP, resetConfig.fontSizeSp, 0.01f)
        assertNull(resetConfig.customFontPath)
    }
}
