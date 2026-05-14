package com.daria.kotlinbase.data.repositories

import com.daria.kotlinbase.data.remote.api.ProductApi
import com.daria.kotlinbase.data.remote.dto.ProductDto
import com.daria.kotlinbase.data.remote.dto.RatingDto
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryImplTest {
    private lateinit var sut: ProductRepositoryImpl
    private lateinit var mockApi: ProductApi

    @Before
    fun setUp() {
        mockApi = mockk()
        sut = ProductRepositoryImpl(api = mockApi)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getProducts calls api`() =
        runTest {
            coEvery { mockApi.getProducts() } returns mockDtos

            sut.getProducts()

            coVerify(exactly = 1) { mockApi.getProducts() }
        }

    @Test
    fun `getProducts maps dtos to domain entities`() =
        runTest {
            coEvery { mockApi.getProducts() } returns mockDtos

            val result = sut.getProducts()

            assertEquals(mockDtos.size, result.size)
            val first = result.first()
            val firstDto = mockDtos.first()
            assertEquals(firstDto.id, first.id)
            assertEquals(firstDto.title, first.title)
            assertEquals(firstDto.image, first.imageUrl)
            assertEquals(firstDto.rating.rate, first.rating, 0.0)
            assertEquals(firstDto.rating.count, first.ratingCount)
        }

    @Test(expected = IOException::class)
    fun `getProducts with api error propagates exception`() =
        runTest {
            coEvery { mockApi.getProducts() } throws IOException("Network error")

            sut.getProducts()
        }

    @Test
    fun `getProduct calls api with id`() =
        runTest {
            coEvery { mockApi.getProduct(TEST_PRODUCT_ID) } returns mockDtos.first()

            sut.getProduct(TEST_PRODUCT_ID)

            coVerify(exactly = 1) { mockApi.getProduct(TEST_PRODUCT_ID) }
        }

    @Test
    fun `getProduct maps dto to domain entity`() =
        runTest {
            val dto = mockDtos.first()
            coEvery { mockApi.getProduct(TEST_PRODUCT_ID) } returns dto

            val result = sut.getProduct(TEST_PRODUCT_ID)

            assertEquals(dto.id, result.id)
            assertEquals(dto.image, result.imageUrl)
            assertEquals(dto.rating.rate, result.rating, 0.0)
            assertEquals(dto.rating.count, result.ratingCount)
        }

    @Test(expected = IOException::class)
    fun `getProduct with api error propagates exception`() =
        runTest {
            coEvery { mockApi.getProduct(TEST_PRODUCT_ID) } throws IOException("Not found")

            sut.getProduct(TEST_PRODUCT_ID)
        }

    companion object {
        private const val TEST_PRODUCT_ID = 7
        private val mockDtos =
            listOf(
                ProductDto(
                    id = TEST_PRODUCT_ID,
                    title = "Backpack",
                    price = 42.5,
                    description = "Spacious",
                    category = "men's clothing",
                    image = "https://example.com/img.jpg",
                    rating = RatingDto(rate = 4.5, count = 120),
                ),
            )
    }
}
