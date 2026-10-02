package com.reader.engine.parser.epub.extractor

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 安全且具备高容错性的 XML / XHTML 解析工具。
 * 1. 禁用外部网络请求与外部实体解析 (XXE 防御与离线保护)；
 * 2. 预处理未声明的 HTML 实体 (如 &nbsp;, &mdash;)，杜绝 SAX 解析崩溃。
 */
object XmlUtils {

    private val HTML_ENTITIES = mapOf(
        "nbsp" to "\u00A0",
        "ensp" to "\u2002",
        "emsp" to "\u2003",
        "thinsp" to "\u2009",
        "mdash" to "—",
        "ndash" to "–",
        "ldquo" to "“",
        "rdquo" to "”",
        "lsquo" to "‘",
        "rsquo" to "’",
        "hellip" to "…",
        "bull" to "•",
        "middot" to "·",
        "copy" to "©",
        "reg" to "®",
        "trade" to "™",
        "deg" to "°",
        "plusmn" to "±",
        "times" to "×",
        "divide" to "÷",
        "laquo" to "«",
        "raquo" to "»",
        "cent" to "¢",
        "pound" to "£",
        "yen" to "¥",
        "euro" to "€"
    )

    private val ENTITY_REGEX = Regex("&([a-zA-Z0-9]+);")

    /**
     * 创建安全离线的 DocumentBuilder。
     */
    fun createSafeDocumentBuilder(): DocumentBuilder {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isValidating = false
            try {
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            } catch (_: Exception) {
                // 部分运行时可能不支持特定 feature 标记，静默降级
            }
        }
        return factory.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
        }
    }

    /**
     * 将 XML/XHTML 字符串解析为 DOM 文档，自动转换未知命名实体。
     */
    fun parseXml(xmlContent: String): Document {
        val sanitized = sanitizeHtmlEntities(xmlContent)
        val builder = createSafeDocumentBuilder()
        return builder.parse(InputSource(StringReader(sanitized)))
    }

    /**
     * 将未声明的常见 HTML 实体替换为 UTF-8 真实字符，保留标准 XML 预定义实体。
     */
    fun sanitizeHtmlEntities(raw: String): String {
        return ENTITY_REGEX.replace(raw) { match ->
            val entityName = match.groupValues[1]
            when (entityName.lowercase()) {
                "amp", "lt", "gt", "quot", "apos" -> match.value // 标准 XML 实体保留
                else -> HTML_ENTITIES[entityName.lowercase()] ?: " "
            }
        }
    }
}

/**
 * 获取子元素列表 (支持忽略命名空间匹配本地名称或全名)。
 */
fun Element.getChildElements(tagName: String? = null): List<Element> {
    val list = mutableListOf<Element>()
    val nodes = this.childNodes
    for (i in 0 until nodes.length) {
        val node = nodes.item(i)
        if (node.nodeType == Node.ELEMENT_NODE) {
            val el = node as Element
            if (tagName == null) {
                list.add(el)
            } else {
                val localName = el.localName ?: el.tagName
                if (localName.equals(tagName, ignoreCase = true) || el.tagName.equals(tagName, ignoreCase = true)) {
                    list.add(el)
                }
            }
        }
    }
    return list
}

/**
 * 获取第一个指定标签的子元素。
 */
fun Element.getFirstChildElement(tagName: String? = null): Element? {
    return getChildElements(tagName).firstOrNull()
}

/**
 * 递归获取第一个指定标签的后代元素。
 */
fun Element.findDescendant(tagName: String): Element? {
    val children = getChildElements()
    for (child in children) {
        val localName = child.localName ?: child.tagName
        if (localName.equals(tagName, ignoreCase = true) || child.tagName.equals(tagName, ignoreCase = true)) {
            return child
        }
        val found = child.findDescendant(tagName)
        if (found != null) return found
    }
    return null
}

/**
 * 递归获取所有指定标签的后代元素。
 */
fun Element.findDescendants(tagName: String): List<Element> {
    val result = mutableListOf<Element>()
    fun traverse(el: Element) {
        for (child in el.getChildElements()) {
            val localName = child.localName ?: child.tagName
            if (localName.equals(tagName, ignoreCase = true) || child.tagName.equals(tagName, ignoreCase = true)) {
                result.add(child)
            }
            traverse(child)
        }
    }
    traverse(this)
    return result
}

/**
 * 安全获取属性值 (若为空或空白则返回 null)。
 */
fun Element.getAttrOrNull(name: String): String? {
    val value = this.getAttribute(name)
    return if (value.isNullOrBlank()) null else value.trim()
}
