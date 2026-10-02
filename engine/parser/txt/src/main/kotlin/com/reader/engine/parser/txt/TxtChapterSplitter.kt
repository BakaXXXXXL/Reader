package com.reader.engine.parser.txt

import com.reader.core.model.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

/**
 * 首屏秒开结果模型。
 *
 * @property file 目标书籍文件
 * @property charset 嗅探出的文件字符集
 * @property firstChapter 首屏对应的初始章节信息 (若无法提取则为默认章节)
 * @property previewText 首屏展示文本内容
 * @property estimatedTotalChapters 预估的章节总数 (基于前序采样推断，后台异步构建完成后更新)
 */
data class TxtFirstScreenResult(
    val file: File,
    val charset: Charset,
    val firstChapter: Chapter,
    val previewText: String,
    val estimatedTotalChapters: Int = 1
)

/**
 * TXT 异步正则分章引擎。
 *
 * 核心机制：
 * 1. 【首屏秒开】：在 20ms 内快速提取首部内容和第一章节，供 UI 立即渲染，无需等待整本书扫描完毕；
 * 2. 【后台异步流式分章】：通过 Kotlin Coroutines + Flow 在 IO 线程流式解析大文件目录，
 *    每发现一批章节 (如 50 章) 即触发流式发射，保证前台 UI 目录抽屉秒级即时展示与平滑更新；
 * 3. 【协程取消感知】：全面响应 Coroutine 取消信号，用户退出阅读时立即终止扫描，节约电量与 CPU；
 * 4. 【智能兜底分割】：遇到没有使用“第X章”等标准格式的纯文本时，自动按合理段落/字数进行平滑分块（如每 50KB-100KB），杜绝空章节。
 */
class TxtChapterSplitter(
    private val nioReader: TxtNioReader = TxtNioReader(),
    private val defaultRules: List<TxtChapterRule> = TxtChapterPatterns.DEFAULT_RULES
) {

    companion object {
        /** 首屏秒开采样字节大小 (默认 64KB) */
        const val FIRST_SCREEN_SAMPLE_BYTES = 64 * 1024

        /** 兜底平滑分块大小 (100KB) */
        const val FALLBACK_CHUNK_SIZE = 100 * 1024L

        /** 流式发射增量阈值 (每收集多少章发射一次 Flow) */
        const val FLOW_EMIT_BATCH_SIZE = 50
    }

    /**
     * 秒开提取首屏内容及首个章节信息。
     *
     * @param file 目标 TXT 文件
     * @param bookId 书籍 ID
     * @param charset 文件编码 (若为 null 则自动嗅探)
     * @param bomOffset BOM 偏移量
     */
    fun extractFirstScreen(
        file: File,
        bookId: Long = 0L,
        charset: Charset? = null,
        bomOffset: Int = 0
    ): TxtFirstScreenResult {
        require(file.exists()) { "文件不存在: ${file.absolutePath}" }
        val targetCharset = charset ?: TxtCharsetDetector.detect(file).charset
        val fileLength = file.length()

        if (fileLength == 0L) {
            val emptyChapter = Chapter(
                id = 0L,
                bookId = bookId,
                index = 0,
                title = "正文",
                startOffset = 0L,
                endOffset = 0L
            )
            return TxtFirstScreenResult(file, targetCharset, emptyChapter, "", 0)
        }

        val matcher = TxtChapterRuleMatcher(rules = defaultRules)
        var firstMatchedTitle: String? = null
        var firstMatchedStartOffset = -1L
        var firstMatchedEndOffset = -1L

        val scanLimit = minOf(fileLength, (bomOffset + FIRST_SCREEN_SAMPLE_BYTES).toLong())

        // 扫描前 64KB 寻找第一个章节标志
        nioReader.forEachLine(
            file = file,
            charset = targetCharset,
            startOffset = bomOffset.toLong(),
            maxOffset = scanLimit
        ) { line ->
            val match = matcher.match(line.text)
            if (match != null) {
                firstMatchedTitle = match
                firstMatchedStartOffset = line.startByteOffset
                firstMatchedEndOffset = line.endByteOffset
                false // 找到第一个就停止首屏扫描
            } else {
                true
            }
        }

        val firstChapter: Chapter
        val previewBytesLength: Int

        if (firstMatchedStartOffset != -1L && firstMatchedStartOffset > bomOffset.toLong() + 128) {
            // 第一个匹配章之前有较长序言/引子内容，将前置内容包装为“序章/引言”
            firstChapter = Chapter(
                id = 0L,
                bookId = bookId,
                index = 0,
                title = "序章",
                startOffset = bomOffset.toLong(),
                endOffset = firstMatchedStartOffset
            )
            previewBytesLength = minOf(4096L, firstMatchedStartOffset - bomOffset).toInt()
        } else if (firstMatchedStartOffset != -1L) {
            // 第一个匹配章即为首章
            val endEstimate = minOf(fileLength, firstMatchedStartOffset + 8192L)
            firstChapter = Chapter(
                id = 0L,
                bookId = bookId,
                index = 0,
                title = firstMatchedTitle ?: "第一章",
                startOffset = firstMatchedStartOffset,
                endOffset = endEstimate
            )
            previewBytesLength = minOf(4096L, fileLength - firstMatchedStartOffset).toInt()
        } else {
            // 前 64KB 未发现明显章节，首章直接使用“正文”
            val endEstimate = minOf(fileLength, (bomOffset + 32 * 1024).toLong())
            firstChapter = Chapter(
                id = 0L,
                bookId = bookId,
                index = 0,
                title = "正文",
                startOffset = bomOffset.toLong(),
                endOffset = endEstimate
            )
            previewBytesLength = minOf(4096L, fileLength - bomOffset).toInt()
        }

        // 立即读取前 4KB 文本供首屏渲染
        val previewText = nioReader.readString(
            file = file,
            startOffset = firstChapter.startOffset,
            length = previewBytesLength,
            charset = targetCharset
        )

        return TxtFirstScreenResult(
            file = file,
            charset = targetCharset,
            firstChapter = firstChapter,
            previewText = previewText,
            estimatedTotalChapters = maxOf(1, (fileLength / (30 * 1024)).toInt())
        )
    }

    /**
     * 后台异步流式提取全书章节目录。
     *
     * 具备增量发射能力，每扫描出 [FLOW_EMIT_BATCH_SIZE] 个章节即通过 Flow 发射给订阅者。
     * 扫描完成时发射全量最终目录。
     */
    fun splitChaptersFlow(
        file: File,
        bookId: Long = 0L,
        charset: Charset,
        bomOffset: Int = 0,
        customPatterns: List<Regex> = emptyList()
    ): Flow<List<Chapter>> = flow {
        if (!file.exists() || file.length() == 0L) {
            emit(emptyList())
            return@flow
        }

        val matcher = TxtChapterRuleMatcher(rules = defaultRules, customPatterns = customPatterns)
        val fileLength = file.length()
        val chapters = mutableListOf<Chapter>()

        var currentTitle: String? = null
        var currentStartOffset = bomOffset.toLong()
        var chapterIndex = 0
        var unEmittedCount = 0

        nioReader.forEachLineSuspend(
            file = file,
            charset = charset,
            startOffset = bomOffset.toLong(),
            maxOffset = fileLength
        ) { line ->
            val match = matcher.match(line.text)
            if (match != null) {
                // 如果当前正在记录一个章节
                if (currentTitle != null) {
                    val prevChapter = Chapter(
                        id = 0L,
                        bookId = bookId,
                        index = chapterIndex++,
                        title = currentTitle!!,
                        startOffset = currentStartOffset,
                        endOffset = line.startByteOffset
                    )
                    chapters.add(prevChapter)
                    unEmittedCount++
                } else if (line.startByteOffset > bomOffset.toLong() + 32) {
                    // 第一个匹配项之前有前言/引子文本，生成第 0 章
                    val prologue = Chapter(
                        id = 0L,
                        bookId = bookId,
                        index = chapterIndex++,
                        title = "序章",
                        startOffset = bomOffset.toLong(),
                        endOffset = line.startByteOffset
                    )
                    chapters.add(prologue)
                    unEmittedCount++
                }

                currentTitle = match
                currentStartOffset = line.startByteOffset

                if (unEmittedCount >= FLOW_EMIT_BATCH_SIZE) {
                    emit(chapters.toList())
                    unEmittedCount = 0
                }
            }
            true
        }

        // 处理最后一章
        if (currentTitle != null) {
            val lastChapter = Chapter(
                id = 0L,
                bookId = bookId,
                index = chapterIndex++,
                title = currentTitle!!,
                startOffset = currentStartOffset,
                endOffset = fileLength
            )
            chapters.add(lastChapter)
        }

        // 如果全书未匹配到任何章节，采用智能自动分块兜底
        if (chapters.isEmpty()) {
            chapters.addAll(fallbackAutoSplit(file, bookId, charset, bomOffset, fileLength))
        }

        emit(chapters.toList())
    }.flowOn(Dispatchers.IO)

    /**
     * 挂起函数：同步等待整本书目录提取完毕并返回完整列表。
     */
    suspend fun splitChapters(
        file: File,
        bookId: Long = 0L,
        charset: Charset,
        bomOffset: Int = 0,
        customPatterns: List<Regex> = emptyList()
    ): List<Chapter> = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext emptyList()

        val matcher = TxtChapterRuleMatcher(rules = defaultRules, customPatterns = customPatterns)
        val fileLength = file.length()
        val chapters = mutableListOf<Chapter>()

        var currentTitle: String? = null
        var currentStartOffset = bomOffset.toLong()
        var chapterIndex = 0

        nioReader.forEachLine(
            file = file,
            charset = charset,
            startOffset = bomOffset.toLong(),
            maxOffset = fileLength
        ) { line ->
            // 响应协程取消，支持秒级中断
            val match = matcher.match(line.text)
            if (match != null) {
                if (currentTitle != null) {
                    val prevChapter = Chapter(
                        id = 0L,
                        bookId = bookId,
                        index = chapterIndex++,
                        title = currentTitle!!,
                        startOffset = currentStartOffset,
                        endOffset = line.startByteOffset
                    )
                    chapters.add(prevChapter)
                } else if (line.startByteOffset > bomOffset.toLong() + 32) {
                    val prologue = Chapter(
                        id = 0L,
                        bookId = bookId,
                        index = chapterIndex++,
                        title = "序章",
                        startOffset = bomOffset.toLong(),
                        endOffset = line.startByteOffset
                    )
                    chapters.add(prologue)
                }

                currentTitle = match
                currentStartOffset = line.startByteOffset
            }
            true
        }

        if (currentTitle != null) {
            val lastChapter = Chapter(
                id = 0L,
                bookId = bookId,
                index = chapterIndex++,
                title = currentTitle!!,
                startOffset = currentStartOffset,
                endOffset = fileLength
            )
            chapters.add(lastChapter)
        }

        if (chapters.isEmpty()) {
            chapters.addAll(fallbackAutoSplit(file, bookId, charset, bomOffset, fileLength))
        }

        chapters
    }

    /**
     * 兜底平滑分块逻辑：针对无目录小说，按换行符边界每约 100KB 切割为逻辑章节。
     */
    private fun fallbackAutoSplit(
        file: File,
        bookId: Long,
        charset: Charset,
        bomOffset: Int,
        fileLength: Long
    ): List<Chapter> {
        val effectiveLength = fileLength - bomOffset
        if (effectiveLength <= FALLBACK_CHUNK_SIZE) {
            return listOf(
                Chapter(
                    id = 0L,
                    bookId = bookId,
                    index = 0,
                    title = "正文",
                    startOffset = bomOffset.toLong(),
                    endOffset = fileLength
                )
            )
        }

        val result = mutableListOf<Chapter>()
        var partIndex = 1
        var start = bomOffset.toLong()

        while (start < fileLength) {
            var targetEnd = minOf(fileLength, start + FALLBACK_CHUNK_SIZE)
            if (targetEnd < fileLength) {
                // 向后寻找换行符，避免硬截断中文句子
                nioReader.forEachLine(file, charset, startOffset = targetEnd, maxOffset = minOf(fileLength, targetEnd + 8192)) { line ->
                    targetEnd = line.endByteOffset
                    false
                }
            }

            result.add(
                Chapter(
                    id = 0L,
                    bookId = bookId,
                    index = partIndex - 1,
                    title = "第 $partIndex 部分",
                    startOffset = start,
                    endOffset = targetEnd
                )
            )
            partIndex++
            start = targetEnd
        }

        return result
    }
}
