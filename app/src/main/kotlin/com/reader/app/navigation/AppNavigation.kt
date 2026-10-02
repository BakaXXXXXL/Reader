package com.reader.app.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.reader.feature.bookshelf.ui.BookshelfScreen
import com.reader.feature.reader.ReaderScreen

/**
 * 应用顶层导航宿主。
 * 实现书架与沉浸式阅读器之间的双向无缝跳转。
 */
@Composable
fun AppNavigation(
    initialRoute: AppRoute = AppRoute.Bookshelf,
    modifier: Modifier = Modifier
) {
    var currentRoute by remember { mutableStateOf(initialRoute) }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            if (targetState is AppRoute.Reader) {
                // 进入阅读器：从右向左滑入
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                )
            } else {
                // 返回书架：从左向右滑回
                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "AppNavigationTransition",
        modifier = modifier.fillMaxSize()
    ) { route ->
        when (route) {
            is AppRoute.Bookshelf -> {
                BookshelfScreen(
                    onBookClick = { bookItem ->
                        currentRoute = AppRoute.Reader(bookId = bookItem.book.id)
                    }
                )
            }

            is AppRoute.Reader -> {
                ReaderScreen(
                    onBackClick = {
                        currentRoute = AppRoute.Bookshelf
                    }
                )
            }
        }
    }
}
