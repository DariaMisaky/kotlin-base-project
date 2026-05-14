package com.daria.kotlinbase.domain.abstractions

import com.daria.kotlinbase.domain.entities.Product

interface ProductRepository {
    suspend fun getProducts(): List<Product>
    suspend fun getProduct(id: Int): Product
}
