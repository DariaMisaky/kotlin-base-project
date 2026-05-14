package com.daria.kotlinbase.domain.usecases

import com.daria.kotlinbase.domain.abstractions.ProductRepository
import com.daria.kotlinbase.domain.entities.Product
import com.daria.kotlinbase.shared.base.BaseUseCase
import com.daria.kotlinbase.shared.base.Result
import com.daria.kotlinbase.shared.utils.extensions.getParsedError
import kotlin.coroutines.cancellation.CancellationException

class GetProductsUseCase(
    private val repo: ProductRepository,
) : BaseUseCase<Unit, List<Product>>() {
    override suspend fun run(params: Unit): Result<List<Product>> =
        try {
            Result.Success(repo.getProducts())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.Error(e.getParsedError())
        }
}
