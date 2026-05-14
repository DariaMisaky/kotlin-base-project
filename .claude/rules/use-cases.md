# Use Case Rules

## Single Responsibility

Each Use Case handles ONE specific business operation. Use Cases are the only layer that ViewModels interact with for data operations.

## Standard Use Case Structure

Use Cases are concrete classes extending `BaseUseCase<P, R>`. Error wrapping is done inside each use case's `run()` method with try/catch. `CancellationException` must be caught and rethrown first to preserve structured concurrency.

```kotlin
// app/src/main/java/com/daria/kotlinbase/domain/usecases/GetProductsUseCase.kt
class GetProductsUseCase(
    private val repo: ProductRepository,
) : BaseUseCase<Unit, List<Product>>() {
    override suspend fun run(params: Unit): Result<List<Product>> =
        try {
            Result.Success(repo.getProducts())
        } catch (e: CancellationException) {
            throw e  // CRITICAL: always rethrow CancellationException first
        } catch (e: Throwable) {
            Result.Error(e.getParsedError())
        }
}
```

## BaseUseCase Pattern

All use cases extend `BaseUseCase<in P, R>`:

```kotlin
// app/src/main/java/com/daria/kotlinbase/shared/base/BaseUseCase.kt
abstract class BaseUseCase<in P, R> {
    abstract suspend fun run(params: P): Result<R>

    // Primary method used in ViewModels
    suspend fun executeNow(params: P): Result<R> = run(params)
}
```

Key points:
- `run()` is the abstract method to implement
- Each `run()` wraps its own try/catch — error handling is NOT in the base class
- `executeNow()` is the primary call method from ViewModels
- `CancellationException` is always caught and rethrown before the general `Throwable` catch
- `throwable.getParsedError()` is an extension function on `Throwable` that extracts a human-readable message

## CancellationException: Why It Must Be Rethrown

```kotlin
// CORRECT - Rethrows CancellationException to preserve coroutine cancellation
override suspend fun run(params: Unit): Result<List<Product>> =
    try {
        Result.Success(repo.getProducts())
    } catch (e: CancellationException) {
        throw e  // REQUIRED: do NOT swallow this
    } catch (e: Throwable) {
        Result.Error(e.getParsedError())
    }

// WRONG - Catching Throwable alone swallows CancellationException
override suspend fun run(params: Unit): Result<List<Product>> =
    try {
        Result.Success(repo.getProducts())
    } catch (e: Throwable) {  // NO! CancellationException caught and silenced
        Result.Error(e.getParsedError())
    }
```

`CancellationException` is how Kotlin structured concurrency signals that a coroutine scope was cancelled. Swallowing it breaks cancellation propagation.

## Use Case with Parameters

The type parameter `P` carries the input:

```kotlin
class GetProductDetailUseCase(
    private val repo: ProductRepository,
) : BaseUseCase<Int, Product>() {
    override suspend fun run(params: Int): Result<Product> =
        try {
            Result.Success(repo.getProduct(params))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.Error(e.getParsedError())
        }
}

// Called from ViewModel as:
val result = getProductDetailUseCase.executeNow(productId)
```

For complex parameters, define a data class:

```kotlin
data class CreateOrderParams(
    val productId: Int,
    val quantity: Int,
)

class CreateOrderUseCase(
    private val repo: OrderRepository,
) : BaseUseCase<CreateOrderParams, Order>() {
    override suspend fun run(params: CreateOrderParams): Result<Order> =
        try {
            Result.Success(repo.createOrder(params.productId, params.quantity))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.Error(e.getParsedError())
        }
}
```

## Use Case with No Parameters

Use `Unit` as the parameter type:

```kotlin
class GetProductsUseCase(
    private val repo: ProductRepository,
) : BaseUseCase<Unit, List<Product>>() {
    override suspend fun run(params: Unit): Result<List<Product>> =
        try {
            Result.Success(repo.getProducts())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.Error(e.getParsedError())
        }
}

// Called in ViewModel as:
val result = getProductsUseCase.executeNow(Unit)
```

## Calling Use Cases from ViewModels

```kotlin
// In ViewModel - use executeNow() inside a coroutine
viewModelScope.launch {
    when (val result = getProductsUseCase.executeNow(Unit)) {
        is Result.Success -> _state.value = _state.value?.copy(products = result.data)
        is Result.Error -> _state.value = _state.value?.copy(errorMessage = result.error)
    }
}

// Or inside performApiCall (from BaseViewModel)
performApiCall {
    when (val result = getProductsUseCase.executeNow(Unit)) {
        is Result.Success -> _items.value = result.data
        is Result.Error -> { /* handle error */ }
    }
}

// WRONG - Don't invoke use case with () operator
val result = getProductsUseCase(Unit)  // NO! Use executeNow()
```

## Dependency Injection (Koin)

Use Cases are registered as singletons in `AppModules.kt`:

```kotlin
// In AppModules.kt
private val useCases = module {
    single { GetProductsUseCase(get()) }
    single { GetProductDetailUseCase(get()) }
    // single { CreateOrderUseCase(get()) }  // add new use cases here
}
```

Use Cases inject the **repository interface** — never the API directly:

```kotlin
// CORRECT - Inject repository interface
class GetProductsUseCase(
    private val repo: ProductRepository,  // interface
) : BaseUseCase<Unit, List<Product>>()

// WRONG - Don't inject API in Use Case
class GetProductsUseCase(
    private val api: ProductApi  // NO! Goes through Repository
) : BaseUseCase<Unit, List<Product>>()

// WRONG - Don't inject concrete repository impl
class GetProductsUseCase(
    private val repo: ProductRepositoryImpl  // NO! Inject the interface
) : BaseUseCase<Unit, List<Product>>()
```

## Use Case File Organization

Use cases are in `domain/usecases/` — flat, no subfolders:

```
app/src/main/java/com/daria/kotlinbase/
└── domain/
    └── usecases/
        ├── GetProductsUseCase.kt
        ├── GetProductDetailUseCase.kt
        └── CreateOrderUseCase.kt    # when added
```

## Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Use Case class | `<Action><Resource>UseCase` | `GetProductsUseCase` |
| Abstract method | `run(params: P): Result<R>` | - |
| Invocation | `executeNow(params)` | `getProductsUseCase.executeNow(Unit)` |
| Location | `domain/usecases/` (flat) | - |

## Action Naming

Use clear action verbs:

| Action | Use When |
|--------|----------|
| `Get` | Fetching data (`GetProductsUseCase`) |
| `Create` | Creating a resource (`CreateOrderUseCase`) |
| `Update` | Modifying an existing resource (`UpdateProfileUseCase`) |
| `Delete` | Removing a resource (`DeleteDeviceUseCase`) |
| `Submit` | Sending form data (`SubmitFeedbackUseCase`) |
| `Fetch` | Alternative to `Get` when "get" is ambiguous |
