package com.example.transportapp.core.common

/**
 * Minimal Result wrapper mirroring the spec's safe-call convention. Kept dependency-free so
 * every pure-Kotlin module can use it.
 */
sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure(val code: ErrorCode, val message: String? = null, val cause: Throwable? = null) : Result<Nothing>

    fun getOrNull(): T? = (this as? Success)?.value
    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure

    companion object {
        fun <T> success(value: T): Result<T> = Success(value)
        fun failure(code: ErrorCode, message: String? = null, cause: Throwable? = null): Result<Nothing> =
            Failure(code, message, cause)
    }
}
