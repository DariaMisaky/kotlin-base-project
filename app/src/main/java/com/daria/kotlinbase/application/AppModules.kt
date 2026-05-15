package com.daria.kotlinbase.application

import com.daria.kotlinbase.data.remote.api.common.ApiProvider
import com.daria.kotlinbase.data.repositories.ProductRepositoryImpl
import com.daria.kotlinbase.domain.abstractions.ProductRepository
import com.daria.kotlinbase.domain.usecases.GetProductDetailUseCase
import com.daria.kotlinbase.domain.usecases.GetProductsUseCase
import com.daria.kotlinbase.presentation.products.detail.ProductDetailViewModel
import com.daria.kotlinbase.presentation.products.list.ProductListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object AppModules {
    private val apiModule =
        module {
            single { ApiProvider.provideProductApi() }
        }

    private val repoModule =
        module {
            single<ProductRepository> { ProductRepositoryImpl(get()) }
        }

    private val useCases =
        module {
            single { GetProductsUseCase(get()) }
            single { GetProductDetailUseCase(get()) }
        }

    private val viewModels =
        module {
            viewModel { ProductListViewModel(get()) }
            viewModel { (productId: Int) -> ProductDetailViewModel(productId, get()) }
        }

    val modules = listOf(apiModule, repoModule, useCases, viewModels)
}
