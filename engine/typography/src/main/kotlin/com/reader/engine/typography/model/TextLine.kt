package com.reader.engine.typography.model

/**
 * 经过排版测量、避头尾微调和两端对齐处理后的一行文本。
 *
 * @property lineIndex 当前页内的行序号 (从 0 开始)
 * @property text 该行完整文本内容
 * @property startCharOffset 该行首字符在章节中的绝对偏移量
 * @property endCharOffset 该行末字符后一位在章节中的绝对偏移量 (开区间 [start, end))
 * @property charPositions 行内各个字符的精确定位列表
 * @property isFirstLineOfParagraph 是否为段落首行 (通常带有中文首行缩进)
 * @property isLastLineOfParagraph 是否为段落末行 (末行不执行两端分散对齐，左对齐自然排列)
 * @property x 行左边缘起始坐标 (px)
 * @property y 行顶部 Y 坐标 (px)
 * @property baselineY 行基线 Y 坐标 (px)
 * @property width 行实际占据的宽度 (px)
 * @property height 该行分配的行高 (px)
 */
data class TextLine(
    val lineIndex: Int,
    val text: String,
    val startCharOffset: Int,
    val endCharOffset: Int,
    val charPositions: List<CharPosition> = emptyList(),
    val isFirstLineOfParagraph: Boolean = false,
    val isLastLineOfParagraph: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f,
    val baselineY: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
) {
    /** 行内字符总数 */
    val charCount: Int
        get() = (endCharOffset - startCharOffset).coerceAtLeast(0)

    val isEmpty: Boolean
        get() = text.isEmpty()
}
