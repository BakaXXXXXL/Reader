package com.reader.engine.parser.epub.zip

import com.reader.engine.parser.epub.extractor.PathResolver
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * EPUB ZIP 归档读取器。
 * 统一封装 ZipFile (随机访问) 与 ZipInputStream (流式访问) 两种模式，
 * 支持高效按需解包与大小写容错寻址。
 */
class EpubZipArchive private constructor(
    private val zipFile: ZipFile?,
    private val inMemoryEntries: Map<String, ByteArray>?
) : Closeable {

    companion object {
        /**
         * 从本地文件打开 ZIP 归档 (基于 ZipFile，内存开销低且支持快速随机访问)。
         */
        fun fromFile(file: File): EpubZipArchive {
            require(file.exists()) { "EPUB file does not exist: ${file.absolutePath}" }
            return EpubZipArchive(ZipFile(file), null)
        }

        /**
         * 从输入流流式加载 ZIP 归档。
         * 针对流式场景读取所有条目至内存条目映射中。
         */
        fun fromStream(inputStream: InputStream): EpubZipArchive {
            val entries = mutableMapOf<String, ByteArray>()
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val normalized = PathResolver.normalize(entry.name)
                        entries[normalized] = zis.readBytes()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            return EpubZipArchive(null, entries)
        }

        /**
         * 直接从内存条目字典构建 (常用于极速单元测试)。
         */
        fun fromMemory(entries: Map<String, ByteArray>): EpubZipArchive {
            val normalizedMap = entries.mapKeys { PathResolver.normalize(it.key) }
            return EpubZipArchive(null, normalizedMap)
        }
    }

    /**
     * 获取归档内所有文件条目路径列表。
     */
    fun listEntries(): List<String> {
        return if (zipFile != null) {
            val list = mutableListOf<String>()
            val enumeration = zipFile.entries()
            while (enumeration.hasMoreElements()) {
                val entry = enumeration.nextElement()
                if (!entry.isDirectory) {
                    list.add(PathResolver.normalize(entry.name))
                }
            }
            list
        } else {
            inMemoryEntries?.keys?.toList() ?: emptyList()
        }
    }

    /**
     * 判断归档中是否存在指定路径的条目。
     */
    fun hasEntry(entryPath: String): Boolean {
        val normalized = PathResolver.normalize(entryPath)
        return findEntryName(normalized) != null
    }

    /**
     * 获取条目二进制字节数组。
     */
    fun getEntryBytes(entryPath: String): ByteArray? {
        val normalized = PathResolver.normalize(entryPath)
        if (zipFile != null) {
            val actualName = findEntryName(normalized) ?: return null
            val entry = zipFile.getEntry(actualName) ?: return null
            return zipFile.getInputStream(entry).use { it.readBytes() }
        } else if (inMemoryEntries != null) {
            val actualName = findEntryName(normalized) ?: return null
            return inMemoryEntries[actualName]
        }
        return null
    }

    /**
     * 获取条目输入流。
     */
    fun getEntryStream(entryPath: String): InputStream? {
        val bytes = getEntryBytes(entryPath) ?: return null
        return ByteArrayInputStream(bytes)
    }

    /**
     * 获取条目纯文本内容。
     */
    fun getEntryText(entryPath: String, charset: Charset = Charsets.UTF_8): String? {
        val bytes = getEntryBytes(entryPath) ?: return null
        return String(bytes, charset)
    }

    /**
     * 查找真实条目名 (支持完全匹配与大小写不敏感容错匹配)。
     */
    private fun findEntryName(normalizedPath: String): String? {
        if (zipFile != null) {
            if (zipFile.getEntry(normalizedPath) != null) return normalizedPath
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name
                if (PathResolver.normalize(name).equals(normalizedPath, ignoreCase = true)) {
                    return name
                }
            }
            return null
        } else if (inMemoryEntries != null) {
            if (inMemoryEntries.containsKey(normalizedPath)) return normalizedPath
            return inMemoryEntries.keys.firstOrNull { it.equals(normalizedPath, ignoreCase = true) }
        }
        return null
    }

    override fun close() {
        zipFile?.close()
    }
}
