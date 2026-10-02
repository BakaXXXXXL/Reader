package com.reader.engine.parser.epub.model

/**
 * EPUB 树形目录项。
 *
 * @property id 目录项唯一标识
 * @property title 目录标题
 * @property href 原始链接相对路径 (可能含 #fragment，如 "chapter1.xhtml#p1")
 * @property contentPath 在 ZIP 压缩包中的规范化内容文件绝对路径 (不含 #fragment)
 * @property fragment 锚点标识 (如有)
 * @property playOrder 播放/阅读排序号
 * @property children 子级目录列表 (支持多层级嵌套)
 */
data class EpubTocItem(
    val id: String,
    val title: String,
    val href: String,
    val contentPath: String,
    val fragment: String? = null,
    val playOrder: Int = 0,
    val children: List<EpubTocItem> = emptyList()
) {
    /** 展平所有目录项 (前序遍历包含自身与所有子级) */
    fun flatten(): List<EpubTocItem> {
        val result = mutableListOf<EpubTocItem>()
        result.add(this)
        for (child in children) {
            result.addAll(child.flatten())
        }
        return result
    }
}
