package com.daria.kotlinbase.data.mappers

import com.daria.kotlinbase.data.remote.dto.ProductDto
import com.daria.kotlinbase.data.remote.dto.RatingDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductMapperTest {
    @Test
    fun `toDomain maps all fields including nested rating`() {
        val dto =
            ProductDto(
                id = 7,
                title = "Backpack",
                price = 42.5,
                description = "Spacious",
                category = "men's clothing",
                image = "https://example.com/img.jpg",
                rating = RatingDto(rate = 4.5, count = 120),
            )

        val product = dto.toDomain()

        assertEquals(7, product.id)
        assertEquals("Backpack", product.title)
        assertEquals(42.5, product.price, 0.0)
        assertEquals("https://example.com/img.jpg", product.imageUrl)
        assertEquals(4.5, product.rating, 0.0)
        assertEquals(120, product.ratingCount)
    }
}
