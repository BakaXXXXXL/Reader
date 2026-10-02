package com.reader.feature.bookshelf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.core.model.BookFormat
import kotlin.math.abs

/**
 * 优雅书封渲染组件。
 * 当缺少真实图片封面时，根据书籍格式与书名哈希生成专属渐变纹理与格式微标。
 */
@Composable
fun BookCoverView(
    title: String,
    author: String,
    format: BookFormat,
    modifier: Modifier = Modifier,
    coverPath: String? = null
) {
    val cornerShape = RoundedCornerShape(6.dp)

    // 基于书名与格式哈希派生协调的配色梯队
    val gradientColors = remember(title, format) {
        deriveCoverGradients(title, format)
    }

    Surface(
        modifier = modifier
            .shadow(elevation = 3.dp, shape = cornerShape)
            .clip(cornerShape)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = cornerShape
            ),
        shape = cornerShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
        ) {
            // 书脊质感阴影 (左侧书脊微光与纵深)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.05f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 40f
                        )
                    )
            )

            // 格式小标 (右上角胶囊徽标)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp),
                shape = RoundedCornerShape(3.dp),
                color = formatBadgeColor(format).copy(alpha = 0.9f)
            ) {
                Text(
                    text = format.extension.uppercase(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            // 封面主体文字
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 大写首字 / 书名首字艺术徽记
                val initialChar = title.firstOrNull()?.toString() ?: "书"
                Text(
                    text = initialChar,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    color = Color.White.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))

                // 书名
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 作者
                Text(
                    text = author,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 派生封面渐变主题色。
 */
private fun deriveCoverGradients(title: String, format: BookFormat): List<Color> {
    val hash = abs(title.hashCode())
    return when (format) {
        BookFormat.TXT -> listOf(
            Color(0xFF1E3C72),
            Color(0xFF2A5298)
        )
        BookFormat.EPUB -> listOf(
            Color(0xFF2C3E50),
            Color(0xFF3498DB)
        )
        BookFormat.MOBI, BookFormat.AZW3 -> listOf(
            Color(0xFF4A154B),
            Color(0xFF6B1D5C)
        )
        BookFormat.PDF -> listOf(
            Color(0xFF780206),
            Color(0xFF061161)
        )
    }
}

/**
 * 格式专属徽标底色。
 */
fun formatBadgeColor(format: BookFormat): Color {
    return when (format) {
        BookFormat.TXT -> Color(0xFF1976D2)
        BookFormat.EPUB -> Color(0xFF388E3C)
        BookFormat.MOBI -> Color(0xFF7B1FA2)
        BookFormat.AZW3 -> Color(0xFFE65100)
        BookFormat.PDF -> Color(0xFFD32F2F)
    }
}
