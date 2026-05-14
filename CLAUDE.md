# Kotlin Base Android Project

Android XML+DataBinding base project with Clean Architecture + MVVM. One sample feature (Products from the FakeStore API at `https://fakestoreapi.com/`) demonstrates the patterns end-to-end.

## Project Structure

Single-module, type-first package structure:

```
app/src/main/java/com/daria/kotlinbase/
├── application/           # KotlinBaseApp, Koin DI modules (AppModules.kt)
├── data/                  # Data layer: remote API, DTOs, mappers, repository impls
│   ├── mappers/           # fun XxxDto.toDomain() extension functions
│   ├── remote/
│   │   ├── api/
│   │   │   ├── common/    # ApiProvider.kt, AuthenticationInterceptor.kt
│   │   │   └── ProductApi.kt
│   │   └── dto/           # @SerializedName data classes (JSON shape)
│   └── repositories/      # <Feature>RepositoryImpl — injects API, applies mapper
├── domain/                # Pure Kotlin: no Android imports
│   ├── abstractions/      # <Feature>Repository interfaces
│   ├── entities/          # Domain data classes (Product, etc.)
│   └── usecases/          # <Action><Resource>UseCase (flat, no subfolders)
├── presentation/          # UI layer: Activities, Fragments, ViewModels
│   ├── MainActivity.kt
│   └── products/
│       ├── list/          # ProductListFragment, ProductListViewModel, ProductsAdapter, ProductListUiState
│       └── detail/        # ProductDetailFragment, ProductDetailViewModel, ProductDetailUiState
└── shared/
    ├── base/              # BaseFragment, BaseViewModel, BaseUseCase, Result, BaseCommand, ApiError
    └── utils/
        ├── bindingadapters/   # ViewBindingAdapters.kt, ImageBindingAdapters.kt
        ├── extensions/        # Throwable.getParsedError()
        └── LiveEvent.kt       # Single-fire event wrapper
```

**Dependency rule:** `presentation → domain ← data`. Domain has no Android imports. Presentation never imports DTOs.

## Architecture

**MVVM + Repository Pattern with interface+impl split and mapper layer**

Each feature follows:
```
Feature/
├── presentation/products/<feature>/
│   ├── ProductListFragment.kt        # Thin fragment: sets binding.viewModel, adapter, layoutManager
│   ├── ProductListViewModel.kt       # Extends BaseViewModel, injects UseCases, exposes LiveData<UiState>
│   ├── ProductListUiState.kt         # Data class with default values (isLoading, isRefreshing, data, error)
│   └── ProductsAdapter.kt            # ListAdapter<Product, VH> for RecyclerView
├── domain/
│   ├── abstractions/ProductRepository.kt   # Interface — returns domain types
│   ├── entities/Product.kt                  # Pure Kotlin domain entity
│   └── usecases/GetProductsUseCase.kt       # Extends BaseUseCase, injects Repository interface
└── data/
    ├── repositories/ProductRepositoryImpl.kt  # Implements interface, applies mapper
    ├── mappers/ProductMapper.kt               # fun ProductDto.toDomain(): Product
    └── remote/
        ├── api/ProductApi.kt                  # Retrofit interface — returns DTOs
        └── dto/ProductDto.kt                  # @SerializedName data class
```

## Tech Stack

| Component | Library | Version |
|-----------|---------|---------|
| Language | Kotlin | 2.2.20 |
| UI | XML Layouts + DataBinding | - |
| DI | Koin | 4.1.1 |
| Networking | Retrofit | 3.0.0 |
| HTTP Client | OkHttp BOM | 5.2.0 |
| JSON | Gson | 2.13.2 |
| Async | Coroutines | 1.10.2 |
| Navigation | Fragment Navigation + SafeArgs | 2.9.5 |
| Images | Coil | 3.0.4 |
| Storage | DataStore Preferences | 1.1.7 (wired, no usage yet) |
| Theme | Material 3 | 1.12.0 |
| Tests | JUnit | 4.13.2 |
| Mocking | MockK | 1.14.9 |
| Code Quality | KtLint | (via Gradle plugin) |

## Build Configuration

```
Compile SDK: 36
Target SDK: 36
Min SDK: 26
Kotlin: 2.2.20
Java: 17
AGP: 8.13.0
Gradle: 8.14.3
```

**No build flavors.** Single `debug`/`release` build type. Base URL is set via `buildConfigField` in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "BASE_URL", "\"https://fakestoreapi.com/\"")
```

## Key Patterns

### Result Wrapper

All Use Case operations return `Result<T>`:

```kotlin
sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val error: String?, var errorCode: Int = -1) : Result<Nothing>()
}
```

### ViewModel Pattern

```kotlin
class ProductListViewModel(
    private val getProductsUseCase: GetProductsUseCase,
) : BaseViewModel() {

    private val _state = MutableLiveData(ProductListUiState())
    val state: LiveData<ProductListUiState> = _state

    init { loadProducts() }

    fun onProductClicked(productId: Int) {
        _baseCmd.value = BaseCommand.PerformNavAction(
            MainNavigationDirections.actionGlobalProductDetail(productId)
        )
    }

    private fun loadProducts(refresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value?.copy(
                isLoading = !refresh, isRefreshing = refresh, errorMessage = null,
            )
            when (val result = getProductsUseCase.executeNow(Unit)) {
                is Result.Success -> _state.value = _state.value?.copy(
                    isLoading = false, isRefreshing = false, products = result.data,
                )
                is Result.Error -> _state.value = _state.value?.copy(
                    isLoading = false, isRefreshing = false, errorMessage = result.error,
                )
            }
        }
    }
}
```

Also available: `performApiCall { }` from `BaseViewModel` for simpler coarse loading-flag scenarios.

### Use Case Pattern

```kotlin
class GetProductsUseCase(
    private val repo: ProductRepository,  // interface
) : BaseUseCase<Unit, List<Product>>() {
    override suspend fun run(params: Unit): Result<List<Product>> =
        try {
            Result.Success(repo.getProducts())
        } catch (e: CancellationException) {
            throw e  // REQUIRED: preserve structured concurrency
        } catch (e: Throwable) {
            Result.Error(e.getParsedError())
        }
}
```

### Repository Pattern

Interface in domain, implementation in data:

```kotlin
// domain/abstractions/ProductRepository.kt
interface ProductRepository {
    suspend fun getProducts(): List<Product>
    suspend fun getProduct(id: Int): Product
}

// data/repositories/ProductRepositoryImpl.kt
class ProductRepositoryImpl(private val api: ProductApi) : ProductRepository {
    override suspend fun getProducts(): List<Product> =
        api.getProducts().map { it.toDomain() }

    override suspend fun getProduct(id: Int): Product =
        api.getProduct(id).toDomain()
}
```

### Mapper Pattern

```kotlin
// data/mappers/ProductMapper.kt
fun ProductDto.toDomain(): Product = Product(
    id = id,
    title = title,
    price = price,
    description = description,
    category = category,
    imageUrl = image,         // field rename
    rating = rating.rate,     // nested DTO flattened
    ratingCount = rating.count,
)
```

### ViewModel Hierarchy

```
ViewModel
  └── BaseViewModel (_baseCmd, isLoading, performApiCall)
        └── FeatureViewModel
```

### BaseCommand Types

```kotlin
sealed class BaseCommand {
    data class PerformNavAction(val navAction: NavDirections) : BaseCommand()
    data object GoBack : BaseCommand()
    data class ShowToast(val message: String) : BaseCommand()
    data class ShowSnackbar(val message: String) : BaseCommand()
    data class ShowError(val message: String?) : BaseCommand()
}
```

Dispatched via `_baseCmd.value = BaseCommand.Xxx(...)`. Observed and handled automatically by `BaseFragment`.

### Dependency Hierarchy

```
ViewModel → UseCase → Repository (interface) → API
```

## Common Commands

```bash
# Build
./gradlew assembleDebug

# Install on device/emulator
./gradlew installDebug

# Code formatting (auto-fix)
./gradlew ktlintFormat

# Code style check
./gradlew ktlintCheck

# Tests
./gradlew testDebugUnitTest

# Android Lint
./gradlew lintDebug

# Clean
./gradlew clean
```

## Main Features

- **Products List**: Fetches products from FakeStore API, displays in a 2-column grid with SwipeRefreshLayout. Demonstrates UiState with `isLoading` + `isRefreshing` + `errorMessage`.
- **Product Detail**: Shows product image (Coil), title, price, description, category, rating. Receives `productId` via SafeArgs.

## Key Directories

- `app/src/main/java/.../application/` — `KotlinBaseApp`, `AppModules.kt` (all Koin registrations)
- `app/src/main/java/.../data/remote/api/common/` — `ApiProvider.kt`, `AuthenticationInterceptor.kt`
- `app/src/main/java/.../data/remote/api/` — `ProductApi.kt` (Retrofit interface)
- `app/src/main/java/.../data/remote/dto/` — `ProductDto.kt`, `RatingDto.kt`
- `app/src/main/java/.../data/mappers/` — `ProductMapper.kt`
- `app/src/main/java/.../data/repositories/` — `ProductRepositoryImpl.kt`
- `app/src/main/java/.../domain/abstractions/` — `ProductRepository.kt` (interface)
- `app/src/main/java/.../domain/entities/` — `Product.kt`
- `app/src/main/java/.../domain/usecases/` — `GetProductsUseCase.kt`, `GetProductDetailUseCase.kt`
- `app/src/main/java/.../presentation/products/` — Fragments, ViewModels, UiStates, Adapter
- `app/src/main/java/.../shared/base/` — `BaseFragment`, `BaseViewModel`, `BaseUseCase`, `Result`, `BaseCommand`, `ApiError`
- `app/src/main/java/.../shared/utils/bindingadapters/` — `ViewBindingAdapters.kt`, `ImageBindingAdapters.kt`
- `app/src/main/res/navigation/` — `main_navigation.xml`

## Navigation Graphs

```
app/src/main/res/navigation/
└── main_navigation.xml    # Products list → product detail (productId: Int arg)
```

## Test Organization

Tests mirror the production package structure:

```
app/src/test/java/com/daria/kotlinbase/
├── data/
│   ├── mappers/
│   │   └── ProductMapperTest.kt
│   └── repositories/
│       └── ProductRepositoryImplTest.kt
└── presentation/
    └── products/
        ├── list/
        │   └── ProductListViewModelTest.kt
        └── detail/
            └── ProductDetailViewModelTest.kt
```

## Naming Conventions

| Element | Pattern | Example |
|---------|---------|---------|
| ViewModel | `<Feature>ViewModel` | `ProductListViewModel` |
| UiState | `<Feature>UiState` | `ProductListUiState` |
| Fragment | `<Feature>Fragment` | `ProductListFragment` |
| Use Case | `<Action><Resource>UseCase` | `GetProductsUseCase` |
| Repository interface | `<Feature>Repository` | `ProductRepository` |
| Repository impl | `<Feature>RepositoryImpl` | `ProductRepositoryImpl` |
| API interface | `<Feature>Api` | `ProductApi` |
| DTO | `<Resource>Dto` | `ProductDto`, `RatingDto` |
| Domain entity | `<Resource>` | `Product` |
| Mapper function | `<Dto>.toDomain()` | `ProductDto.toDomain()` |
| Layout | `fragment_<feature>.xml` | `fragment_product_list.xml` |
| Navigation Graph | `<feature>_navigation.xml` | `main_navigation.xml` |

## DI Registration (AppModules.kt)

```kotlin
object AppModules {
    private val apiModule = module {
        single { ApiProvider.provideProductApi() }
    }

    private val repoModule = module {
        single<ProductRepository> { ProductRepositoryImpl(get()) }  // interface binding
    }

    private val useCases = module {
        single { GetProductsUseCase(get()) }
        single { GetProductDetailUseCase(get()) }
    }

    private val viewModels = module {
        viewModel { ProductListViewModel(get()) }
        viewModel { (productId: Int) -> ProductDetailViewModel(productId, get()) }
    }

    val modules = listOf(apiModule, repoModule, useCases, viewModels)
}
```

Key difference from concrete-repo projects: repositories are bound as `single<Interface> { Impl(get()) }`.

## API Interfaces

APIs use bare paths (no `v1/` prefix). Base URL (`https://fakestoreapi.com/`) is set in `BuildConfig.BASE_URL`.

```kotlin
interface ProductApi {
    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDto
}
```

## Interceptors

Currently only one custom interceptor:

- **`AuthenticationInterceptor`** (stub) — adds `Accept: application/json` header. Uncomment the `Authorization` header line when auth is added.

All interceptors live in `data/remote/api/common/`.
