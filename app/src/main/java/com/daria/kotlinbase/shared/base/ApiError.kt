package com.daria.kotlinbase.shared.base

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ApiError(
    val message: String = "",
    val code: String = "",
    val errors: List<String>? = null,
) : Parcelable
