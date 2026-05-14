package com.daria.kotlinbase.data.remote.api

import com.daria.kotlinbase.data.remote.dto.ProductDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ProductApi {
    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @GET("products/{id}")
    suspend fun getProduct(
        @Path("id") id: Int,
    ): ProductDto
}
