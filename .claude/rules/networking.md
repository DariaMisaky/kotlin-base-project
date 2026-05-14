# Networking Rules

## Architecture Overview

Networking follows a layered pattern:
1. **ApiProvider** - Singleton object providing Retrofit API instances (`data/remote/api/common/ApiProvider.kt`)
2. **API Interface** - Retrofit interface defining endpoints (`data/remote/api/`)
3. **DTOs** - Data classes with `@SerializedName` that match the JSON shape (`data/remote/dto/`)
4. **Mappers** - Extension functions converting DTOs to domain entities (`data/mappers/`)
5. **Repository** - Delegates to API and maps result to domain types

## ApiProvider

Networking is configured in a singleton `object ApiProvider`:

```kotlin
// app/src/main/java/com/daria/kotlinbase/data/remote/api/common/ApiProvider.kt
object ApiProvider {

    private const val TIMEOUT_SECONDS = 30L

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor: HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    private val authInterceptor = AuthenticationInterceptor()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    fun provideProductApi(): ProductApi = retrofit.create(ProductApi::class.java)

    // When adding a new API: add a new fun provideXxxApi() here
}
```

Key points:
- `BASE_URL` comes from `BuildConfig.BASE_URL` (set as `buildConfigField` in `app/build.gradle.kts`)
- Logging interceptor is `BODY` level in debug builds only, `NONE` in release
- 30-second timeouts for connect, read, and write (use `TIMEOUT_SECONDS` constant — don't hardcode)
- A single `AuthenticationInterceptor` stub is currently the only custom interceptor
- Adding a new feature API: add one `fun provideXxxApi()` function to `ApiProvider`

## Retrofit API Interface

```kotlin
// app/src/main/java/com/daria/kotlinbase/data/remote/api/ProductApi.kt
interface ProductApi {

    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDto
}
```

Key patterns:
- All methods are `suspend` functions
- Return **DTOs**, not domain entities (the repository impl applies the mapper)
- Paths are bare (no `v1/` prefix) — base URL from `BuildConfig.BASE_URL` includes the API root
- Name the interface `<Feature>API` (e.g., `OrderApi`, `UserApi`)

## DTOs

DTOs live in `data/remote/dto/` and use Gson `@SerializedName`. They reflect the exact JSON shape:

```kotlin
// app/src/main/java/com/daria/kotlinbase/data/remote/dto/ProductDto.kt
data class ProductDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("price") val price: Double,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("image") val image: String,      // key differs from domain field name
    @SerializedName("rating") val rating: RatingDto,  // nested DTO
)

data class RatingDto(
    @SerializedName("rate") val rate: Double,
    @SerializedName("count") val count: Int,
)
```

DTO rules:
- Use `@SerializedName` on every field — never rely on field name matching
- Nest DTOs for nested JSON objects (e.g., `RatingDto` inside `ProductDto`)
- DTOs do NOT implement `Parcelable` — convert to domain entity before passing between screens
- Name DTOs `<Resource>Dto` (e.g., `ProductDto`, `OrderDto`)

## Mapper Layer

Mappers convert DTOs to domain entities in `data/mappers/`:

```kotlin
// app/src/main/java/com/daria/kotlinbase/data/mappers/ProductMapper.kt
fun ProductDto.toDomain(): Product = Product(
    id = id,
    title = title,
    price = price,
    description = description,
    category = category,
    imageUrl = image,         // rename: JSON "image" → domain "imageUrl"
    rating = rating.rate,     // flatten nested DTO
    ratingCount = rating.count,
)
```

- One extension function per DTO: `fun XxxDto.toDomain(): Xxx`
- Repository calls `dto.toDomain()` or `list.map { it.toDomain() }` before returning

## Interceptors

Currently only one custom interceptor is present:

### AuthenticationInterceptor (stub)

```kotlin
// app/src/main/java/com/daria/kotlinbase/data/remote/api/common/AuthenticationInterceptor.kt
class AuthenticationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/json")
            // .header("Authorization", "Bearer $token")  // uncomment when auth is added
        return chain.proceed(builder.build())
    }
}
```

This interceptor is a stub — it adds the `Accept` header but does not yet inject a Bearer token. When auth is needed, inject the token provider here.

All custom interceptors live in `data/remote/api/common/`.

## File Organization

```
app/src/main/java/com/daria/kotlinbase/
└── data/
    ├── remote/
    │   ├── api/
    │   │   ├── common/
    │   │   │   ├── ApiProvider.kt
    │   │   │   └── AuthenticationInterceptor.kt
    │   │   └── ProductApi.kt
    │   └── dto/
    │       └── ProductDto.kt     # + RatingDto
    ├── mappers/
    │   └── ProductMapper.kt
    └── repositories/
        └── ProductRepositoryImpl.kt
```

## Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| API interface | `<Feature>Api` | `ProductApi` |
| DTO | `<Resource>Dto` | `ProductDto`, `RatingDto` |
| Domain entity | `<Resource>` | `Product` |
| Request body | `<Action>Body` or `<Action>Request` | `CreateOrderBody` |
| Interceptor | `<Purpose>Interceptor` | `AuthenticationInterceptor` |
| Mapper function | `<Dto>.toDomain()` | `ProductDto.toDomain()` |

## HTTP Methods

| Action | HTTP Method | Annotation |
|--------|-------------|------------|
| Get list | GET | `@GET("products")` |
| Get single | GET | `@GET("products/{id}")` with `@Path` |
| Create | POST | `@POST("orders")` with `@Body` |
| Full update | PUT | `@PUT("orders/{id}")` with `@Body` |
| Partial update | PATCH | `@PATCH("orders/{id}")` with `@Body` |
| Delete | DELETE | `@DELETE("orders/{id}")` |

## Koin Registration

APIs are registered in `AppModules.kt` via `ApiProvider`:

```kotlin
private val apiModule = module {
    single { ApiProvider.provideProductApi() }
    // single { ApiProvider.provideOrderApi() }  // add new APIs here
}
```
