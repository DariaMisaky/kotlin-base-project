package com.daria.kotlinbase.shared.base

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val error: String?, var errorCode: Int = -1) : Result<Nothing>()
}
