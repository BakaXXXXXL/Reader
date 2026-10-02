package com.reader.core.model

/**
 * 书籍核心领域模型。
 *
 * @property id 唯一主键标识 (0 表示未持久化新增对象)
 * @property title 书名
 * @property author 作者名称 (默认 "未知作者")
 * @property coverPath 封面缓存文件绝对路径 (若存在)
 * @property uriString 存储文件 URI 字符串 (SAF content:// 或本地绝对路径)
 * @property format 电子书格式 (TXT, EPUB, MOBI, AZW3, PDF)
 * @property fileSize 文件字节大小
 * @property totalChapters 章节总数
 * @property addTime 加入书架时间戳 (毫秒)
 * @property lastReadTime 最近一次阅读时间戳 (毫秒)
 * @property archivePath 临时解包或解密缓存根目录绝对路径 (如 EPUB 缓存)
 */
data class Book(
    val id: Long = 0L,
    val title: String,
    val author: String = "未知作者",
    val coverPath: String? = null,
    val uriString: String,
    val format: BookFormat,
    val fileSize: Long,
    val totalChapters: Int = 0,
    val addTime: Long = System.currentTimeMillis(),
    val lastReadTime: Long = System.currentTimeMillis(),
    val archivePath: String? = null
) {
    val isTxt: Boolean get() = format == BookFormat.TXT
    val isEpub: Boolean get() = format == BookFormat.EPUB
    val isMobiOrAzw: Boolean get() = format == BookFormat.MOBI || format == BookFormat.AZW3
    val isPdf: Boolean get() = format == BookFormat.PDF
}
