package com.reader.engine.parser.epub.model

/**
 * EPUB 正文抽取出的结构化元素。
 */
sealed interface EpubContentElement {
    /**
     * 标题元素 (h1 ~ h6)。
     *
     * @property text 标题文本
     * @property level 标题层级 (1 - 6)
     */
    data class Heading(
        val text: String,
        val level: Int
    ) : EpubContentElement

    /**
     * 段落纯文本元素。
     *
     * @property text 段落纯净文本
     */
    data class Paragraph(
        val text: String
    ) : EpubContentElement

    /**
     * 内嵌图片元素。
     *
     * @property imagePath 图片在 ZIP 压缩包中的规范化完整路径 (如 "OEBPS/images/fig1.jpg")
     * @property altText 图片替代文本 (如有)
     */
    data class Image(
        val imagePath: String,
        val altText: String? = null
    ) : EpubContentElement
}
