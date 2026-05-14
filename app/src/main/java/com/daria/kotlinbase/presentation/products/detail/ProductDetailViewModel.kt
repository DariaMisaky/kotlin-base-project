package com.daria.kotlinbase.presentation.products.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.daria.kotlinbase.domain.usecases.GetProductDetailUseCase
import com.daria.kotlinbase.shared.base.BaseCommand
import com.daria.kotlinbase.shared.base.BaseViewModel
import com.daria.kotlinbase.shared.base.Result
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val productId: Int,
    private val getProductDetailUseCase: GetProductDetailUseCase,
) : BaseViewModel() {

    private val _state = MutableLiveData(ProductDetailUiState())
    val state: LiveData<ProductDetailUiState> = _state

    init {
        loadDetail()
    }

    fun onBack() {
        _baseCmd.value = BaseCommand.GoBack
    }

    fun onRetry() = loadDetail()

    private fun loadDetail() {
        viewModelScope.launch {
            _state.value = ProductDetailUiState(isLoading = true)
            when (val result = getProductDetailUseCase.executeNow(productId)) {
                is Result.Success -> _state.value = ProductDetailUiState(
                    isLoading = false,
                    product = result.data,
                )
                is Result.Error -> _state.value = ProductDetailUiState(
                    isLoading = false,
                    errorMessage = result.error,
                )
            }
        }
    }
}
