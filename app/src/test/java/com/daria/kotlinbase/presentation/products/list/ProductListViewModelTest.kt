package com.daria.kotlinbase.presentation.products.list

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.daria.kotlinbase.domain.entities.Product
import com.daria.kotlinbase.domain.usecases.GetProductsUseCase
import com.daria.kotlinbase.shared.base.BaseCommand
import com.daria.kotlinbase.shared.base.Result
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var sut: ProductListViewModel
    private lateinit var mockGetProductsUseCase: GetProductsUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockGetProductsUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun createSut() = ProductListViewModel(getProductsUseCase = mockGetProductsUseCase)

    @Test
    fun `init with success updates state products and clears loading`() =
        runTest {
            coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Success(mockProducts)

            sut = createSut()
            advanceUntilIdle()

            assertEquals(mockProducts, sut.state.value?.products)
            assertEquals(false, sut.state.value?.isLoading)
            assertEquals(false, sut.state.value?.isRefreshing)
            assertNull(sut.state.value?.errorMessage)
        }

    @Test
    fun `init with error updates state errorMessage`() =
        runTest {
            coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Error("Network error")

            sut = createSut()
            advanceUntilIdle()

            assertEquals("Network error", sut.state.value?.errorMessage)
            assertEquals(false, sut.state.value?.isLoading)
            assertEquals(false, sut.state.value?.isRefreshing)
            assertTrue(sut.state.value?.products?.isEmpty() == true)
        }

    @Test
    fun `onProductClicked emits PerformNavAction`() =
        runTest {
            coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Success(emptyList())
            sut = createSut()
            advanceUntilIdle()
            val emittedCommands = mutableListOf<BaseCommand>()
            sut.baseCmd.observeForever { emittedCommands.add(it) }

            sut.onProductClicked(productId = 42)

            assertEquals(1, emittedCommands.size)
            assertTrue(emittedCommands.first() is BaseCommand.PerformNavAction)
        }

    @Test
    fun `onRefresh with success clears errorMessage and updates products`() =
        runTest {
            coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Error("Error")
            sut = createSut()
            advanceUntilIdle()

            coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Success(mockProducts)
            sut.onRefresh()
            advanceUntilIdle()

            assertNull(sut.state.value?.errorMessage)
            assertEquals(mockProducts, sut.state.value?.products)
            assertEquals(false, sut.state.value?.isRefreshing)
        }

    @Test
    fun `onRetry calls use case again and recovers from error`() =
        runTest {
            coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Error("Error")
            sut = createSut()
            advanceUntilIdle()

            coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Success(mockProducts)
            sut.onRetry()
            advanceUntilIdle()

            coVerify(atLeast = 2) { mockGetProductsUseCase.executeNow(Unit) }
            assertEquals(mockProducts, sut.state.value?.products)
            assertNull(sut.state.value?.errorMessage)
        }

    companion object {
        private val mockProducts =
            listOf(
                Product(
                    id = 1,
                    title = "Backpack",
                    price = 42.5,
                    description = "Spacious",
                    category = "men's clothing",
                    imageUrl = "https://example.com/img.jpg",
                    rating = 4.5,
                    ratingCount = 120,
                ),
            )
    }
}
