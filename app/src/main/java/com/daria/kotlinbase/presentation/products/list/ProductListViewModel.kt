package com.daria.kotlinbase.presentation.products.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.daria.kotlinbase.domain.usecases.GetProductsUseCase
import com.daria.kotlinbase.MainNavigationDirections
import com.daria.kotlinbase.shared.base.BaseCommand
import com.daria.kotlinbase.shared.base.BaseViewModel
import com.daria.kotlinbase.shared.base.Result
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val getProductsUseCase: GetProductsUseCase,
) : BaseViewModel() {

    private val _state = MutableLiveData(ProductListUiState())
    val state: LiveData<ProductListUiState> = _state

    init {
        loadProducts()
    }

    fun onProductClicked(productId: Int) {
        _baseCmd.value = BaseCommand.PerformNavAction(
            MainNavigationDirections.actionGlobalProductDetail(productId)
        )
    }

    fun onRefresh() = loadProducts(refresh = true)

    fun onRetry() = loadProducts()

    private fun loadProducts(refresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value?.copy(
                isLoading = !refresh,
                isRefreshing = refresh,
                errorMessage = null,
            )
            when (val result = getProductsUseCase.executeNow(Unit)) {
                is Result.Success -> _state.value = _state.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    products = result.data,
                    errorMessage = null,
                )
                is Result.Error -> _state.value = _state.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = result.error,
                )
            }
        }
    }
}
