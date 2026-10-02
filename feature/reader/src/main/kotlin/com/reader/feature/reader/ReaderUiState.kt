package com.reader.feature.reader

import androidx.compose.runtime.Immutable
import com.reader.core.model.Book
import com.reader.core.model.Bookmark
import com.reader.core.model.Chapter
import com.reader.core.model.ReadLocator
import com.reader.core.model.ReaderConfig

/**
 * 抽屉内选项卡类型：目录 (章节列表) 与 书签列表。
 */
enum class DrawerTab {
    CHAPTERS,
    BOOKMARKS
}

/**
 * 阅读器不可变 UI 状态 (MVI UiState)。
 *
 * 聚合了沉浸阅读器所需的全量视觉与业务状态：
 * 包括书籍元信息、章节与分页进度、排版配置、沉浸交互控制栏开关、
 * 侧边目录抽屉开关、排版调节弹窗开关与书签集合。
 *
 * @property isLoading 数据加载中标志
 * @property errorMessage 异常错误信息 (为 null 时无错误)
 * @property book 当前阅读书籍实体
 * @property currentChapter 当前阅读章节
 * @property chapters 完整章节目录列表
 * @property isChaptersReversed 目录是否逆序展示 (默认 false 为正序)
 * @property currentLocator 统一定位器快照 (用于无缝记忆恢复)
 * @property currentPageIndex 当前章节内页码 (0-indexed)
 * @property totalPagesInChapter 当前章节总页数
 * @property totalProgress 全书进度比例 (0.0f .. 1.0f)
 * @property chapterProgress 本章进度比例 (0.0f .. 1.0f)
 * @property currentPageContent 当前页文本或渲染段落内容
 * @property readerConfig 当前全局/书籍阅读排版与主题配置
 * @property isControlsVisible 沉浸式顶部与底部控制栏是否可见 (触控屏幕中心唤起/隐藏)
 * @property isDrawerOpen 目录与书签侧边抽屉是否处于打开状态
 * @property selectedDrawerTab 目录抽屉当前选中的 Tab (目录 / 书签)
 * @property isTypographySheetVisible 排版设置底部弹窗是否可见
 * @property bookmarks 当前书籍的书签集合
 * @property isCurrentPageBookmarked 当前页面位置是否已添加书签
 */
@Immutable
data class ReaderUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val book: Book? = null,
    val currentChapter: Chapter? = null,
    val chapters: List<Chapter> = emptyList(),
    val isChaptersReversed: Boolean = false,
    val currentLocator: ReadLocator? = null,
    val currentPageIndex: Int = 0,
    val totalPagesInChapter: Int = 1,
    val totalProgress: Float = 0.0f,
    val chapterProgress: Float = 0.0f,
    val currentPageContent: String = "",
    val readerConfig: ReaderConfig = ReaderConfig.DEFAULT,
    val isControlsVisible: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val selectedDrawerTab: DrawerTab = DrawerTab.CHAPTERS,
    val isTypographySheetVisible: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val isCurrentPageBookmarked: Boolean = false
) {
    /**
     * 依据正序/倒序配置派生的目录列表
     */
    val displayChapters: List<Chapter>
        get() = if (isChaptersReversed) chapters.asReversed() else chapters

    /**
     * 格式化全书进度百分比 (如 "35.8%")
     */
    val totalProgressPercent: String
        get() {
            val clamped = (totalProgress.coerceIn(0.0f, 1.0f) * 100.0f)
            return String.format(java.util.Locale.US, "%.1f%%", clamped)
        }

    /**
     * 格式化本章进度百分比 (如 "25.0%")
     */
    val chapterProgressPercent: String
        get() {
            val clamped = (chapterProgress.coerceIn(0.0f, 1.0f) * 100.0f)
            return String.format(java.util.Locale.US, "%.1f%%", clamped)
        }

    /**
     * 页码显示文案 (如 "3 / 15")
     */
    val pageIndicatorText: String
        get() = "${currentPageIndex + 1} / ${totalPagesInChapter.coerceAtLeast(1)}"

    /**
     * 当前章节名称，若为空则显示默认提示
     */
    val chapterTitleDisplay: String
        get() = currentChapter?.title ?: "正在加载章节..."

    /**
     * 书籍标题名称，若为空则显示默认提示
     */
    val bookTitleDisplay: String
        get() = book?.title ?: "本地阅读"

    /**
     * 是否存在上一章
     */
    val hasPrevChapter: Boolean
        get() = (currentChapter?.index ?: 0) > 0

    /**
     * 是否存在下一章
     */
    val hasNextChapter: Boolean
        get() {
            val idx = currentChapter?.index ?: 0
            return chapters.isNotEmpty() && idx < chapters.size - 1
        }
}
