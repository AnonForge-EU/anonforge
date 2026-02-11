package com.anonforge.core.network

/**
 * Sealed class representing network operation results.
 * Provides type-safe error handling for API calls.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()

    /** True if this result is a success. */
    @Suppress("unused") // Public API for result checking
    val isSuccess: Boolean get() = this is Success

    /** True if this result is an error. */
    @Suppress("unused") // Public API for result checking
    val isError: Boolean get() = this is Error

    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data

    @Suppress("unused") // Public API for consumers
    fun getOrElse(defaultValue: @UnsafeVariance T): T = getOrNull() ?: defaultValue

    inline fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Execute action when result is an error.
     * @param action Callback with error message and optional HTTP code
     */
    @Suppress("unused") // Public API for error handling chains
    inline fun onError(action: (String, Int?) -> Unit): NetworkResult<T> {
        if (this is Error) action(message, code)
        return this
    }

    companion object {
        fun <T> success(data: T): NetworkResult<T> = Success(data)
        fun error(message: String, code: Int? = null): NetworkResult<Nothing> = Error(message, code)

        /** Create a loading state. */
        @Suppress("unused") // Public API for loading states
        fun loading(): NetworkResult<Nothing> = Loading

        /**
         * Wrap a suspending block with automatic exception handling.
         * Converts common network exceptions to NetworkResult.Error.
         */
        @Suppress("unused") // Public API for safe API calls
        suspend fun <T> catching(block: suspend () -> T): NetworkResult<T> = try {
            Success(block())
        } catch (_: java.net.UnknownHostException) {
            Error("No internet connection")
        } catch (_: java.net.SocketTimeoutException) {
            Error("Connection timed out")
        } catch (_: Exception) {
            Error("An error occurred")
        }
    }
}

/**
 * Safe API call wrapper with automatic error handling.
 * Use for APIs that return T directly (not Response<T>).
 */
@Suppress("unused") // Available for APIs returning T directly
suspend fun <T> safeApiCall(apiCall: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(apiCall())
    } catch (e: retrofit2.HttpException) {
        NetworkResult.Error(
            message = e.message ?: "HTTP error",
            code = e.code()
        )
    } catch (_: java.net.UnknownHostException) {
        NetworkResult.Error("No internet connection")
    } catch (_: java.net.SocketTimeoutException) {
        NetworkResult.Error("Connection timed out")
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "Unknown error")
    }
}
