# Repository Rules

## Repository Pattern

Repositories follow the **interface + implementation** split. The interface lives in `domain/abstractions/` and returns plain domain types. The implementation lives in `data/repositories/` and injects the API, converting DTOs to domain entities via mapper extensions.

## Repository Structure

### Interface (domain layer)

```kotlin
// app/src/main/java/com/daria/kotlinbase/domain/abstractions/ProductRepository.kt
interface ProductRepository {
    suspend fun getProducts(): List<Product>
    suspend fun getProduct(id: Int): Product
}
```

### Implementation (data layer)

```kotlin
// app/src/main/java/com/daria/kotlinbase/data/repositories/ProductRepositoryImpl.kt
class ProductRepositoryImpl(
    private val api: ProductApi,
) : ProductRepository {

    override suspend fun getProducts(): List<Product> =
        api.getProducts().map { it.toDomain() }

    override suspend fun getProduct(id: Int): Product =
        api.getProduct(id).toDomain()
}
```

## Repository Responsibilities

Repositories handle:
1. Delegating to API clients
2. Converting DTOs to domain entities via mapper extensions (`Dto.toDomain()`)

Repositories do NOT handle:
- Wrapping in `Result` (Use Cases do that)
- Error handling (exceptions propagate to Use Cases)
- Business logic (belongs in Use Cases)
- Coroutine context switching

```kotlin
// CORRECT - Delegates to API, maps DTO to domain
override suspend fun getProducts(): List<Product> =
    api.getProducts().map { it.toDomain() }

// CORRECT - Single item with mapper
override suspend fun getProduct(id: Int): Product =
    api.getProduct(id).toDomain()

// WRONG - Wrapping in Result (this is the Use Case's job)
override suspend fun getProducts(): Result<List<Product>> {
    return try {
        Result.Success(api.getProducts().map { it.toDomain() })
    } catch (e: Throwable) { ... }  // NO!
}
```

## Mapper Layer

DTOs live in `data/remote/dto/` and domain entities live in `domain/entities/`. They are always separate — never reuse a DTO as a domain type.

Mappers are extension functions in `data/mappers/<Feature>Mapper.kt`:

```kotlin
// app/src/main/java/com/daria/kotlinbase/data/mappers/ProductMapper.kt
fun ProductDto.toDomain(): Product = Product(
    id = id,
    title = title,
    price = price,
    description = description,
    category = category,
    imageUrl = image,       // field rename
    rating = rating.rate,   // nested DTO flattened
    ratingCount = rating.count,
)
```

Key mapper rules:
- One extension function per DTO: `fun XxxDto.toDomain(): Xxx`
- Mappers live in `data/mappers/` — NOT in the DTO or entity files
- Domain entities in `domain/entities/` are pure Kotlin — no Android or Retrofit imports
- DTOs in `data/remote/dto/` use `@SerializedName` and match the JSON shape

## No Shared Models

```kotlin
// CORRECT - Separate DTO and domain entity
data class ProductDto(
    @SerializedName("image") val image: String,  // matches JSON key
    ...
)

data class Product(
    val imageUrl: String,  // domain-friendly name
    ...
)

// WRONG - Using DTO directly in domain/UI layers
val product: ProductDto = repo.getProduct(id)  // NO! Use the domain entity
```

## Interface + Implementation (NOT concrete-only)

```kotlin
// CORRECT - Interface in domain/abstractions/
interface ProductRepository {
    suspend fun getProducts(): List<Product>
}

// CORRECT - Implementation in data/repositories/
class ProductRepositoryImpl(private val api: ProductApi) : ProductRepository

// WRONG - No interface, concrete class only
class ProductRepo(val api: ProductAPI) {  // NO! This project uses interface+impl
    suspend fun getProducts() = api.getProducts()
}
```

## Dependency Injection (Koin)

Repositories are bound as **interface → implementation** singletons in `AppModules.kt`:

```kotlin
// In AppModules.kt
private val repoModule = module {
    single<ProductRepository> { ProductRepositoryImpl(get()) }
}
```

Note the `single<Interface> { Impl(get()) }` form — this is the **opposite** of a concrete-only binding. Use cases inject `ProductRepository` (the interface), not `ProductRepositoryImpl`.

## File Locations

```
app/src/main/java/com/daria/kotlinbase/
├── domain/
│   ├── abstractions/
│   │   └── ProductRepository.kt     # Interface
│   └── entities/
│       └── Product.kt               # Pure Kotlin domain entity
└── data/
    ├── repositories/
    │   └── ProductRepositoryImpl.kt # Implementation
    ├── mappers/
    │   └── ProductMapper.kt         # fun ProductDto.toDomain(): Product
    └── remote/
        └── dto/
            └── ProductDto.kt        # @SerializedName data class
```

## Method Naming Conventions

| Action | Method Pattern | Example |
|--------|---------------|---------|
| Get list | `get<Resources>()` | `getProducts()` |
| Get single | `get<Resource>(id)` | `getProduct(id)` |
| Create/Submit | `create<Resource>(request)` | `createOrder(body)` |
| Update | `update<Resource>(id, request)` | `updateProfile(body)` |
| Delete | `delete<Resource>(id)` | `deleteDevice(id)` |

## Naming Conventions Summary

| Element | Convention | Example |
|---------|------------|---------|
| Repository interface | `<Feature>Repository` | `ProductRepository` |
| Repository implementation | `<Feature>RepositoryImpl` | `ProductRepositoryImpl` |
| Interface location | `domain/abstractions/` | - |
| Implementation location | `data/repositories/` | - |
| Mapper file | `<Feature>Mapper.kt` | `ProductMapper.kt` |
| Mapper function | `<Dto>.toDomain()` | `ProductDto.toDomain()` |
| Domain entity location | `domain/entities/` | - |
| DTO location | `data/remote/dto/` | - |
