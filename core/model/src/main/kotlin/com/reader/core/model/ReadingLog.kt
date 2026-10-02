package com.reader.core.model

/**
 * 阅读统计日志模型。
 * 记录单次或每日阅读时长与阅读字数流水，供阅读日历与热力图分析。
 *
 * @property id 唯一日志 ID (0 表示新增)
 * @property bookId 所属书籍 ID
 * @property readDate 阅读日期，格式 "YYYY-MM-DD"
 * @property durationSeconds 阅读持续有效时长 (秒)
 * @property charactersRead 该时间段内翻阅的估算字符数
 * @property timestamp 记录生成时间戳 (毫秒)
 */
data class ReadingLog(
    val id: Long = 0L,
    val bookId: Long,
    val readDate: String,
    val durationSeconds: Long = 0L,
    val charactersRead: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(durationSeconds >= 0L) { "durationSeconds must be non-negative: $durationSeconds" }
        require(charactersRead >= 0) { "charactersRead must be non-negative: $charactersRead" }
    }
}
