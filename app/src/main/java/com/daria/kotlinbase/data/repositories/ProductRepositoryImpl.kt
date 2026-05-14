package com.daria.kotlinbase.data.repositories

import com.daria.kotlinbase.data.mappers.toDomain
import com.daria.kotlinbase.data.remote.api.ProductApi
import com.daria.kotlinbase.domain.abstractions.ProductRepository
import com.daria.kotlinbase.domain.entities.Product

class ProductRepositoryImpl(
    private val api: ProductApi,
) : ProductRepository {
    override suspend fun getProducts(): List<Product> = api.getProducts().map { it.toDomain() }

    override suspend fun getProduct(id: Int): Product = api.getProduct(id).toDomain()
}
