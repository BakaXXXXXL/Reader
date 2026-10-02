package com.reader.core.model

/**
 * 支持的电子书离线格式枚举。
 * 包含后缀名与 MIME 类型，以及便捷解析辅助函数。
 */
enum class BookFormat(val extension: String, val mimeType: String) {
    TXT("txt", "text/plain"),
    EPUB("epub", "application/epub+zip"),
    MOBI("mobi", "application/x-mobipocket-ebook"),
    AZW3("azw3", "application/vnd.amazon.ebook"),
    PDF("pdf", "application/pdf");

    companion object {
        fun fromExtension(ext: String?): BookFormat? {
            if (ext.isNullOrBlank()) return null
            val normalized = ext.trim().lowercase().removePrefix(".")
            return entries.firstOrNull { it.extension == normalized }
        }

        fun fromFileName(fileName: String?): BookFormat? {
            if (fileName.isNullOrBlank()) return null
            val ext = fileName.substringAfterLast('.', "")
            return fromExtension(ext)
        }

        fun fromMimeType(mimeType: String?): BookFormat? {
            if (mimeType.isNullOrBlank()) return null
            val normalized = mimeType.trim().lowercase()
            return entries.firstOrNull { it.mimeType.equals(normalized, ignoreCase = true) }
        }
    }
}
