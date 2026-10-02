package com.reader.engine.parser.epub.model

/**
 * EPUB 电子书内存领域模型。
 *
 * @property metadata 书籍元数据
 * @property opfPath OPF 描述文件在 ZIP 中的完整路径 (如 "OEBPS/content.opf")
 * @property opfDir OPF 所在目录前缀 (如 "OEBPS/")
 * @property manifest 资源清单映射表 (id -> EpubManifestItem)
 * @property spine 阅读次序列表
 * @property toc 多层级目录树
 * @property coverImagePath 封面图片在 ZIP 中的完整路径 (若已识别)
 * @property version EPUB 规范版本 ("2.0", "3.0" 等)
 */
data class EpubBook(
    val metadata: EpubMetadata,
    val opfPath: String,
    val opfDir: String,
    val manifest: Map<String, EpubManifestItem>,
    val spine: List<EpubSpineItem>,
    val toc: List<EpubTocItem>,
    val coverImagePath: String? = null,
    val version: String = "2.0"
) {
    /** 展平后的目录列表 */
    val flatToc: List<EpubTocItem>
        get() = toc.flatMap { it.flatten() }

    /** 获取 Spine 对应的有效 Manifest 列表 */
    val spineItems: List<EpubManifestItem>
        get() = spine.mapNotNull { manifest[it.idRef] }
}
