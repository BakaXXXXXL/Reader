package com.reader.engine.parser.epub.model

/**
 * EPUB Spine 章节阅读顺序项。
 *
 * @property idRef 引用的 Manifest 条目 ID
 * @property linear 是否为主要阅读流 (true 为顺序阅读，false 为辅助/弹出参考)
 */
data class EpubSpineItem(
    val idRef: String,
    val linear: Boolean = true
)
