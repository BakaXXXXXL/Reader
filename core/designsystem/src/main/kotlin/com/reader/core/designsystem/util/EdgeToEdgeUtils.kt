package com.reader.core.designsystem.util

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Tap action zones for reader full-screen interactions.
 */
enum class ReaderTapAction {
    /** Navigate to previous page */
    PREVIOUS_PAGE,

    /** Navigate to next page */
    NEXT_PAGE,

    /** Toggle reading control overlay (bars, menus) */
    TOGGLE_MENU,

    /** No action */
    NONE;

    companion object {
        /**
         * Resolves tap action based on touch offset and container size.
         * Default configuration:
         * - Left 30%: Previous page
         * - Center 40%: Toggle menu
         * - Right 30%: Next page
         */
        fun resolve(
            tapOffset: Offset,
            containerSize: Size,
            leftRatio: Float = 0.30f,
            rightRatio: Float = 0.70f
        ): ReaderTapAction {
            if (containerSize.width <= 0f) return NONE
            val normalizedX = tapOffset.x / containerSize.width
            return when {
                normalizedX < leftRatio -> PREVIOUS_PAGE
                normalizedX > rightRatio -> NEXT_PAGE
                else -> TOGGLE_MENU
            }
        }
    }
}

/**
 * Utility functions for Edge-to-Edge window setup and gesture detection.
 */
object EdgeToEdgeUtils {

    /**
     * Configures the activity window for full modern edge-to-edge drawing.
     * Compatible across Android 8.0 (API 26) through Android 15 (API 35).
     */
    fun setupEdgeToEdge(activity: Activity) {
        val window: Window = activity.window

        // Tell system to layout behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Make system bars completely transparent
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        // Enable short edges display cutout mode for camera notches
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    /**
     * Enter full-screen immersive mode, hiding both status and navigation bars.
     */
    fun hideSystemBars(activity: Activity) {
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * Exit immersive mode, restoring status and navigation bars.
     */
    fun showSystemBars(activity: Activity) {
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Modifier for detecting reader 3-zone tap gestures.
 */
fun Modifier.readerTapGesture(
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleMenu: () -> Unit
): Modifier = this.pointerInput(Unit) {
    detectTapGestures { offset ->
        when (ReaderTapAction.resolve(offset, containerSize = Size(size.width.toFloat(), size.height.toFloat()))) {
            ReaderTapAction.PREVIOUS_PAGE -> onPreviousPage()
            ReaderTapAction.NEXT_PAGE -> onNextPage()
            ReaderTapAction.TOGGLE_MENU -> onToggleMenu()
            ReaderTapAction.NONE -> Unit
        }
    }
}
