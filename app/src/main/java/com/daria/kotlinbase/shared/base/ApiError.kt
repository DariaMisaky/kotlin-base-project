package com.daria.kotlinbase.shared.base

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ApiError(
    @SerializedName("message") val message: String = "",
    @SerializedName("code") val code: String = "",
    @SerializedName("errors") val errors: List<String>? = null,
) : Parcelable
