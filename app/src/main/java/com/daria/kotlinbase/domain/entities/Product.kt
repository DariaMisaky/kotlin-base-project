package com.daria.kotlinbase.domain.entities

data class Product(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String,
    val category: String,
    val imageUrl: String,
    val rating: Double,
    val ratingCount: Int,
)
