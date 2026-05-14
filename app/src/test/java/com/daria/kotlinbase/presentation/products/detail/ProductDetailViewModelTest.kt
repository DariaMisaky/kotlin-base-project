package com.daria.kotlinbase.presentation.products.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.daria.kotlinbase.domain.entities.Product
import com.daria.kotlinbase.domain.usecases.GetProductDetailUseCase
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
class ProductDetailViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var sut: ProductDetailViewModel
    private lateinit var mockGetProductDetailUseCase: GetProductDetailUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockGetProductDetailUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun createSut(productId: Int = TEST_PRODUCT_ID) =
        ProductDetailViewModel(
            productId = productId,
            getProductDetailUseCase = mockGetProductDetailUseCase,
        )

    @Test
    fun `init with success updates state product and clears loading`() =
        runTest {
            coEvery { mockGetProductDetailUseCase.executeNow(TEST_PRODUCT_ID) } returns Result.Success(mockProduct)

            sut = createSut()
            advanceUntilIdle()

            assertEquals(mockProduct, sut.state.value?.product)
            assertEquals(false, sut.state.value?.isLoading)
            assertNull(sut.state.value?.errorMessage)
        }

    @Test
    fun `init with error updates state errorMessage`() =
        runTest {
            coEvery { mockGetProductDetailUseCase.executeNow(TEST_PRODUCT_ID) } returns Result.Error("Not found")

            sut = createSut()
            advanceUntilIdle()

            assertEquals("Not found", sut.state.value?.errorMessage)
            assertEquals(false, sut.state.value?.isLoading)
            assertNull(sut.state.value?.product)
        }

    @Test
    fun `init passes productId to use case`() =
        runTest {
            coEvery { mockGetProductDetailUseCase.executeNow(any()) } returns Result.Success(mockProduct)

            sut = createSut(productId = 99)
            advanceUntilIdle()

            coVerify(exactly = 1) { mockGetProductDetailUseCase.executeNow(99) }
        }

    @Test
    fun `onBack emits GoBack command`() =
        runTest {
            coEvery { mockGetProductDetailUseCase.executeNow(TEST_PRODUCT_ID) } returns Result.Success(mockProduct)
            sut = createSut()
            advanceUntilIdle()
            val emittedCommands = mutableListOf<BaseCommand>()
            sut.baseCmd.observeForever { emittedCommands.add(it) }

            sut.onBack()

            assertEquals(1, emittedCommands.size)
            assertTrue(emittedCommands.first() is BaseCommand.GoBack)
        }

    @Test
    fun `onRetry calls use case again and recovers from error`() =
        runTest {
            coEvery { mockGetProductDetailUseCase.executeNow(TEST_PRODUCT_ID) } returns Result.Error("Error")
            sut = createSut()
            advanceUntilIdle()

            coEvery { mockGetProductDetailUseCase.executeNow(TEST_PRODUCT_ID) } returns Result.Success(mockProduct)
            sut.onRetry()
            advanceUntilIdle()

            coVerify(atLeast = 2) { mockGetProductDetailUseCase.executeNow(TEST_PRODUCT_ID) }
            assertEquals(mockProduct, sut.state.value?.product)
            assertNull(sut.state.value?.errorMessage)
        }

    companion object {
        private const val TEST_PRODUCT_ID = 7
        private val mockProduct =
            Product(
                id = TEST_PRODUCT_ID,
                title = "Backpack",
                price = 42.5,
                description = "Spacious",
                category = "men's clothing",
                imageUrl = "https://example.com/img.jpg",
                rating = 4.5,
                ratingCount = 120,
            )
    }
}
