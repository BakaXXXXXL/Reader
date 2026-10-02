package com.reader.core.designsystem.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Controller for managing Android System Bars (Status Bar, Navigation Bar)
 * and immersive full-screen display state.
 */
@Stable
class SystemBarController(
    private val window: Window?,
    private val view: View?
) {
    private val insetsController: WindowInsetsControllerCompat? =
        if (window != null && view != null) {
            WindowCompat.getInsetsController(window, view).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else null

    var areSystemBarsVisible: Boolean by mutableStateOf(true)
        private set

    var isImmersive: Boolean by mutableStateOf(false)
        private set

    /**
     * Show both status bar and navigation bar.
     */
    fun showSystemBars() {
        insetsController?.show(WindowInsetsCompat.Type.systemBars())
        areSystemBarsVisible = true
        isImmersive = false
    }

    /**
     * Hide both status bar and navigation bar for full-screen immersive reading.
     */
    fun hideSystemBars() {
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        areSystemBarsVisible = false
        isImmersive = true
    }

    /**
     * Toggle system bars between visible and hidden.
     */
    fun toggleSystemBars() {
        if (areSystemBarsVisible) {
            hideSystemBars()
        } else {
            showSystemBars()
        }
    }

    /**
     * Enable or disable immersive full-screen reading mode.
     */
    fun setImmersiveMode(enabled: Boolean) {
        if (enabled) {
            hideSystemBars()
        } else {
            showSystemBars()
        }
    }

    /**
     * Configure light status bar icons (dark icons on light background).
     */
    fun setLightStatusBar(isLight: Boolean) {
        insetsController?.isAppearanceLightStatusBars = isLight
    }

    /**
     * Configure light navigation bar icons (dark icons on light background).
     */
    fun setLightNavigationBar(isLight: Boolean) {
        insetsController?.isAppearanceLightNavigationBars = isLight
    }

    /**
     * Keep screen permanently turned on while reading.
     */
    fun setKeepScreenOn(keepOn: Boolean) {
        if (window == null) return
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * Configure cutout mode so the canvas extends through camera notches.
     */
    fun enableShortEdgesCutoutMode() {
        if (window == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
}

/**
 * Remember and observe a [SystemBarController] instance within the Composable tree.
 */
@Composable
fun rememberSystemBarController(): SystemBarController {
    val context = LocalContext.current
    val view = LocalView.current
    val window = (context as? Activity)?.window

    return remember(window, view) {
        SystemBarController(window, view)
    }
}

/**
 * Effect to keep the screen on while this composable is in the active composition.
 */
@Composable
fun KeepScreenOnEffect(enabled: Boolean = true) {
    val controller = rememberSystemBarController()
    DisposableEffect(enabled) {
        if (enabled) {
            controller.setKeepScreenOn(true)
        }
        onDispose {
            if (enabled) {
                controller.setKeepScreenOn(false)
            }
        }
    }
}
