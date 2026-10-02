package com.reader.engine.parser.epub.extractor

import com.reader.engine.parser.epub.model.EpubManifestItem
import com.reader.engine.parser.epub.model.EpubMetadata
import com.reader.engine.parser.epub.model.EpubSpineItem
import org.w3c.dom.Element

/**
 * OPF (Open Packaging Format) 数据解析器。
 * 负责解析 EPUB package 核心描述文件：
 * 1. Metadata (书名、作者、语言、封面标记等)；
 * 2. Manifest (包内全部资源清单，解析完整绝对路径)；
 * 3. Spine (主阅读流顺序)；
 * 4. 封面图片归档路径智能判定 (兼容 EPUB 2 与 EPUB 3 标准)。
 */
object OpfPackageParser {

    data class OpfResult(
        val version: String,
        val metadata: EpubMetadata,
        val manifest: Map<String, EpubManifestItem>,
        val spine: List<EpubSpineItem>,
        val tocManifestId: String?,
        val coverImagePath: String?
    )

    /**
     * 解析 OPF XML 内容。
     *
     * @param opfXml OPF 文本字符串
     * @param opfPath OPF 在 ZIP 压缩包中的路径 (如 "OEBPS/content.opf")
     */
    fun parse(opfXml: String, opfPath: String): OpfResult {
        val opfDir = PathResolver.getDirectory(opfPath)
        val doc = XmlUtils.parseXml(opfXml)
        val packageEl = doc.documentElement

        val version = packageEl.getAttrOrNull("version") ?: "2.0"

        // 1. Metadata
        val metadataEl = packageEl.getFirstChildElement("metadata")
        val metadata = parseMetadata(metadataEl)

        // 2. Manifest
        val manifestEl = packageEl.getFirstChildElement("manifest")
        val manifest = parseManifest(manifestEl, opfDir)

        // 3. Spine
        val spineEl = packageEl.getFirstChildElement("spine")
        val (spineList, tocId) = parseSpine(spineEl)

        // 4. 识别封面图片完整路径
        val coverImagePath = resolveCoverImagePath(metadata, manifest)

        return OpfResult(
            version = version,
            metadata = metadata,
            manifest = manifest,
            spine = spineList,
            tocManifestId = tocId,
            coverImagePath = coverImagePath
        )
    }

    private fun parseMetadata(metadataEl: Element?): EpubMetadata {
        if (metadataEl == null) {
            return EpubMetadata(title = "未知书名")
        }

        var title: String? = null
        val authors = mutableListOf<String>()
        var language: String? = null
        var identifier: String? = null
        var publisher: String? = null
        var description: String? = null
        var publishDate: String? = null
        var coverItemId: String? = null

        val children = metadataEl.getChildElements()
        for (child in children) {
            val localName = child.localName ?: child.tagName
            val text = child.textContent?.trim() ?: ""

            when {
                localName.equals("title", ignoreCase = true) ||
                        child.tagName.endsWith(":title", ignoreCase = true) -> {
                    if (title.isNullOrBlank() && text.isNotBlank()) title = text
                }
                localName.equals("creator", ignoreCase = true) ||
                        child.tagName.endsWith(":creator", ignoreCase = true) -> {
                    if (text.isNotBlank()) authors.add(text)
                }
                localName.equals("language", ignoreCase = true) ||
                        child.tagName.endsWith(":language", ignoreCase = true) -> {
                    if (language.isNullOrBlank() && text.isNotBlank()) language = text
                }
                localName.equals("identifier", ignoreCase = true) ||
                        child.tagName.endsWith(":identifier", ignoreCase = true) -> {
                    if (identifier.isNullOrBlank() && text.isNotBlank()) identifier = text
                }
                localName.equals("publisher", ignoreCase = true) ||
                        child.tagName.endsWith(":publisher", ignoreCase = true) -> {
                    if (publisher.isNullOrBlank() && text.isNotBlank()) publisher = text
                }
                localName.equals("description", ignoreCase = true) ||
                        child.tagName.endsWith(":description", ignoreCase = true) -> {
                    if (description.isNullOrBlank() && text.isNotBlank()) description = text
                }
                localName.equals("date", ignoreCase = true) ||
                        child.tagName.endsWith(":date", ignoreCase = true) -> {
                    if (publishDate.isNullOrBlank() && text.isNotBlank()) publishDate = text
                }
                localName.equals("meta", ignoreCase = true) -> {
                    // EPUB 2 封面标记: <meta name="cover" content="cover-image-id"/>
                    val nameAttr = child.getAttrOrNull("name")
                    val contentAttr = child.getAttrOrNull("content")
                    if (nameAttr.equals("cover", ignoreCase = true) && !contentAttr.isNullOrBlank()) {
                        coverItemId = contentAttr
                    }
                }
            }
        }

        val resolvedAuthor = if (authors.isNotEmpty()) authors.joinToString(", ") else "未知作者"
        val resolvedTitle = if (!title.isNullOrBlank()) title else "未知书名"

        return EpubMetadata(
            title = resolvedTitle,
            author = resolvedAuthor,
            language = language,
            identifier = identifier,
            publisher = publisher,
            description = description,
            publishDate = publishDate,
            coverItemId = coverItemId
        )
    }

    private fun parseManifest(manifestEl: Element?, opfDir: String): Map<String, EpubManifestItem> {
        if (manifestEl == null) return emptyMap()

        val items = mutableMapOf<String, EpubManifestItem>()
        val itemElements = manifestEl.getChildElements("item")

        for (el in itemElements) {
            val id = el.getAttrOrNull("id") ?: continue
            val href = el.getAttrOrNull("href") ?: continue
            val mediaType = el.getAttrOrNull("media-type") ?: "application/octet-stream"
            val properties = el.getAttrOrNull("properties")

            // 解析在 ZIP 内部的完整物理路径
            val fullPath = PathResolver.resolve(opfDir, href)

            items[id] = EpubManifestItem(
                id = id,
                href = href,
                fullPath = fullPath,
                mediaType = mediaType,
                properties = properties
            )
        }

        return items
    }

    private fun parseSpine(spineEl: Element?): Pair<List<EpubSpineItem>, String?> {
        if (spineEl == null) return Pair(emptyList(), null)

        val tocId = spineEl.getAttrOrNull("toc")
        val items = mutableListOf<EpubSpineItem>()

        val itemrefElements = spineEl.getChildElements("itemref")
        for (el in itemrefElements) {
            val idRef = el.getAttrOrNull("idref") ?: continue
            val linearAttr = el.getAttrOrNull("linear")
            val linear = !linearAttr.equals("no", ignoreCase = true)
            items.add(EpubSpineItem(idRef = idRef, linear = linear))
        }

        return Pair(items, tocId)
    }

    /**
     * 智能判定并返回封面图片在 ZIP 中的完整规范化路径。
     */
    private fun resolveCoverImagePath(
        metadata: EpubMetadata,
        manifest: Map<String, EpubManifestItem>
    ): String? {
        // 1. 优先 EPUB 3 标准：manifest item 声明 properties="cover-image"
        val epub3Cover = manifest.values.firstOrNull { it.isCoverImage && it.isImage }
        if (epub3Cover != null) return epub3Cover.fullPath

        // 2. 检查 EPUB 2 元数据引用的 coverItemId
        if (!metadata.coverItemId.isNullOrBlank()) {
            val item = manifest[metadata.coverItemId]
            if (item != null) return item.fullPath
        }

        // 3. 检查 Manifest 条目 ID 命名为 "cover" 或 "cover-image"
        val idCover = manifest.values.firstOrNull {
            it.isImage && (it.id.equals("cover", ignoreCase = true) || it.id.equals("cover-image", ignoreCase = true))
        }
        if (idCover != null) return idCover.fullPath

        // 4. 检查 Manifest 条目文件名包含 "cover"
        val hrefCover = manifest.values.firstOrNull {
            it.isImage && it.href.substringAfterLast('/').contains("cover", ignoreCase = true)
        }
        if (hrefCover != null) return hrefCover.fullPath

        return null
    }
}
