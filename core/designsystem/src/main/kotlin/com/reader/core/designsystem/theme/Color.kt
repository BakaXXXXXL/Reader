package com.reader.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Reader App Color Palette definitions.
 * Contains core Material 3 brand colors, system tones, and reading canvas colors.
 */
object ReaderColors {
    // Primary Brand Tones (Modern Indigo / Ocean Blue)
    val PrimaryLight = Color(0xFF1E6091)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val PrimaryContainerLight = Color(0xFFCBE6FF)
    val OnPrimaryContainerLight = Color(0xFF001E30)

    val PrimaryDark = Color(0xFF90CCFF)
    val OnPrimaryDark = Color(0xFF003353)
    val PrimaryContainerDark = Color(0xFF004B76)
    val OnPrimaryContainerDark = Color(0xFFCBE6FF)

    // Secondary Tones (Slate Teal)
    val SecondaryLight = Color(0xFF4F606E)
    val OnSecondaryLight = Color(0xFFFFFFFF)
    val SecondaryContainerLight = Color(0xFFD2E5F5)
    val OnSecondaryContainerLight = Color(0xFF0B1D29)

    val SecondaryDark = Color(0xFFB7C9D9)
    val OnSecondaryDark = Color(0xFF21323F)
    val SecondaryContainerDark = Color(0xFF384956)
    val OnSecondaryContainerDark = Color(0xFFD2E5F5)

    // Tertiary Tones (Soft Amber)
    val TertiaryLight = Color(0xFF65587B)
    val OnTertiaryLight = Color(0xFFFFFFFF)
    val TertiaryContainerLight = Color(0xFFEBDCFF)
    val OnTertiaryContainerLight = Color(0xFF201634)

    val TertiaryDark = Color(0xFFCFBDFF)
    val OnTertiaryDark = Color(0xFF362B4A)
    val TertiaryContainerDark = Color(0xFF4D4162)
    val OnTertiaryContainerDark = Color(0xFFEBDCFF)

    // Neutral Surfaces & Backgrounds
    val BackgroundLight = Color(0xFFFCFCFF)
    val OnBackgroundLight = Color(0xFF1A1C1E)
    val SurfaceLight = Color(0xFFFCFCFF)
    val OnSurfaceLight = Color(0xFF1A1C1E)
    val SurfaceVariantLight = Color(0xFFDEE3EB)
    val OnSurfaceVariantLight = Color(0xFF42474E)
    val OutlineLight = Color(0xFF72777F)
    val OutlineVariantLight = Color(0xFFC2C7CF)

    val BackgroundDark = Color(0xFF111315)
    val OnBackgroundDark = Color(0xFFE2E2E5)
    val SurfaceDark = Color(0xFF111315)
    val OnSurfaceDark = Color(0xFFE2E2E5)
    val SurfaceVariantDark = Color(0xFF42474E)
    val OnSurfaceVariantDark = Color(0xFFC2C7CF)
    val OutlineDark = Color(0xFF8C9199)
    val OutlineVariantDark = Color(0xFF42474E)

    // Error Tones
    val ErrorLight = Color(0xFFBA1A1A)
    val OnErrorLight = Color(0xFFFFFFFF)
    val ErrorContainerLight = Color(0xFFFFDAD6)
    val OnErrorContainerLight = Color(0xFF410002)

    val ErrorDark = Color(0xFFFFB4AB)
    val OnErrorDark = Color(0xFF690005)
    val ErrorContainerDark = Color(0xFF93000A)
    val OnErrorContainerDark = Color(0xFFFFDAD6)

    // ==========================================
    // 5 Reading Preset Specific Colors
    // ==========================================

    // 1. DEFAULT_LIGHT (日间白纸)
    val DefaultLightCanvas = Color(0xFFFAF9F6)        // Pure warm white paper
    val DefaultLightText = Color(0xFF1F1F1F)          // Ink black
    val DefaultLightSecondary = Color(0xFF666666)     // Soft graphite
    val DefaultLightSurface = Color(0xFFFFFFFF)       // Clean white card
    val DefaultLightPrimary = Color(0xFF1E6091)       // Indigo accent
    val DefaultLightDivider = Color(0xFFE5E5E5)
    val DefaultLightHighlight = Color(0x66B0D5FF)

    // 2. PARCHMENT (复古羊皮纸)
    val ParchmentCanvas = Color(0xFFF4ECD8)           // Classic warm rice paper
    val ParchmentText = Color(0xFF382918)             // Deep antique brown ink
    val ParchmentSecondary = Color(0xFF786248)        // Medium sepia
    val ParchmentSurface = Color(0xFFEBE0C7)          // Warm parchment surface
    val ParchmentPrimary = Color(0xFF8A5A2B)          // Vintage amber brown
    val ParchmentDivider = Color(0xFFD6C8A8)
    val ParchmentHighlight = Color(0x66E2BC7A)

    // 3. EYE_CARE_GREEN (护眼豆沙绿)
    val EyeCareGreenCanvas = Color(0xFFCCE2CB)        // Soft muted green
    val EyeCareGreenText = Color(0xFF183820)          // Forest dark green ink
    val EyeCareGreenSecondary = Color(0xFF3F6147)     // Sage secondary
    val EyeCareGreenSurface = Color(0xFFBED8BD)       // Gentle green surface
    val EyeCareGreenPrimary = Color(0xFF26673E)       // Deep emerald
    val EyeCareGreenDivider = Color(0xFFAECDAE)
    val EyeCareGreenHighlight = Color(0x6686C594)

    // 4. E_INK (水墨屏极简 - 纯黑白锐利无彩度)
    val EInkCanvas = Color(0xFFFFFFFF)                // Pure reflective white
    val EInkText = Color(0xFF000000)                  // Crisp solid black
    val EInkSecondary = Color(0xFF4A4A4A)             // Mid charcoal
    val EInkSurface = Color(0xFFF0F0F0)               // Very light grey
    val EInkPrimary = Color(0xFF000000)               // Pure black
    val EInkDivider = Color(0xFF888888)               // Distinct line
    val EInkHighlight = Color(0x40000000)             // 25% black tint

    // 5. OLED_BLACK (OLED 纯黑极致省电)
    val OledBlackCanvas = Color(0xFF000000)           // True OLED black #000000
    val OledBlackText = Color(0xFFB0B0B0)             // Low-glare soft grey
    val OledBlackSecondary = Color(0xFF6E6E6E)        // Dim grey
    val OledBlackSurface = Color(0xFF121212)          // Deep shadow grey
    val OledBlackPrimary = Color(0xFF5390D9)          // Muted cyan-blue
    val OledBlackDivider = Color(0xFF242424)
    val OledBlackHighlight = Color(0x55305A88)
}
