package com.reader.engine.parser.epub.model

/**
 * EPUB 书籍元数据模型。
 *
 * @property title 书名
 * @property author 作者 (默认为 "未知作者")
 * @property language 语言 (如 "zh-CN", "en")
 * @property identifier 唯一标识 (如 UUID, ISBN)
 * @property publisher 出版社
 * @property description 书籍简介
 * @property publishDate 出版日期
 * @property coverItemId OPF 中标记的封面资源 ID
 */
data class EpubMetadata(
    val title: String,
    val author: String = "未知作者",
    val language: String? = null,
    val identifier: String? = null,
    val publisher: String? = null,
    val description: String? = null,
    val publishDate: String? = null,
    val coverItemId: String? = null
)
