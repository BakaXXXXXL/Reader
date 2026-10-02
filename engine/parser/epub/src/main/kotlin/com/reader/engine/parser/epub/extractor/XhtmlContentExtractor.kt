package com.reader.engine.parser.epub.extractor

import com.reader.engine.parser.epub.model.EpubChapterContent
import com.reader.engine.parser.epub.model.EpubContentElement
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * EPUB XHTML 正文轻量级内容提取器。
 * 1. 过滤 script, style, head, noscript 等无关干扰标签；
 * 2. 提取层级标题 (h1 ~ h6)、正文段落与内嵌图片；
 * 3. 规范化内嵌图片相对路径为归档完整路径；
 * 4. 汇总纯净段落文本与内嵌图片清单。
 */
object XhtmlContentExtractor {

    private val IGNORED_TAGS = setOf(
        "script", "style", "head", "noscript", "meta", "link"
    )

    private val HEADING_TAGS = setOf(
        "h1", "h2", "h3", "h4", "h5", "h6"
    )

    private val BLOCK_LEAF_TAGS = setOf(
        "p", "blockquote", "pre", "li", "dd", "dt"
    )

    /**
     * 提取 XHTML 单章正文。
     *
     * @param xhtmlContent XHTML 源码
     * @param chapterPath 本章节在 ZIP 压缩包中的路径 (如 "OEBPS/Text/ch01.xhtml")
     * @param defaultTitle 外部推断的默认标题 (如来自 TOC)
     * @return 结构化章节内容
     */
    fun extract(
        xhtmlContent: String,
        chapterPath: String,
        defaultTitle: String? = null
    ): EpubChapterContent {
        val chapterDir = PathResolver.getDirectory(chapterPath)
        val doc = XmlUtils.parseXml(xhtmlContent)
        val body = doc.documentElement.findDescendant("body") ?: doc.documentElement

        val elements = mutableListOf<EpubContentElement>()
        val imagePaths = mutableListOf<String>()

        processNode(body, chapterDir, elements, imagePaths)

        // 推断章节标题
        val firstHeading = elements.filterIsInstance<EpubContentElement.Heading>().firstOrNull()?.text
        val inferredTitle = when {
            !defaultTitle.isNullOrBlank() -> defaultTitle
            !firstHeading.isNullOrBlank() -> firstHeading
            else -> elements.filterIsInstance<EpubContentElement.Paragraph>().firstOrNull()?.text?.take(30)
                ?: "未命名章节"
        }

        // 拼接纯文本 (标题与段落)
        val plainText = elements.mapNotNull {
            when (it) {
                is EpubContentElement.Heading -> it.text
                is EpubContentElement.Paragraph -> it.text
                is EpubContentElement.Image -> null
            }
        }.filter { it.isNotBlank() }.joinToString("\n\n")

        return EpubChapterContent(
            chapterTitle = inferredTitle,
            contentPath = chapterPath,
            elements = elements,
            plainText = plainText,
            imagePaths = imagePaths
        )
    }

    private fun processNode(
        node: Node,
        chapterDir: String,
        elements: MutableList<EpubContentElement>,
        imagePaths: MutableList<String>
    ) {
        if (node.nodeType != Node.ELEMENT_NODE) return
        val el = node as Element
        val tagName = (el.localName ?: el.tagName).lowercase()

        if (IGNORED_TAGS.contains(tagName)) return

        when {
            HEADING_TAGS.contains(tagName) -> {
                val level = tagName.substring(1).toIntOrNull() ?: 1
                val text = extractInlineText(el).trim()
                if (text.isNotBlank()) {
                    elements.add(EpubContentElement.Heading(text = text, level = level))
                }
            }

            tagName == "img" -> {
                val src = el.getAttrOrNull("src")
                if (!src.isNullOrBlank()) {
                    val resolved = PathResolver.resolve(chapterDir, src)
                    val alt = el.getAttrOrNull("alt")
                    elements.add(EpubContentElement.Image(imagePath = resolved, altText = alt))
                    imagePaths.add(resolved)
                }
            }

            tagName == "image" -> {
                // SVG <image xlink:href="..." href="..."/>
                val href = el.getAttrOrNull("xlink:href")
                    ?: el.getAttrOrNull("href")
                    ?: el.getAttribute("xlink:href").takeIf { it.isNotBlank() }
                if (!href.isNullOrBlank()) {
                    val resolved = PathResolver.resolve(chapterDir, href)
                    elements.add(EpubContentElement.Image(imagePath = resolved, altText = null))
                    imagePaths.add(resolved)
                }
            }

            BLOCK_LEAF_TAGS.contains(tagName) -> {
                // 段落叶子节点：可能内含图片或文本
                processParagraphBlock(el, chapterDir, elements, imagePaths)
            }

            else -> {
                // 容器节点 (div, section, article, table 等)
                // 判断是否包含子块元素或图片
                if (containsBlockOrMedia(el)) {
                    val children = el.childNodes
                    for (i in 0 until children.length) {
                        processNode(children.item(i), chapterDir, elements, imagePaths)
                    }
                } else {
                    // 没有子块元素，纯文本容器，当做段落处理
                    val text = extractInlineText(el).trim()
                    if (text.isNotBlank()) {
                        elements.add(EpubContentElement.Paragraph(text))
                    }
                }
            }
        }
    }

    private fun processParagraphBlock(
        el: Element,
        chapterDir: String,
        elements: MutableList<EpubContentElement>,
        imagePaths: MutableList<String>
    ) {
        // 段落内部如果嵌有 img/image，需分别拆分输出图片与前后文本
        val children = el.childNodes
        var currentText = StringBuilder()

        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.ELEMENT_NODE) {
                val childEl = child as Element
                val tag = (childEl.localName ?: childEl.tagName).lowercase()
                if (tag == "img") {
                    val text = currentText.toString().trim()
                    if (text.isNotBlank()) {
                        elements.add(EpubContentElement.Paragraph(text))
                        currentText = StringBuilder()
                    }
                    val src = childEl.getAttrOrNull("src")
                    if (!src.isNullOrBlank()) {
                        val resolved = PathResolver.resolve(chapterDir, src)
                        elements.add(EpubContentElement.Image(resolved, childEl.getAttrOrNull("alt")))
                        imagePaths.add(resolved)
                    }
                    continue
                } else if (tag == "image") {
                    val text = currentText.toString().trim()
                    if (text.isNotBlank()) {
                        elements.add(EpubContentElement.Paragraph(text))
                        currentText = StringBuilder()
                    }
                    val href = childEl.getAttrOrNull("xlink:href") ?: childEl.getAttrOrNull("href")
                    if (!href.isNullOrBlank()) {
                        val resolved = PathResolver.resolve(chapterDir, href)
                        elements.add(EpubContentElement.Image(resolved, null))
                        imagePaths.add(resolved)
                    }
                    continue
                } else if (tag == "br") {
                    currentText.append("\n")
                    continue
                }
            }
            currentText.append(extractInlineText(child))
        }

        val remainingText = currentText.toString().trim()
        if (remainingText.isNotBlank()) {
            elements.add(EpubContentElement.Paragraph(remainingText))
        }
    }

    private fun containsBlockOrMedia(el: Element): Boolean {
        val children = el.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.ELEMENT_NODE) {
                val tag = ((child as Element).localName ?: child.tagName).lowercase()
                if (BLOCK_LEAF_TAGS.contains(tag) ||
                    HEADING_TAGS.contains(tag) ||
                    tag == "div" || tag == "section" || tag == "article" ||
                    tag == "img" || tag == "image" || tag == "svg" ||
                    tag == "table" || tag == "ul" || tag == "ol"
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun extractInlineText(node: Node): String {
        return when (node.nodeType) {
            Node.TEXT_NODE -> {
                node.nodeValue ?: ""
            }
            Node.ELEMENT_NODE -> {
                val el = node as Element
                val tag = (el.localName ?: el.tagName).lowercase()
                if (IGNORED_TAGS.contains(tag)) return ""
                if (tag == "br") return "\n"

                val sb = StringBuilder()
                val children = el.childNodes
                for (i in 0 until children.length) {
                    sb.append(extractInlineText(children.item(i)))
                }
                sb.toString()
            }
            else -> ""
        }
    }
}
