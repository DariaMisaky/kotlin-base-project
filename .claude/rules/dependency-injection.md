# Dependency Injection Rules

## Koin Framework

All dependencies are managed using Koin 4.1.1. Dependencies are registered in `AppModules.kt` and injected via constructor parameters or Koin's `get()`.

## Module Organization

All DI is centralized in a single file:

```
app/src/main/java/com/daria/kotlinbase/application/
├── KotlinBaseApp.kt    # Koin initialization (Application subclass)
└── AppModules.kt       # All module definitions
```

`AppModules.kt` contains named sub-modules combined into a single `modules` list:

```kotlin
// app/src/main/java/com/daria/kotlinbase/application/AppModules.kt
object AppModules {

    private val apiModule = module {
        single { ApiProvider.provideProductApi() }
    }

    private val repoModule = module {
        single<ProductRepository> { ProductRepositoryImpl(get()) }
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

## Registration Patterns

### ViewModels

```kotlin
private val viewModels = module {
    // Simple ViewModel
    viewModel { ProductListViewModel(get()) }

    // ViewModel with runtime parameter (e.g., product ID from SafeArgs)
    viewModel { (productId: Int) -> ProductDetailViewModel(productId, get()) }
}
```

Runtime parameters are passed from the Fragment using `parametersOf`:

```kotlin
// In Fragment
override val viewModel: ProductDetailViewModel by viewModel {
    parametersOf(args.productId)
}
```

### APIs (via ApiProvider)

```kotlin
private val apiModule = module {
    single { ApiProvider.provideProductApi() }
    // single { ApiProvider.provideOrderApi() }  // add new APIs here
}
```

### Repositories (interface binding — REQUIRED)

```kotlin
private val repoModule = module {
    // CORRECT - single<Interface> { Implementation(get()) }
    single<ProductRepository> { ProductRepositoryImpl(get()) }

    // WRONG - concrete binding (this is NOT how this project works)
    // single { ProductRepositoryImpl(get()) }  // NO! Bind to interface
}
```

This project uses **interface binding** (`single<Interface> { Impl(get()) }`). Use cases inject the interface type, not the concrete implementation. This is the opposite of projects that use concrete-only repos.

### Use Cases (singletons)

```kotlin
private val useCases = module {
    single { GetProductsUseCase(get()) }
    single { GetProductDetailUseCase(get()) }
}
```

## Application Class

```kotlin
// app/src/main/java/com/daria/kotlinbase/KotlinBaseApp.kt
class KotlinBaseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@KotlinBaseApp)
            modules(AppModules.modules)
        }
    }
}
```

## Dependency Injection Hierarchy

Follow this hierarchy — each layer only injects from the layer below:

```
ViewModel
    ↓ injects
UseCase
    ↓ injects
Repository (interface)
    ↓ injects
API
```

```kotlin
// CORRECT - ViewModel injects UseCase
class ProductListViewModel(
    private val getProductsUseCase: GetProductsUseCase,
) : BaseViewModel()

// WRONG - ViewModel injects Repository (skips UseCase layer)
class ProductListViewModel(
    private val repo: ProductRepository  // NO!
) : BaseViewModel()

// CORRECT - UseCase injects Repository interface
class GetProductsUseCase(
    private val repo: ProductRepository,  // interface
) : BaseUseCase<Unit, List<Product>>()

// WRONG - UseCase injects API (skips Repository layer)
class GetProductsUseCase(
    private val api: ProductApi  // NO!
) : BaseUseCase<Unit, List<Product>>()
```

## Koin Scope Types

| Koin DSL | Scope | Usage |
|----------|-------|-------|
| `single { }` | Singleton | Repositories, Use Cases, APIs |
| `single<Interface> { Impl(get()) }` | Singleton (interface-bound) | Repository bindings |
| `viewModel { }` | ViewModel-scoped | ViewModels |
| `viewModel { (param: Type) -> }` | ViewModel-scoped + runtime param | ViewModels with constructor args |

## Fragment ViewModel Injection

```kotlin
// Simple ViewModel (no runtime params)
override val viewModel: ProductListViewModel by viewModel()

// ViewModel with runtime parameter
override val viewModel: ProductDetailViewModel by viewModel {
    parametersOf(args.productId)
}
```

## Adding New Dependencies

When adding a new feature, register all components in `AppModules.kt`:

```kotlin
// 1. Add API (if new endpoint group)
// In apiModule: single { ApiProvider.provideOrderApi() }

// 2. Add Repository (interface binding)
// In repoModule: single<OrderRepository> { OrderRepositoryImpl(get()) }

// 3. Add Use Cases
// In useCases: single { GetOrdersUseCase(get()) }

// 4. Add ViewModel
// In viewModels: viewModel { OrderListViewModel(get()) }
```

## DataStore

`DataStore Preferences` is a dependency in the project but has no registered Koin bindings yet. When auth/preferences are needed, register a `DataStore<Preferences>` singleton in a new `dataStoreModule`:

```kotlin
private val dataStoreModule = module {
    single {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { androidContext().preferencesDataStoreFile("app_prefs") }
        )
    }
}
```

## Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Koin keyword | `single`, `viewModel` | - |
| Interface binding | `single<Interface> { Impl(get()) }` | `single<ProductRepository> { ProductRepositoryImpl(get()) }` |
| Dependency resolution | `get()` | `get()`, `get(qualifier)` |
| Runtime parameter | `{ (param: Type) -> ... }` | `{ (productId: Int) -> ... }` |
