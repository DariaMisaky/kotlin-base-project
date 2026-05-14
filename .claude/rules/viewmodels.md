# ViewModel Rules

## All ViewModels Must Follow MVVM Pattern

ViewModels extend `BaseViewModel` and manage UI state via LiveData. This project uses XML DataBinding + Fragments exclusively (no Compose).

## ViewModel Pattern

The primary pattern uses a `UiState` data class and `viewModelScope.launch` for fine-grained state control (separate `isLoading` vs `isRefreshing` flags):

```kotlin
// app/src/main/java/com/daria/kotlinbase/presentation/products/list/ProductListViewModel.kt
class ProductListViewModel(
    private val getProductsUseCase: GetProductsUseCase,
) : BaseViewModel() {

    private val _state = MutableLiveData(ProductListUiState())
    val state: LiveData<ProductListUiState> = _state

    init {
        loadProducts()
    }

    fun onProductClicked(productId: Int) {
        _baseCmd.value = BaseCommand.PerformNavAction(
            MainNavigationDirections.actionGlobalProductDetail(productId)
        )
    }

    fun onRefresh() = loadProducts(refresh = true)

    fun onRetry() = loadProducts()

    private fun loadProducts(refresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value?.copy(
                isLoading = !refresh,
                isRefreshing = refresh,
                errorMessage = null,
            )
            when (val result = getProductsUseCase.executeNow(Unit)) {
                is Result.Success -> _state.value = _state.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    products = result.data,
                    errorMessage = null,
                )
                is Result.Error -> _state.value = _state.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = result.error,
                )
            }
        }
    }
}
```

## UiState Pattern

Each screen has a dedicated UiState data class:

```kotlin
// app/src/main/java/com/daria/kotlinbase/presentation/products/list/ProductListUiState.kt
data class ProductListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null,
)

// app/src/main/java/com/daria/kotlinbase/presentation/products/detail/ProductDetailUiState.kt
data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: Product? = null,
    val errorMessage: String? = null,
)
```

- Use `data class` with default values for all fields
- Use `copy()` to update individual fields (immutable updates)
- Expose as `LiveData<UiState>` (private `MutableLiveData`, public `LiveData`)

## Two Loading Patterns

### Pattern 1: Manual launch + state copy (preferred for fine-grained state)

Use when the screen distinguishes between initial load and pull-to-refresh:

```kotlin
private fun loadProducts(refresh: Boolean = false) {
    viewModelScope.launch {
        _state.value = _state.value?.copy(
            isLoading = !refresh,
            isRefreshing = refresh,
            errorMessage = null,
        )
        when (val result = getProductsUseCase.executeNow(Unit)) {
            is Result.Success -> _state.value = _state.value?.copy(
                isLoading = false,
                isRefreshing = false,
                products = result.data,
            )
            is Result.Error -> _state.value = _state.value?.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = result.error,
            )
        }
    }
}
```

### Pattern 2: performApiCall (for simple loading-flag scenarios)

`performApiCall` is available from `BaseViewModel` when a single loading flag is sufficient and you want automatic error routing to `ShowError`:

```kotlin
fun loadData() {
    performApiCall {  // sets isLoading = true, resets to false in finally
        when (val result = getProductsUseCase.executeNow(Unit)) {
            is Result.Success -> _items.value = result.data
            is Result.Error -> _baseCmd.value = BaseCommand.ShowError(result.error)
        }
    }
}
```

`performApiCall` does NOT distinguish loading vs refreshing — use Pattern 1 when that distinction matters.

## ViewModel Hierarchy

```
ViewModel
  └── BaseViewModel (loading flag, performApiCall, _baseCmd)
        └── FeatureViewModel
```

`BaseViewModel` provides:
- `_baseCmd: MutableLiveEvent<BaseCommand>` — dispatch navigation/toast/snackbar/error
- `baseCmd: LiveEvent<BaseCommand>` — observed by `BaseFragment`
- `isLoading: LiveData<Boolean>` — toggled by `performApiCall`
- `performApiCall(showLoading, block)` — wraps a coroutine with loading state + error routing

## Calling Use Cases

Use `executeNow()` and `when` pattern for result handling:

```kotlin
// CORRECT
when (val result = getProductsUseCase.executeNow(Unit)) {
    is Result.Success -> _state.value = _state.value?.copy(products = result.data)
    is Result.Error -> _state.value = _state.value?.copy(errorMessage = result.error)
}

// WRONG - Don't invoke use case with () operator
val result = getProductsUseCase(Unit)  // NO! Use executeNow()
```

## Navigation Patterns

Navigation uses `_baseCmd` (from `BaseViewModel`):

```kotlin
// Navigate with SafeArgs direction
fun onProductClicked(productId: Int) {
    _baseCmd.value = BaseCommand.PerformNavAction(
        MainNavigationDirections.actionGlobalProductDetail(productId)
    )
}

// Go back
fun onBack() {
    _baseCmd.value = BaseCommand.GoBack
}

// Show toast
fun onError(message: String) {
    _baseCmd.value = BaseCommand.ShowToast(message)
}

// Show snackbar
fun onSaved() {
    _baseCmd.value = BaseCommand.ShowSnackbar("Saved successfully")
}

// Show error (routes to snackbar via BaseFragment fallback)
fun onNetworkError(message: String?) {
    _baseCmd.value = BaseCommand.ShowError(message)
}
```

## Screen-Specific Commands

For one-shot events that are specific to a single screen, use a nested `sealed class Command` with a dedicated `LiveEvent`:

```kotlin
class ProductListViewModel(...) : BaseViewModel() {

    // Screen-specific commands (recommended pattern even if not yet in the codebase)
    private val _cmd = MutableLiveEvent<Command>()
    val cmd: LiveEvent<Command> = _cmd

    fun onScrollToTop() {
        _cmd.value = Command.ScrollToTop
    }

    sealed class Command {
        data object ScrollToTop : Command()
        data class ShareProduct(val url: String) : Command()
    }
}

// In Fragment:
viewModel.cmd.observe(viewLifecycleOwner) { command ->
    when (command) {
        is ProductListViewModel.Command.ScrollToTop -> recycler.scrollToPosition(0)
        is ProductListViewModel.Command.ShareProduct -> shareUrl(command.url)
    }
}
```

## ViewModel with Runtime Parameters

For ViewModels that need data passed at creation time (e.g., `productId`):

```kotlin
// ViewModel constructor
class ProductDetailViewModel(
    private val productId: Int,
    private val getProductDetailUseCase: GetProductDetailUseCase,
) : BaseViewModel()

// Koin registration
viewModel { (productId: Int) -> ProductDetailViewModel(productId, get()) }

// Fragment injection
override val viewModel: ProductDetailViewModel by viewModel {
    parametersOf(args.productId)
}
```

## Dependency Injection

ViewModels inject Use Cases only (Koin, not Hilt):

```kotlin
// CORRECT - Inject UseCase
class ProductListViewModel(
    private val getProductsUseCase: GetProductsUseCase,
) : BaseViewModel()

// WRONG - Don't inject Repository directly
class ProductListViewModel(
    private val productRepo: ProductRepository  // NO!
) : BaseViewModel()

// WRONG - No @HiltViewModel or @Inject
@HiltViewModel  // NO! Project uses Koin
class ProductListViewModel @Inject constructor(...)
```

## File Organization

```
app/src/main/java/com/daria/kotlinbase/
└── presentation/
    └── products/
        ├── list/
        │   ├── ProductListFragment.kt
        │   ├── ProductListViewModel.kt
        │   ├── ProductListUiState.kt
        │   └── ProductsAdapter.kt
        └── detail/
            ├── ProductDetailFragment.kt
            ├── ProductDetailViewModel.kt
            └── ProductDetailUiState.kt
```

## Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| ViewModel class | `<Feature>ViewModel` | `ProductListViewModel` |
| UiState class | `<Feature>UiState` | `ProductListUiState` |
| Command sealed class | `Command` (nested) | `ProductListViewModel.Command` |
| Handler methods | `on<Action>()` or `on<Action>Clicked()` | `onProductClicked()`, `onBack()` |
| LiveData (private) | `_field` | `_state` |
| LiveData (public) | `field` | `state` |
| Base commands | `_baseCmd` | (inherited from `BaseViewModel`) |
| Screen commands | `_cmd` / `cmd` | feature-specific events |
