package com.reader.core.common.result

/**
 * 跨模块业务统一结果封装。
 * 遵循函数式与单向数据流规范，包含成功 [Success]、失败 [Error] 与执行中 [Loading] 三种状态。
 */
sealed class AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>()

    data class Error(val error: AppError, val cause: Throwable? = null) : AppResult<Nothing>()

    data object Loading : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw cause ?: IllegalStateException(error.message)
        is Loading -> throw IllegalStateException("Result is still in Loading state")
    }

    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(error, cause)
        is Loading -> Loading
    }

    inline fun <R> flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> Error(error, cause)
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (AppError, Throwable?) -> Unit): AppResult<T> {
        if (this is Error) action(error, cause)
        return this
    }

    inline fun onLoading(action: () -> Unit): AppResult<T> {
        if (this is Loading) action()
        return this
    }

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (AppError, Throwable?) -> R,
        onLoading: () -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(error, cause)
        is Loading -> onLoading()
    }

    companion object {
        inline fun <T> runCatchingAppResult(block: () -> T): AppResult<T> {
            return try {
                Success(block())
            } catch (t: Throwable) {
                Error(
                    error = when (t) {
                        is java.io.FileNotFoundException -> AppError.FileNotFound(t.message ?: "")
                        is SecurityException -> AppError.FileAccessDenied(t.message ?: "")
                        else -> AppError.Unknown(t.message ?: "Unknown error", t)
                    },
                    cause = t
                )
            }
        }
    }
}
