package com.reader.core.common.result

/**
 * 统一业务错误模型层级结构。
 * 覆盖文件 IO、格式解析、数据库持久化及远端同步等全场景。
 */
sealed class AppError {
    abstract val message: String
    abstract val userFriendlyMessage: String

    data class FileNotFound(
        val path: String,
        override val message: String = "File not found: $path"
    ) : AppError() {
        override val userFriendlyMessage: String = "目标文件不存在或已被移动"
    }

    data class FileAccessDenied(
        val path: String,
        override val message: String = "Access denied: $path"
    ) : AppError() {
        override val userFriendlyMessage: String = "没有访问该文件的权限，请重新授权"
    }

    data class UnsupportedFormat(
        val extensionOrMime: String?,
        override val message: String = "Unsupported format: $extensionOrMime"
    ) : AppError() {
        override val userFriendlyMessage: String = "暂不支持该文件格式"
    }

    data class FileCorrupted(
        val details: String,
        override val message: String = "Corrupted file: $details"
    ) : AppError() {
        override val userFriendlyMessage: String = "文件已损坏或不完整，无法解析"
    }

    data class EncodingDetectionFailed(
        override val message: String = "Failed to detect file encoding"
    ) : AppError() {
        override val userFriendlyMessage: String = "无法自动识别该文本编码，请尝试手动切换编码"
    }

    data class DatabaseOperationFailed(
        val operation: String,
        override val message: String = "Database error during: $operation",
        val cause: Throwable? = null
    ) : AppError() {
        override val userFriendlyMessage: String = "本地数据存储异常"
    }

    data class ChapterNotFound(
        val chapterIndex: Int,
        override val message: String = "Chapter not found at index: $chapterIndex"
    ) : AppError() {
        override val userFriendlyMessage: String = "未找到指定章节"
    }

    data class SyncFailed(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError() {
        override val userFriendlyMessage: String = "云端同步失败: $message"
    }

    data class Unknown(
        override val message: String = "An unknown error occurred",
        val cause: Throwable? = null
    ) : AppError() {
        override val userFriendlyMessage: String = "发生未知错误"
    }
}
