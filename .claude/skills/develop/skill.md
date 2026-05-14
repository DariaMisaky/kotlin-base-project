---
name: develop
description: Implement Android features following Clean Architecture + MVVM patterns
allowed-tools: Read, Grep, Glob, Edit, Write, Bash, Task
---

# Android Feature Development Guide

When implementing features, follow these patterns strictly.

## Code Style
- Never add unnecessary comments — code should be self-explanatory
- Use descriptive method and variable names
- KtLint disable comments are acceptable only for justified exceptions
- Follow existing code patterns in the codebase

## Architecture Layers

### 1. UI Layer (MVVM with Fragments + XML DataBinding)

This project uses XML DataBinding + Fragments exclusively (no Compose).

**ViewModels:**
- Extend `BaseViewModel`
- Constructor injection via Koin (no annotations)
- Use `LiveData<UiState>` for UI state, `LiveEvent` for commands
- Inject Use Cases only (never Repositories or APIs directly)
- Use `executeNow()` to call use cases
- Fine-grained state: `viewModelScope.launch` + `UiState.copy()` for `isLoading` vs `isRefreshing`
- Coarse state: `performApiCall { }` when a single loading flag is sufficient

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

    fun onRefresh() = loadProducts(refresh = true)

    private fun loadProducts(refresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value?.copy(
                isLoading = !refresh,
                isRefreshing = refresh,
                errorMessage = null,
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

**UiState:**
```kotlin
data class ProductListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null,
)
```

**Fragments:**
- Extend `BaseFragment<BINDING, VIEW_MODEL>`
- Use XML DataBinding with `fragment_<feature>.xml` layout
- Set `binding.viewModel = viewModel` in `onViewCreated`
- Configure RecyclerView layoutManager and adapter in `onViewCreated`
- All visibility/click wiring goes in XML (thin Fragment, fat XML)

```kotlin
class ProductListFragment :
    BaseFragment<FragmentProductListBinding, ProductListViewModel>(R.layout.fragment_product_list) {

    override val viewModel: ProductListViewModel by viewModel()

    private val productsAdapter by lazy {
        ProductsAdapter(onProductClick = viewModel::onProductClicked)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.productsRecycler.layoutManager = GridLayoutManager(context, 2)
        binding.productsRecycler.adapter = productsAdapter
    }
}
```

### 2. Domain Layer (Use Cases + Entities)

**Use Cases:**
- One Use Case per business operation
- Extend `BaseUseCase<P, R>` — located in `domain/usecases/` (flat, no subfolders)
- Error wrapping inside `run()` — catch `CancellationException` first and rethrow, then catch `Throwable`
- Inject Repository interface only

```kotlin
class GetProductsUseCase(
    private val repo: ProductRepository,  // interface, not impl
) : BaseUseCase<Unit, List<Product>>() {
    override suspend fun run(params: Unit): Result<List<Product>> =
        try {
            Result.Success(repo.getProducts())
        } catch (e: CancellationException) {
            throw e  // REQUIRED
        } catch (e: Throwable) {
            Result.Error(e.getParsedError())
        }
}
```

**Domain Entities:**
- Pure Kotlin in `domain/entities/`
- No Android imports, no `@SerializedName`, no Parcelable

### 3. Data Layer (Repositories, APIs, DTOs, Mappers)

**Repositories:**
- Interface in `domain/abstractions/<Feature>Repository.kt` (returns domain types)
- Implementation in `data/repositories/<Feature>RepositoryImpl.kt` (applies mapper)
- No Result wrapping — exceptions propagate to Use Cases

```kotlin
// domain/abstractions/ProductRepository.kt
interface ProductRepository {
    suspend fun getProducts(): List<Product>
    suspend fun getProduct(id: Int): Product
}

// data/repositories/ProductRepositoryImpl.kt
class ProductRepositoryImpl(
    private val api: ProductApi,
) : ProductRepository {
    override suspend fun getProducts(): List<Product> =
        api.getProducts().map { it.toDomain() }

    override suspend fun getProduct(id: Int): Product =
        api.getProduct(id).toDomain()
}
```

**APIs:**
- Retrofit interfaces with suspend functions returning DTOs
- Paths are bare (no `v1/` prefix)
- Located in `data/remote/api/`

**DTOs:**
- `@SerializedName` on every field, located in `data/remote/dto/`
- Never used in domain or UI layers

**Mappers:**
- Extension functions `fun XxxDto.toDomain(): Xxx` in `data/mappers/<Feature>Mapper.kt`

## Dependency Injection (Koin)

Register all new components in `AppModules.kt`:

```kotlin
// apiModule - only if new API interface
single { ApiProvider.provideOrderApi() }

// repoModule - interface binding (NOT concrete)
single<OrderRepository> { OrderRepositoryImpl(get()) }

// useCases module
single { GetOrdersUseCase(get()) }

// viewModels module
viewModel { OrderListViewModel(get()) }

// viewModels with runtime params
viewModel { (orderId: Int) -> OrderDetailViewModel(orderId, get()) }
```

## Navigation

- Use SafeArgs for fragment navigation
- Dispatch via `_baseCmd.value = BaseCommand.PerformNavAction(directions)`
- Define actions in `res/navigation/main_navigation.xml`
- Use `BaseCommand.GoBack` for back navigation
- `BaseCommand.ShowError`, `ShowToast`, `ShowSnackbar` for messages

## DataBinding

- All layouts wrapped in `<layout>` with `<data><variable name="viewModel" .../></data>`
- Use `app:isVisible`, `app:isRefreshing`, `app:items`, `app:imageUrl`, `app:onNavigationClick` binding adapters
- Click handlers in XML: `android:onClick="@{() -> viewModel.onAction()}"`

## Localization

- Use `@string/key` for all user-facing strings in XML
- Use `getString(R.string.key)` in Kotlin code
- Never hardcode strings (English only — no `values-de/`)

## After Implementation

Run `/finish` to validate code quality before completing.

## Checklist

Before marking complete, ensure:
- [ ] ViewModel extends `BaseViewModel` (no Hilt annotations — Koin only)
- [ ] ViewModel injects Use Cases only (not Repository or API)
- [ ] UiState data class defined with default values for all fields
- [ ] `viewModelScope.launch` + `copy()` used for fine-grained state (isLoading vs isRefreshing)
- [ ] Use Case extends `BaseUseCase<P, R>` in `domain/usecases/` (flat)
- [ ] Use Case catches `CancellationException` and rethrows before catching `Throwable`
- [ ] Repository interface in `domain/abstractions/`, impl in `data/repositories/`
- [ ] DTO in `data/remote/dto/`, domain entity in `domain/entities/`
- [ ] Mapper extension `fun XxxDto.toDomain()` in `data/mappers/`
- [ ] All components registered in `AppModules.kt` (Koin)
- [ ] Repository bound as `single<Interface> { Impl(get()) }`
- [ ] All strings in `values/strings.xml` (English only)
- [ ] Navigation uses SafeArgs and BaseCommand via `_baseCmd`
- [ ] Layouts use `<layout>` wrapper with DataBinding
- [ ] Tests written for ViewModel, Use Case, Repository, Mapper
