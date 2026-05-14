package com.daria.kotlinbase.shared.utils.extensions

import com.daria.kotlinbase.shared.base.ApiError
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.HttpException

fun Throwable.getParsedError(): String? =
    when (this) {
        is HttpException -> {
            try {
                val body = response()?.errorBody()?.string()
                if (body.isNullOrBlank()) {
                    message
                } else {
                    Gson().fromJson(body, ApiError::class.java).message.ifBlank { message }
                }
            } catch (e: JsonSyntaxException) {
                message
            } catch (e: Exception) {
                message
            }
        }
        else -> message
    }
