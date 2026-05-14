package com.daria.kotlinbase.domain.usecases

import com.daria.kotlinbase.domain.abstractions.ProductRepository
import com.daria.kotlinbase.domain.entities.Product
import com.daria.kotlinbase.shared.base.BaseUseCase
import com.daria.kotlinbase.shared.base.Result
import com.daria.kotlinbase.shared.utils.extensions.getParsedError
import kotlin.coroutines.cancellation.CancellationException

class GetProductDetailUseCase(
    private val repo: ProductRepository,
) : BaseUseCase<Int, Product>() {
    override suspend fun run(params: Int): Result<Product> =
        try {
            Result.Success(repo.getProduct(params))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.Error(e.getParsedError())
        }
}
