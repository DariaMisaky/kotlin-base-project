package com.daria.kotlinbase.presentation.products.detail

import com.daria.kotlinbase.domain.entities.Product

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: Product? = null,
    val errorMessage: String? = null,
)
