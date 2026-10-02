package com.reader.engine.typography.model

/**
 * 测量与对齐后的单个字符物理布局位置。
 * 记录字符内容、字符在章节全文中的绝对偏移量，以及绘制基准坐标。
 *
 * @property char 字符本身
 * @property charOffset 字符在章节全文中的绝对索引偏移 (0-indexed)
 * @property x 字符左边缘绘制 X 像素坐标
 * @property y 字符基线 (Baseline) 绘制 Y 像素坐标
 * @property width 该字符自身的测量净宽度 (不含两端对齐分配的微调间隙)
 * @property spacingToNext 该字符与下一个字符之间的实际渲染间隙 (包含 letterSpacing 与两端对齐微调间隙)
 */
data class CharPosition(
    val char: Char,
    val charOffset: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val spacingToNext: Float = 0f
)
