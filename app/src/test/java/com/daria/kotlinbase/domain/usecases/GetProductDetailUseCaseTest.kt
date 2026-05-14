package com.daria.kotlinbase.domain.usecases

import com.daria.kotlinbase.domain.abstractions.ProductRepository
import com.daria.kotlinbase.domain.entities.Product
import com.daria.kotlinbase.shared.base.Result
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GetProductDetailUseCaseTest {
    private lateinit var sut: GetProductDetailUseCase
    private lateinit var mockRepo: ProductRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk()
        sut = GetProductDetailUseCase(repo = mockRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `executeNow calls repository with id`() =
        runTest {
            coEvery { mockRepo.getProduct(TEST_PRODUCT_ID) } returns mockProduct

            sut.executeNow(TEST_PRODUCT_ID)

            coVerify(exactly = 1) { mockRepo.getProduct(TEST_PRODUCT_ID) }
        }

    @Test
    fun `executeNow with success returns Result Success`() =
        runTest {
            coEvery { mockRepo.getProduct(TEST_PRODUCT_ID) } returns mockProduct

            val result = sut.executeNow(TEST_PRODUCT_ID)

            assertTrue(result is Result.Success)
            assertEquals(mockProduct, (result as Result.Success).data)
        }

    @Test
    fun `executeNow with exception returns Result Error`() =
        runTest {
            coEvery { mockRepo.getProduct(TEST_PRODUCT_ID) } throws IOException("Not found")

            val result = sut.executeNow(TEST_PRODUCT_ID)

            assertTrue(result is Result.Error)
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
