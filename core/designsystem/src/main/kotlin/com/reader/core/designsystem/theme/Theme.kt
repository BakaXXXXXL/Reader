package com.reader.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Standard Fallback Light Color Scheme (used when Dynamic Color is off or not in Reader mode).
 */
val ReaderLightColorScheme: ColorScheme = lightColorScheme(
    primary = ReaderColors.PrimaryLight,
    onPrimary = ReaderColors.OnPrimaryLight,
    primaryContainer = ReaderColors.PrimaryContainerLight,
    onPrimaryContainer = ReaderColors.OnPrimaryContainerLight,
    secondary = ReaderColors.SecondaryLight,
    onSecondary = ReaderColors.OnSecondaryLight,
    secondaryContainer = ReaderColors.SecondaryContainerLight,
    onSecondaryContainer = ReaderColors.OnSecondaryContainerLight,
    tertiary = ReaderColors.TertiaryLight,
    onTertiary = ReaderColors.OnTertiaryLight,
    tertiaryContainer = ReaderColors.TertiaryContainerLight,
    onTertiaryContainer = ReaderColors.OnTertiaryContainerLight,
    error = ReaderColors.ErrorLight,
    onError = ReaderColors.OnErrorLight,
    errorContainer = ReaderColors.ErrorContainerLight,
    onErrorContainer = ReaderColors.OnErrorContainerLight,
    background = ReaderColors.BackgroundLight,
    onBackground = ReaderColors.OnBackgroundLight,
    surface = ReaderColors.SurfaceLight,
    onSurface = ReaderColors.OnSurfaceLight,
    surfaceVariant = ReaderColors.SurfaceVariantLight,
    onSurfaceVariant = ReaderColors.OnSurfaceVariantLight,
    outline = ReaderColors.OutlineLight,
    outlineVariant = ReaderColors.OutlineVariantLight
)

/**
 * Standard Fallback Dark Color Scheme (used when Dynamic Color is off or not in Reader mode).
 */
val ReaderDarkColorScheme: ColorScheme = darkColorScheme(
    primary = ReaderColors.PrimaryDark,
    onPrimary = ReaderColors.OnPrimaryDark,
    primaryContainer = ReaderColors.PrimaryContainerDark,
    onPrimaryContainer = ReaderColors.OnPrimaryContainerDark,
    secondary = ReaderColors.SecondaryDark,
    onSecondary = ReaderColors.OnSecondaryDark,
    secondaryContainer = ReaderColors.SecondaryContainerDark,
    onSecondaryContainer = ReaderColors.OnSecondaryContainerDark,
    tertiary = ReaderColors.TertiaryDark,
    onTertiary = ReaderColors.OnTertiaryDark,
    tertiaryContainer = ReaderColors.TertiaryContainerDark,
    onTertiaryContainer = ReaderColors.OnTertiaryContainerDark,
    error = ReaderColors.ErrorDark,
    onError = ReaderColors.OnErrorDark,
    errorContainer = ReaderColors.ErrorContainerDark,
    onErrorContainer = ReaderColors.OnErrorContainerDark,
    background = ReaderColors.BackgroundDark,
    onBackground = ReaderColors.OnBackgroundDark,
    surface = ReaderColors.SurfaceDark,
    onSurface = ReaderColors.OnSurfaceDark,
    surfaceVariant = ReaderColors.SurfaceVariantDark,
    onSurfaceVariant = ReaderColors.OnSurfaceVariantDark,
    outline = ReaderColors.OutlineDark,
    outlineVariant = ReaderColors.OutlineVariantDark
)

/**
 * Configuration metadata for the current theme.
 */
@Immutable
data class ReaderThemeConfig(
    val darkTheme: Boolean,
    val dynamicColor: Boolean,
    val readingTheme: ReadingThemeType?
)

/**
 * CompositionLocal providing current active [ReadingPalette].
 */
val LocalReadingPalette = staticCompositionLocalOf<ReadingPalette> {
    ReadingPalettes.DefaultLight
}

/**
 * CompositionLocal providing current [ReaderThemeConfig].
 */
val LocalReaderThemeConfig = staticCompositionLocalOf<ReaderThemeConfig> {
    ReaderThemeConfig(
        darkTheme = false,
        dynamicColor = true,
        readingTheme = null
    )
}

/**
 * Main application theme wrapper.
 *
 * Supports:
 * 1. Material 3 Dynamic Color (Android 12+ / API 31+).
 * 2. Dedicated Reading Themes (5 presets: DEFAULT_LIGHT, PARCHMENT, EYE_CARE_GREEN, E_INK, OLED_BLACK).
 * 3. Edge-to-Edge System Bar appearance synchronization.
 *
 * @param darkTheme Whether dark mode is enabled.
 * @param dynamicColor Whether Android 12+ dynamic color extraction is enabled (ignored if readingTheme is specified).
 * @param readingTheme Optional reading preset. When provided, overrides the color scheme to the chosen reading theme.
 * @param content Composable children content.
 */
@Composable
fun ReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    readingTheme: ReadingThemeType? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Determine active ReadingPalette
    val activePalette: ReadingPalette = if (readingTheme != null) {
        ReadingPalettes.fromType(readingTheme)
    } else {
        if (darkTheme) ReadingPalettes.OledBlack else ReadingPalettes.DefaultLight
    }

    // Determine target M3 ColorScheme
    val colorScheme: ColorScheme = when {
        readingTheme != null -> {
            activePalette.colorScheme
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            ReaderDarkColorScheme
        }
        else -> {
            ReaderLightColorScheme
        }
    }

    // Edge-to-Edge System Bar Sync
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // InsetsController uses light status bars = dark icons
                val isLightBackground = colorScheme.background.luminance() > 0.5f
                insetsController.isAppearanceLightStatusBars = isLightBackground
                insetsController.isAppearanceLightNavigationBars = isLightBackground
            }
        }
    }

    val themeConfig = ReaderThemeConfig(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        readingTheme = readingTheme
    )

    CompositionLocalProvider(
        LocalReadingPalette provides activePalette,
        LocalReaderThemeConfig provides themeConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ReaderTypography,
            shapes = ReaderShapes,
            content = content
        )
    }
}

/**
 * Convenient object to access theme extras.
 */
object ReaderTheme {
    /** Currently active reading palette */
    val readingPalette: ReadingPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalReadingPalette.current

    /** Currently active theme config */
    val config: ReaderThemeConfig
        @Composable
        @ReadOnlyComposable
        get() = LocalReaderThemeConfig.current
}
