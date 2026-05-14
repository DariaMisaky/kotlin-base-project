package com.daria.kotlinbase.data.mappers

import com.daria.kotlinbase.data.remote.dto.ProductDto
import com.daria.kotlinbase.domain.entities.Product

fun ProductDto.toDomain(): Product = Product(
    id = id,
    title = title,
    price = price,
    description = description,
    category = category,
    imageUrl = image,
    rating = rating.rate,
    ratingCount = rating.count,
)
