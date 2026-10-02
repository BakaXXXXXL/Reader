package com.reader.engine.parser.epub.extractor

import com.reader.engine.parser.epub.model.EpubTocItem
import org.w3c.dom.Element

/**
 * EPUB 2 NCX (Navigation Center eXtended, toc.ncx) 目录解析器。
 * 递归解析多层级树状 navMap 结构。
 */
object TocNcxParser {

    /**
     * 解析 NCX XML 文本。
     *
     * @param ncxXml toc.ncx 内容
     * @param ncxPath toc.ncx 在 ZIP 归档中的路径 (用于解析相对内容路径)
     * @return 多层级目录列表
     */
    fun parse(ncxXml: String, ncxPath: String): List<EpubTocItem> {
        val ncxDir = PathResolver.getDirectory(ncxPath)
        val doc = XmlUtils.parseXml(ncxXml)
        val root = doc.documentElement

        val navMap = root.findDescendant("navMap") ?: return emptyList()
        val navPoints = navMap.getChildElements("navPoint")

        return navPoints.mapIndexed { index, el ->
            parseNavPoint(el, ncxDir, index + 1)
        }
    }

    private fun parseNavPoint(el: Element, ncxDir: String, fallbackOrder: Int): EpubTocItem {
        val id = el.getAttrOrNull("id") ?: "navpoint-$fallbackOrder"
        val playOrder = el.getAttrOrNull("playOrder")?.toIntOrNull() ?: fallbackOrder

        // 解析标题
        val navLabel = el.getFirstChildElement("navLabel")
        val titleText = navLabel?.findDescendant("text")?.textContent?.trim()
            ?: navLabel?.textContent?.trim()
            ?: "未命名章节"

        // 解析内容路径
        val contentEl = el.getFirstChildElement("content")
        val rawSrc = contentEl?.getAttrOrNull("src") ?: ""
        val (pathPart, fragment) = PathResolver.splitFragment(rawSrc)
        val fullContentPath = if (pathPart.isNotEmpty()) PathResolver.resolve(ncxDir, pathPart) else ""

        // 递归解析子节点
        val childNavPoints = el.getChildElements("navPoint")
        val children = childNavPoints.mapIndexed { childIndex, childEl ->
            parseNavPoint(childEl, ncxDir, playOrder * 100 + childIndex + 1)
        }

        return EpubTocItem(
            id = id,
            title = titleText,
            href = rawSrc,
            contentPath = fullContentPath,
            fragment = fragment,
            playOrder = playOrder,
            children = children
        )
    }
}
