package com.reader.engine.parser.epub.extractor

import com.reader.engine.parser.epub.zip.EpubZipArchive

/**
 * META-INF/container.xml 解析器。
 * 遵循 OCF (Open Container Format) 规范定位 OPF 描述文件路径。
 */
object ContainerXmlParser {

    private const val CONTAINER_PATH = "META-INF/container.xml"
    private const val OPF_MIME_TYPE = "application/oebps-package+xml"

    /**
     * 从 ZIP 归档中解析 META-INF/container.xml 并获取 OPF 文件完整路径。
     *
     * @param archive EPUB 归档
     * @return OPF 文件在归档中的规范化完整路径 (如 "OEBPS/content.opf")
     * @throws IllegalArgumentException 当缺少 container.xml 或未找到有效 rootfile 时
     */
    fun parseOpfPath(archive: EpubZipArchive): String {
        val containerXml = archive.getEntryText(CONTAINER_PATH)
            ?: throw IllegalArgumentException("Missing required EPUB container file: $CONTAINER_PATH")

        return parseOpfPathFromXml(containerXml)
    }

    /**
     * 直接从 container.xml 字符串中提取 OPF 完整路径。
     */
    fun parseOpfPathFromXml(xmlContent: String): String {
        val doc = XmlUtils.parseXml(xmlContent)
        val rootfiles = doc.documentElement.findDescendant("rootfiles")
            ?: throw IllegalArgumentException("Invalid container.xml: <rootfiles> tag not found")

        val rootfileList = rootfiles.getChildElements("rootfile")
        if (rootfileList.isEmpty()) {
            throw IllegalArgumentException("Invalid container.xml: No <rootfile> tag defined in <rootfiles>")
        }

        // 优先匹配标准 package+xml 的 rootfile
        val matchedRootfile = rootfileList.firstOrNull {
            it.getAttrOrNull("media-type")?.equals(OPF_MIME_TYPE, ignoreCase = true) == true
        } ?: rootfileList.first()

        val fullPath = matchedRootfile.getAttrOrNull("full-path")
            ?: throw IllegalArgumentException("Invalid container.xml: <rootfile> missing 'full-path' attribute")

        return PathResolver.normalize(fullPath)
    }
}
