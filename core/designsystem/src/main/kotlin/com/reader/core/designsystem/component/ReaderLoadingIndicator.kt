package com.reader.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centered full-screen or full-container loading indicator with an optional status message.
 *
 * @param modifier Modifier for container layout.
 * @param message Optional message displayed beneath the spinner (e.g. "正在分章解析...", "正在排版...").
 * @param progress Optional determinate progress value (0.0f..1.0f). If null, shows indeterminate animation.
 * @param indicatorColor Color for the circular spinner; defaults to [MaterialTheme.colorScheme.primary].
 */
@Composable
fun ReaderLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
    progress: Float? = null,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(size),
                    color = indicatorColor,
                    strokeWidth = strokeWidth
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(size),
                    color = indicatorColor,
                    strokeWidth = strokeWidth
                )
            }

            if (!message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact inline loading indicator for buttons, list items, or small dialogs.
 */
@Composable
fun ReaderInlineLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.5.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = indicatorColor,
            strokeWidth = strokeWidth
        )
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
