package com.reader.engine.parser.epub.extractor

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * 针对 EPUB 内部归档路径的规范化与相对路径解析器。
 */
object PathResolver {

    /**
     * 规范化归档路径：统一正斜杠、去除多余斜杠、解析 `.` 与 `..`、去除前导斜杠。
     */
    fun normalize(path: String): String {
        val decoded = try {
            URLDecoder.decode(path, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            path
        }

        val clean = decoded.replace('\\', '/').trim()
        val segments = clean.split('/')
        val stack = mutableListOf<String>()

        for (segment in segments) {
            when {
                segment.isEmpty() || segment == "." -> continue
                segment == ".." -> {
                    if (stack.isNotEmpty()) {
                        stack.removeAt(stack.size - 1)
                    }
                }
                else -> stack.add(segment)
            }
        }
        return stack.joinToString("/")
    }

    /**
     * 解析相对路径。
     *
     * @param baseDir 基础目录路径 (如 "OEBPS/" 或 "OEBPS/Text/")
     * @param relative 相对路径 (如 "../Images/cover.jpg" 或 "section1.xhtml")
     * @return 解析并规范化后的归档完整路径
     */
    fun resolve(baseDir: String, relative: String): String {
        val cleanRelative = relative.trim().replace('\\', '/')
        if (cleanRelative.startsWith("/")) {
            return normalize(cleanRelative)
        }

        val cleanBase = baseDir.trim().replace('\\', '/')
        val baseDirNormalized = if (cleanBase.endsWith("/")) {
            cleanBase
        } else if (cleanBase.isNotEmpty()) {
            val lastSlash = cleanBase.lastIndexOf('/')
            if (lastSlash >= 0) cleanBase.substring(0, lastSlash + 1) else ""
        } else {
            ""
        }

        return normalize(baseDirNormalized + cleanRelative)
    }

    /**
     * 分离路径与锚点 (fragment)。
     * 如 "Text/ch01.xhtml#p1" -> Pair("Text/ch01.xhtml", "p1")
     */
    fun splitFragment(href: String): Pair<String, String?> {
        val hashIndex = href.indexOf('#')
        return if (hashIndex >= 0) {
            Pair(href.substring(0, hashIndex), href.substring(hashIndex + 1))
        } else {
            Pair(href, null)
        }
    }

    /**
     * 获取文件所在目录 (末尾包含 '/')。
     * 如 "OEBPS/content.opf" -> "OEBPS/"
     * 如 "content.opf" -> ""
     */
    fun getDirectory(filePath: String): String {
        val normalized = normalize(filePath)
        val lastSlash = normalized.lastIndexOf('/')
        return if (lastSlash >= 0) normalized.substring(0, lastSlash + 1) else ""
    }
}
