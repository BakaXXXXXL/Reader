package com.reader.feature.bookshelf.model

import com.reader.core.model.BookFormat
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

/**
 * 本地待导入候选文件模型。
 * 用于 SAF 单选/多选或目录扫描后的展示与导入状态跟踪。
 */
data class ImportCandidate(
    val uriString: String,
    val fileName: String,
    val fileSize: Long,
    val format: BookFormat,
    val isSelected: Boolean = true,
    val status: ImportCandidateStatus = ImportCandidateStatus.PENDING,
    val errorMessage: String? = null
) {
    /** 格式化文件大小 */
    val formattedFileSize: String
        get() {
            if (fileSize <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (log10(fileSize.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = fileSize / 1024.0.pow(digitGroups.toDouble())
            return if (digitGroups == 0) {
                "${fileSize} B"
            } else {
                String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
            }
        }
}

/**
 * 候选文件导入生命周期状态。
 */
enum class ImportCandidateStatus {
    /** 等待导入 */
    PENDING,

    /** 正在导入与解析元数据 */
    IMPORTING,

    /** 导入成功并已入库 */
    SUCCESS,

    /** 导入失败 */
    FAILED,

    /** 书籍已存在，已跳过 */
    SKIPPED_ALREADY_EXISTS
}
