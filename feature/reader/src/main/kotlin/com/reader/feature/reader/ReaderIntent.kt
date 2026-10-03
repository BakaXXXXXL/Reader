package com.reader.feature.reader

import com.reader.core.model.Bookmark
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReaderThemePreset

/**
 * 阅读器用户意图与单向事件 (MVI ReaderIntent)。
 *
 * 规范所有由 UI 交互触发的意图，输入 ViewModel 处理并驱动 [ReaderUiState] 状态流转。
 */
sealed interface ReaderIntent {

    /** 加载指定书籍与持久化阅读进度 */
    data class LoadBook(val bookId: Long) : ReaderIntent

    /** 点击屏幕中心区域：切换顶部与底部沉浸控制栏的显示/隐藏 */
    data object ToggleControls : ReaderIntent

    /** 明确设定顶部与底部控制栏可见性 */
    data class SetControlsVisible(val visible: Boolean) : ReaderIntent

    /** 翻到上一页 */
    data object PrevPage : ReaderIntent

    /** 翻到下一页 */
    data object NextPage : ReaderIntent

    /** 跳转至当前章节内指定页 (0-based) */
    data class JumpToPage(val pageIndex: Int) : ReaderIntent

    /** 快捷跳转至上一章节起始位置 */
    data object PrevChapter : ReaderIntent

    /** 快捷跳转至下一章节起始位置 */
    data object NextChapter : ReaderIntent

    /** 跳转至目标章节 (由章节索引指定) */
    data class JumpToChapter(val chapterIndex: Int) : ReaderIntent

    /** 拖动底部进度滑块触发进度跳转 (0.0f .. 1.0f) */
    data class SeekToProgress(val progress: Float) : ReaderIntent

    /** 打开或关闭侧边目录抽屉 */
    data class SetDrawerOpen(val isOpen: Boolean) : ReaderIntent

    /** 切换目录抽屉内部的选项卡 (目录 / 书签) */
    data class SwitchDrawerTab(val tab: DrawerTab) : ReaderIntent

    /** 切换目录列表的排列顺序 (正序 / 倒序) */
    data object ToggleChapterOrder : ReaderIntent

    /** 打开或关闭底部排版设置弹窗 */
    data class SetTypographySheetVisible(val visible: Boolean) : ReaderIntent

    /**
     * 相对增减字号 (sp)
     * @param deltaSp 步进值，例如 +1f 或 -1f
     */
    data class ChangeFontSize(val deltaSp: Float) : ReaderIntent

    /** 直接设定固定字号 (sp) */
    data class SetFontSize(val fontSizeSp: Float) : ReaderIntent

    /** 调整行高倍率 (例如 1.2f, 1.6f, 2.0f) */
    data class SetLineHeight(val multiplier: Float) : ReaderIntent

    /** 调整字间距 (em) */
    data class SetLetterSpacing(val letterSpacingEm: Float) : ReaderIntent

    /** 调整段落间距 (dp) */
    data class SetParagraphSpacing(val spacingDp: Float) : ReaderIntent

    /** 调整页面水平左右内边距 (dp) */
    data class SetHorizontalPadding(val paddingDp: Float) : ReaderIntent

    /** 调整页面垂直上下内边距 (dp) */
    data class SetVerticalPadding(val paddingDp: Float) : ReaderIntent

    /** 切换阅读主题预设 (日间白纸/复古羊皮/护眼豆沙/水墨屏/纯黑夜间) */
    data class SelectThemePreset(val preset: ReaderThemePreset) : ReaderIntent

    /** 切换翻页动画模式 (出厂默认横向平滑覆盖/3D仿真/平滑滑动/垂直滚动/无动画) */
    data class SelectPageTurnAnimation(val animation: PageTurnAnimation) : ReaderIntent

    /** 开关阅读屏幕常亮 */
    data class SetKeepScreenOn(val enabled: Boolean) : ReaderIntent

    /** 开关音量物理键翻页支持 */
    data class SetVolumeKeyPageTurn(val enabled: Boolean) : ReaderIntent

    /** 当前阅读页添加或移除书签 */
    data object ToggleBookmark : ReaderIntent

    /** 删除指定已存书签 */
    data class DeleteBookmark(val bookmarkId: Long) : ReaderIntent

    /** 跳转至书签所记录的位置 */
    data class JumpToBookmark(val bookmark: Bookmark) : ReaderIntent

    /** 更新视口物理尺寸并自适应重新排版分页 */
    data class UpdateViewport(val width: Float, val height: Float) : ReaderIntent

    /** 清理当前错误提示 */
    data object ClearError : ReaderIntent
}
