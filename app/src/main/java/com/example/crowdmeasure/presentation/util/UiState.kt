package com.example.crowdmeasure.presentation.util

/**
 * Generic UI state for one-shot operations or screen loading.
 * - Idle: initial/no-op state
 * - Loading: in progress
 * - Success: has data (or Unit)
 * - Error: has message + optional throwable
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UiState<Nothing>
}

fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success<T>)?.data
fun UiState<*>.isLoading(): Boolean = this is UiState.Loading
fun UiState<*>.isError(): Boolean = this is UiState.Error