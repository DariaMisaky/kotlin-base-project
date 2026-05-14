# Navigation Rules

## Fragment Navigation + SafeArgs

All features use **Fragment Navigation + SafeArgs** (XML navigation graphs). There is no Jetpack Compose in this project.

## XML Navigation Graphs

Navigation graphs are defined in XML with `<fragment>`, `<action>`, `<argument>`, and `<deepLink>` elements:

```xml
<!-- app/src/main/res/navigation/main_navigation.xml -->
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/main_navigation"
    app:startDestination="@id/productListFragment">

    <fragment
        android:id="@+id/productListFragment"
        android:name="com.daria.kotlinbase.presentation.products.list.ProductListFragment"
        android:label="ProductListFragment">
        <action
            android:id="@+id/action_productListFragment_to_productDetailFragment"
            app:destination="@id/productDetailFragment" />
    </fragment>

    <fragment
        android:id="@+id/productDetailFragment"
        android:name="com.daria.kotlinbase.presentation.products.detail.ProductDetailFragment"
        android:label="ProductDetailFragment">
        <argument
            android:name="productId"
            app:argType="integer" />
    </fragment>
</navigation>
```

## Current Navigation Graphs

```
app/src/main/res/navigation/
└── main_navigation.xml    # Products list + detail (current only graph)
```

When the app grows, add separate graphs per feature area and nest them (or use global actions for cross-graph navigation).

## Navigation via BaseCommand

ViewModels dispatch navigation via `_baseCmd` (from `BaseViewModel`), observed by `BaseFragment`:

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
```

`BaseFragment` handles all `BaseCommand` variants automatically:

```kotlin
// BaseFragment.handleBaseCommand() handles:
// - PerformNavAction -> findNavController().navigate(command.navAction)
// - GoBack -> findNavController().popBackStack()
// - ShowSnackbar -> Snackbar.make(binding.root, ...)
// - ShowToast -> Toast.makeText(...)
// - ShowError -> Snackbar with fallback to generic_error string resource
```

## BaseCommand Types

```kotlin
// app/src/main/java/com/daria/kotlinbase/shared/base/BaseCommand.kt
sealed class BaseCommand {
    data class PerformNavAction(val navAction: NavDirections) : BaseCommand()
    data object GoBack : BaseCommand()
    data class ShowToast(val message: String) : BaseCommand()
    data class ShowSnackbar(val message: String) : BaseCommand()
    data class ShowError(val message: String?) : BaseCommand()
}
```

Keep `BaseCommand` to these five types. Do not add project-specific navigation logic here — use a screen-specific `Command` sealed class in the ViewModel for feature events (scroll to top, open bottom sheet, etc.).

## Products Example: List to Detail

### Navigation graph argument

```xml
<fragment android:id="@+id/productDetailFragment" ...>
    <argument
        android:name="productId"
        app:argType="integer" />
</fragment>
```

### ViewModel dispatches navigation

```kotlin
// ProductListViewModel.kt
fun onProductClicked(productId: Int) {
    _baseCmd.value = BaseCommand.PerformNavAction(
        MainNavigationDirections.actionGlobalProductDetail(productId)
    )
}
```

### Fragment receives the argument

```kotlin
// ProductDetailFragment.kt
private val args by navArgs<ProductDetailFragmentArgs>()

override val viewModel: ProductDetailViewModel by viewModel {
    parametersOf(args.productId)
}
```

## Global Actions

For cross-graph navigation, define a `<action>` at the root `<navigation>` level with `app:popUpTo` as needed:

```xml
<navigation android:id="@+id/main_navigation" ...>
    <!-- Global action accessible from anywhere in the graph -->
    <action
        android:id="@+id/action_global_productDetail"
        app:destination="@id/productDetailFragment" />
    ...
</navigation>
```

Reference via `MainNavigationDirections.actionGlobalProductDetail(productId)`.

## Adding a New Screen

1. Add a `<fragment>` entry to the relevant XML navigation graph
2. Define `<action>` elements for navigation to/from this fragment
3. Add `<argument>` elements for any type-safe arguments
4. In the source ViewModel, dispatch `BaseCommand.PerformNavAction(DirectionsClass.action...())`
5. In the target fragment, read args via `by navArgs<TargetFragmentArgs>()`

## Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Navigation graph | `<feature>_navigation.xml` or `nav_<feature>.xml` | `main_navigation.xml` |
| Fragment ID | `@+id/<feature>Fragment` | `@+id/productListFragment` |
| Action ID | `@+id/action_<from>_to_<to>` | `@+id/action_productListFragment_to_productDetailFragment` |
| Global action | `@+id/action_global_<destination>` | `@+id/action_global_productDetail` |
| Argument | camelCase matching ViewModel param | `productId` |
