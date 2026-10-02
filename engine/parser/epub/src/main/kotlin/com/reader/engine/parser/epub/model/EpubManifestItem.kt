package com.reader.engine.parser.epub.model

/**
 * EPUB Manifest 清单资源条目。
 *
 * @property id 资源唯一 ID
 * @property href 相对 OPF 文件的资源相对路径 (未解码/解码后)
 * @property fullPath 资源在 ZIP 压缩包中的完整规范化路径 (如 "OEBPS/Images/cover.jpg")
 * @property mediaType 资源的 MIME 类型 (如 "application/xhtml+xml", "image/jpeg")
 * @property properties 附加属性 (如 "cover-image", "nav")
 */
data class EpubManifestItem(
    val id: String,
    val href: String,
    val fullPath: String,
    val mediaType: String,
    val properties: String? = null
) {
    /** 是否为 EPUB 3 标准声明的封面图片 */
    val isCoverImage: Boolean
        get() = properties?.split("\\s+".toRegex())?.contains("cover-image") == true ||
                id.equals("cover", ignoreCase = true) ||
                id.equals("cover-image", ignoreCase = true)

    /** 是否为 EPUB 3 标准导航文档 */
    val isNav: Boolean
        get() = properties?.split("\\s+".toRegex())?.contains("nav") == true

    /** 是否为 EPUB 2 NCX 目录文件 */
    val isNcx: Boolean
        get() = mediaType.equals("application/x-dtbncx+xml", ignoreCase = true) ||
                href.endsWith(".ncx", ignoreCase = true)

    /** 是否为 XHTML / HTML 文本内容 */
    val isXhtml: Boolean
        get() = mediaType.equals("application/xhtml+xml", ignoreCase = true) ||
                mediaType.equals("text/html", ignoreCase = true) ||
                href.endsWith(".xhtml", ignoreCase = true) ||
                href.endsWith(".html", ignoreCase = true)

    /** 是否为图像资源 */
    val isImage: Boolean
        get() = mediaType.startsWith("image/", ignoreCase = true)
}
