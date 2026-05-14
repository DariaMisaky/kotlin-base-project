# Kotlin Base Project

A skeleton Android app with **Clean Architecture + MVVM + XML + DataBinding**, with one wired sample feature (Products list & detail from the FakeStore API). Built as a starting point for live coding exercises and as a personal reference for the Wolfpack Digital base patterns, modernized with the Extra-Karte production stack.

## Run

```bash
./gradlew assembleDebug
./gradlew installDebug   # device / emulator required
./gradlew testDebugUnitTest
./gradlew ktlintCheck
```

The base URL is set in `app/build.gradle.kts` via `buildConfigField "BASE_URL"`.

## Structure

```
app/src/main/java/com/daria/kotlinbase/
├── application/           # Application class + Koin modules (AppModules)
├── data/                  # Retrofit API, DTOs, mappers, repository impls
│   ├── mappers/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── common/    # ApiProvider, AuthenticationInterceptor
│   │   │   └── ProductApi.kt
│   │   └── dto/
│   └── repositories/
├── domain/                # Pure Kotlin: entities, repository interfaces, use cases
│   ├── abstractions/
│   ├── entities/
│   └── usecases/
├── presentation/          # Activities, Fragments, ViewModels
│   ├── MainActivity.kt
│   └── products/
│       ├── list/          # ProductListFragment + ViewModel + Adapter
│       └── detail/        # ProductDetailFragment + ViewModel
└── shared/
    ├── base/              # BaseFragment, BaseViewModel, BaseUseCase, Result, ApiError, BaseCommand
    └── utils/
        ├── bindingadapters/  # Image (Coil) + view visibility
        ├── extensions/       # Throwable.getParsedError()
        └── LiveEvent.kt      # Single-fire event holder
```

Dependency rule: `presentation → domain ← data`. Domain has no Android imports. Presentation does not import DTOs. Data does not import Fragments/Activities.

## Tech stack

- **UI:** XML layouts + DataBinding, Fragment Navigation 2.9.5 + SafeArgs
- **Theme:** Material 3 (Theme.Material3.DayNight)
- **DI:** Koin 4.1.1
- **Network:** Retrofit 3.0.0, OkHttp 5.2.0, Gson 2.13.2
- **Async:** Coroutines 1.10.2, LiveData + custom `LiveEvent` for one-shot side effects
- **Images:** Coil 3
- **Storage:** DataStore Preferences 1.1.7 (wired but no usage in this skeleton)
- **Tests:** JUnit 4, MockK, kotlinx.coroutines.test

minSdk 26, compileSdk/targetSdk 36, Kotlin 2.2.20, Java 17, AGP 8.13.0.

## Architecture decisions

### Why single-module

Smaller surface, faster builds, no Gradle module-path complexity. A single-module project with clean package boundaries gives 80% of the value of multi-module without the setup cost. If the project grows beyond ~20 features, splitting into `:core`, `:data`, `:domain`, `:presentation` is a low-risk migration.

### Why Koin over Hilt

Runtime DI with zero compile-time overhead, no KAPT/KSP boilerplate, very fast feedback loop when iterating on bindings. The DSL is idiomatic Kotlin and parametersOf makes runtime arguments (like `productId`) ergonomic. Compile-time safety of Hilt is valuable on larger codebases but not necessary here.

### Why a custom `Result` instead of `kotlin.Result`

`kotlin.Result` is restricted as a return type (the compiler warns) and its error type is just `Throwable` — we want a human-readable message at the API boundary. The `Result.Error(error, errorCode)` shape lets the UI render a useful message without inspecting the exception type.

### Why `LiveEvent` over plain LiveData for navigation/snackbar

`MutableLiveData` re-emits the last value to any new observer (including after configuration changes), which would cause duplicate navigations or repeated snackbars. `LiveEvent` wraps each observer in a single-use latch so each navigation/snackbar fires exactly once.

### Why DataBinding (not ViewBinding)

DataBinding lets the ViewModel reach the XML directly (`@{viewModel.state.isLoading}`), so showing/hiding ProgressBar, error layouts, or refresh state is declarative — no manual `visibility = if (loading) View.VISIBLE else View.GONE` in the Fragment. The `BindingAdapter` mechanism keeps view-specific glue (image loading, custom attributes) out of the Fragment.

### Why type-safe Navigation with SafeArgs

`MainNavigationDirections.actionGlobalProductDetail(productId)` is a compile-time-checked call. Renaming arguments or changing types breaks the build immediately instead of at runtime with a `Bundle` ClassCastException.

## JDK requirement

Gradle 8.14 runs on JDK 17 (or JBR 21 bundled with Android Studio). If the system JDK is 24+ and `./gradlew` fails to start, set `org.gradle.java.home` in `~/.gradle/gradle.properties` to a JDK 17 path:

```
org.gradle.java.home=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

## Out of scope (intentionally)

- Multi-module setup
- Room cache / offline-first
- Pagination (Paging 3)
- Auth flow with token refresh
- Build flavors, Firebase, Crashlytics
- UI tests (Espresso / Compose UI test)

Each of these is a 30-90 minute addition once the base lands.
