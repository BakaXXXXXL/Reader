package com.reader.feature.bookshelf.model

/**
 * 本地文件导入对话框状态。
 * 支持 SAF 单选/多选及本地目录扫描，并实时跟踪批量导入进度。
 */
data class ImportDialogState(
    val isVisible: Boolean = false,
    val candidates: List<ImportCandidate> = emptyList(),
    val isImporting: Boolean = false,
    val currentIndex: Int = 0,
    val currentFileName: String? = null,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val skippedCount: Int = 0,
    val scanPath: String = "",
    val isScanning: Boolean = false,
    val scanErrorMessage: String? = null
) {
    /** 候选文件总数 */
    val totalCount: Int get() = candidates.size

    /** 用户勾选待导入的数量 */
    val selectedCount: Int get() = candidates.count { it.isSelected }

    /** 是否全部勾选 */
    val isAllSelected: Boolean get() = candidates.isNotEmpty() && candidates.all { it.isSelected }

    /** 导入总进度 (0.0f ~ 1.0f) */
    val progress: Float
        get() {
            val total = selectedCount.takeIf { it > 0 } ?: totalCount
            if (total == 0) return 0.0f
            return (currentIndex.toFloat() / total.toFloat()).coerceIn(0.0f, 1.0f)
        }

    /** 是否所有任务已完成 */
    val isFinished: Boolean
        get() = !isImporting && (successCount > 0 || failureCount > 0 || skippedCount > 0)
}
