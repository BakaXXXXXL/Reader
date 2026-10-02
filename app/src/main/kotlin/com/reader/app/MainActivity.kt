package com.reader.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.reader.app.navigation.AppNavigation
import com.reader.app.navigation.AppRoute
import com.reader.core.designsystem.theme.ReaderTheme

/**
 * 主入口 Activity。
 * 全面适配 Edge-to-Edge 边缘沉浸式全屏，承载顶层 Compose 导航。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启用沉浸式边缘到边缘
        enableEdgeToEdge()

        // 检查是否有外部文件通过 Intent 打开
        val initialRoute = parseInitialRouteFromIntent(intent)

        setContent {
            ReaderTheme {
                AppNavigation(
                    initialRoute = initialRoute,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun parseInitialRouteFromIntent(intent: Intent?): AppRoute {
        if (intent == null) return AppRoute.Bookshelf
        val action = intent.action
        val data: Uri? = intent.data

        if (Intent.ACTION_VIEW == action && data != null) {
            // 通过外部打开电子书文件（例如 content:// 或 file://）
            // 路由至阅读器，由数据源桥接载入该文件
            return AppRoute.Reader(bookId = 0L)
        }
        return AppRoute.Bookshelf
    }
}
