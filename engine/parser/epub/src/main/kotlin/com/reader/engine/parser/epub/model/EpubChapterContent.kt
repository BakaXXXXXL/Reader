package com.reader.engine.parser.epub.model

/**
 * EPUB 单章节内容结构解析结果。
 *
 * @property chapterTitle 章节标题 (来自首个标题或目录推断)
 * @property contentPath 在 ZIP 归档中的规范化路径
 * @property elements 解析后的结构化正文元素列表 (包含标题、段落、图片)
 * @property plainText 纯净段落文本 (各段以换行拼接，供轻量排版使用)
 * @property imagePaths 本章引用的所有图片完整归档路径列表
 */
data class EpubChapterContent(
    val chapterTitle: String,
    val contentPath: String,
    val elements: List<EpubContentElement>,
    val plainText: String,
    val imagePaths: List<String>
)
