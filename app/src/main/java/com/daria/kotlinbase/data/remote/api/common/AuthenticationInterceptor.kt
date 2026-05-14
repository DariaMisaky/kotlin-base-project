package com.daria.kotlinbase.data.remote.api.common

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Stub auth interceptor. When the app needs an auth token, inject the
 * token provider here and uncomment the `Authorization` header line.
 */
class AuthenticationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/json")
            // .header("Authorization", "Bearer $token")
        return chain.proceed(builder.build())
    }
}
