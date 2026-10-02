package com.reader.engine.parser.epub.extractor

import com.reader.engine.parser.epub.model.EpubTocItem
import org.w3c.dom.Element

/**
 * EPUB 3 标准导航文档 (nav.xhtml) 目录解析器。
 * 遵循 EPUB 3 Navigation Document 规范解析 <nav epub:type="toc"> 树形层级结构。
 */
object NavXhtmlParser {

    /**
     * 解析 nav.xhtml XML/XHTML 文本。
     *
     * @param navXml nav.xhtml 内容
     * @param navPath nav.xhtml 在 ZIP 归档中的路径
     * @return 多层级目录列表
     */
    fun parse(navXml: String, navPath: String): List<EpubTocItem> {
        val navDir = PathResolver.getDirectory(navPath)
        val doc = XmlUtils.parseXml(navXml)

        val navEl = findTocNavElement(doc.documentElement) ?: return emptyList()
        val listEl = navEl.getChildElements("ol").firstOrNull()
            ?: navEl.getChildElements("ul").firstOrNull()
            ?: navEl.findDescendant("ol")
            ?: navEl.findDescendant("ul")
            ?: return emptyList()

        return parseListItems(listEl, navDir, 1)
    }

    private fun findTocNavElement(root: Element): Element? {
        val navList = root.findDescendants("nav")
        if (navList.isEmpty()) return null

        // 1. 匹配 epub:type="toc" 或 type 包含 toc
        val typeToc = navList.firstOrNull { el ->
            val epubType = el.getAttrOrNull("epub:type") ?: el.getAttrOrNull("type")
            epubType?.contains("toc", ignoreCase = true) == true
        }
        if (typeToc != null) return typeToc

        // 2. 匹配 role="doc-toc"
        val roleToc = navList.firstOrNull { el ->
            el.getAttrOrNull("role")?.contains("doc-toc", ignoreCase = true) == true
        }
        if (roleToc != null) return roleToc

        // 3. 匹配 id="toc"
        val idToc = navList.firstOrNull { el ->
            el.getAttrOrNull("id")?.contains("toc", ignoreCase = true) == true
        }
        if (idToc != null) return idToc

        // 4. 回退至首个 <nav>
        return navList.first()
    }

    private fun parseListItems(listEl: Element, navDir: String, startOrder: Int): List<EpubTocItem> {
        val liElements = listEl.getChildElements("li")
        var currentOrder = startOrder

        return liElements.mapNotNull { li ->
            val item = parseSingleListItem(li, navDir, currentOrder)
            if (item != null) currentOrder++
            item
        }
    }

    private fun parseSingleListItem(liEl: Element, navDir: String, order: Int): EpubTocItem? {
        val anchorEl = liEl.getFirstChildElement("a")
        val spanEl = liEl.getFirstChildElement("span")

        val title: String
        val rawHref: String

        if (anchorEl != null) {
            title = anchorEl.textContent?.trim() ?: "未命名章节"
            rawHref = anchorEl.getAttrOrNull("href") ?: ""
        } else if (spanEl != null) {
            title = spanEl.textContent?.trim() ?: "未命名分组"
            rawHref = ""
        } else {
            // 提取 li 下首段文本作为标题
            val directText = liEl.textContent?.trim() ?: ""
            if (directText.isBlank()) return null
            title = directText.lines().firstOrNull()?.trim() ?: "未命名章节"
            rawHref = ""
        }

        val (pathPart, fragment) = PathResolver.splitFragment(rawHref)
        val fullContentPath = if (pathPart.isNotEmpty()) {
            PathResolver.resolve(navDir, pathPart)
        } else {
            ""
        }

        // 解析嵌套子列表 (ol 或 ul)
        val childListEl = liEl.getChildElements("ol").firstOrNull()
            ?: liEl.getChildElements("ul").firstOrNull()
            ?: liEl.findDescendant("ol")
            ?: liEl.findDescendant("ul")

        val children = if (childListEl != null) {
            parseListItems(childListEl, navDir, order * 100 + 1)
        } else {
            emptyList()
        }

        return EpubTocItem(
            id = "nav-item-$order",
            title = title,
            href = rawHref,
            contentPath = fullContentPath,
            fragment = fragment,
            playOrder = order,
            children = children
        )
    }
}
