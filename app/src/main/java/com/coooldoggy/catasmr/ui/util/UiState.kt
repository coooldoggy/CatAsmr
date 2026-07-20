package com.coooldoggy.catasmr.ui.util

sealed class AsyncState<out T> {
    data object Idle : AsyncState<Nothing>()
    data object Loading : AsyncState<Nothing>()
    data class Success<T>(val data: T) : AsyncState<T>()
    data class Error(val exception: Exception, val message: String) : AsyncState<Nothing>()
}

sealed class UiError {
    data class PermissionDenied(val permission: String) : UiError()
    data class OperationFailed(val message: String) : UiError()
    data class NetworkError(val message: String) : UiError()
    data class Unknown(val message: String) : UiError()
}
