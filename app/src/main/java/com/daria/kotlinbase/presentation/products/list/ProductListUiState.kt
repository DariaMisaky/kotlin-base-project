package com.daria.kotlinbase.presentation.products.list

import com.daria.kotlinbase.domain.entities.Product

data class ProductListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null,
)
