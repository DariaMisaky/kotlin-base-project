# DataBinding Rules

## DataBinding is Enabled

DataBinding is enabled via `buildFeatures { dataBinding = true }` in `app/build.gradle.kts`. Every layout that needs ViewModel access must use the DataBinding layout wrapper.

## Layout Structure

Every DataBinding layout is wrapped in `<layout>` with a `<data>` block declaring the ViewModel variable:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <data>
        <variable
            name="viewModel"
            type="com.daria.kotlinbase.presentation.products.list.ProductListViewModel" />
    </data>

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:isRefreshing="@{viewModel.state.isRefreshing}"
        app:onRefresh="@{() -> viewModel.onRefresh()}">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/productsRecycler"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:items="@{viewModel.state.products}" />

    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

</layout>
```

## BaseFragment Setup

`BaseFragment` inflates the binding via `DataBindingUtil.inflate` and sets `lifecycleOwner`:

```kotlin
// BaseFragment.onCreateView (already implemented in shared/base/BaseFragment.kt)
_binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
binding.lifecycleOwner = viewLifecycleOwner  // enables LiveData observation in XML
```

Fragments set the ViewModel binding in `onViewCreated`:

```kotlin
// ProductListFragment.kt
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.viewModel = viewModel
    binding.productsRecycler.layoutManager = GridLayoutManager(context, GRID_SPAN)
    binding.productsRecycler.adapter = productsAdapter
}
```

Fragments are thin: set the viewModel binding, configure RecyclerView layout manager and adapter. Everything else — visibility, loading state, click handlers — belongs in XML.

## Click Handlers in XML

Bind click handlers directly in XML using lambda expressions:

```xml
<!-- Button click -->
<Button
    android:onClick="@{() -> viewModel.onBack()}" />

<!-- Click with argument -->
<com.google.android.material.card.MaterialCardView
    android:onClick="@{() -> viewModel.onProductClicked(product.id)}" />

<!-- Navigation click on Toolbar -->
<com.google.android.material.appbar.MaterialToolbar
    app:onNavigationClick="@{() -> viewModel.onBack()}" />
```

## Available BindingAdapters

All binding adapters are in `shared/utils/bindingadapters/`:

### View visibility (`ViewBindingAdapters.kt`)

```xml
<!-- GONE when false/null, VISIBLE when true -->
<ProgressBar app:isVisible="@{viewModel.state.isLoading}" />

<!-- INVISIBLE when false/null, VISIBLE when true -->
<TextView app:isVisibleOrInvisible="@{viewModel.hasTitle}" />
```

### SwipeRefreshLayout (`ViewBindingAdapters.kt`)

```xml
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    app:isRefreshing="@{viewModel.state.isRefreshing}"
    app:onRefresh="@{() -> viewModel.onRefresh()}" />
```

### RecyclerView with ListAdapter (`ViewBindingAdapters.kt`)

```xml
<androidx.recyclerview.widget.RecyclerView
    app:items="@{viewModel.state.products}" />
```

This calls `adapter.submitList(items)` — the adapter must extend `ListAdapter<T, VH>`.

### MaterialToolbar navigation click (`ViewBindingAdapters.kt`)

```xml
<com.google.android.material.appbar.MaterialToolbar
    app:onNavigationClick="@{() -> viewModel.onBack()}" />
```

### ImageView with Coil (`ImageBindingAdapters.kt`)

```xml
<ImageView
    app:imageUrl="@{product.imageUrl}" />
```

Loads the URL with Coil 3 and a crossfade animation. Null or blank URLs are safely ignored.

## Adding a New BindingAdapter

Add extension functions to the appropriate file in `shared/utils/bindingadapters/`:

```kotlin
// Example: binding adapter for loading a circular image
@BindingAdapter("imageUrlCircle")
fun ImageView.bindImageUrlCircle(url: String?) {
    if (url.isNullOrBlank()) return
    load(url) {
        crossfade(true)
        transformations(CircleCropTransformation())
    }
}
```

## Fragment Pattern: Thin Fragment, Fat XML

**Before (too much logic in Fragment):**

```kotlin
// BAD - Fragment micromanaging visibility
override fun onViewCreated(view: View, ...) {
    viewModel.state.observe(viewLifecycleOwner) { state ->
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.errorText.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE
        binding.errorText.text = state.errorMessage
        productsAdapter.submitList(state.products)
    }
}
```

**After (wired in XML):**

```xml
<!-- GOOD - XML handles all visibility and data binding -->
<ProgressBar
    app:isVisible="@{viewModel.state.isLoading}" />

<TextView
    android:text="@{viewModel.state.errorMessage}"
    app:isVisible="@{viewModel.state.errorMessage != null}" />

<androidx.recyclerview.widget.RecyclerView
    app:items="@{viewModel.state.products}" />
```

```kotlin
// GOOD - Fragment only sets adapter/layout manager
override fun onViewCreated(view: View, ...) {
    super.onViewCreated(view, ...)
    binding.viewModel = viewModel
    binding.productsRecycler.layoutManager = GridLayoutManager(context, 2)
    binding.productsRecycler.adapter = productsAdapter
}
```

## UiState with DataBinding

DataBinding observes `LiveData<UiState>` directly in XML when `lifecycleOwner` is set:

```xml
<data>
    <variable
        name="viewModel"
        type="com.daria.kotlinbase.presentation.products.list.ProductListViewModel" />
</data>

<!-- Access nested UiState fields directly -->
<ProgressBar app:isVisible="@{viewModel.state.isLoading}" />
<TextView android:text="@{viewModel.state.errorMessage}" />
```

## RecyclerView Item Binding

Each RecyclerView item layout also uses DataBinding with its own variable:

```xml
<!-- item_product.xml -->
<layout>
    <data>
        <variable name="product" type="com.daria.kotlinbase.domain.entities.Product" />
    </data>

    <com.google.android.material.card.MaterialCardView ...>
        <ImageView app:imageUrl="@{product.imageUrl}" />
        <TextView android:text="@{product.title}" />
        <TextView android:text="@{@string/product_price_format(product.price)}" />
    </com.google.android.material.card.MaterialCardView>
</layout>
```

In the adapter `onBindViewHolder`:

```kotlin
override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
    holder.binding.product = getItem(position)
    holder.binding.executePendingBindings()
}
```

## Theme

Material 3 (`Theme.Material3.DayNight.NoActionBar`) is applied in `AndroidManifest.xml`. Use Material 3 components:
- `MaterialCardView`, `MaterialToolbar`, `MaterialButton`, `MaterialTextView`
- Colors via Material 3 color tokens (`?attr/colorPrimary`, `?attr/colorSurface`, etc.)
- Do not use `AppCompat` widget variants when a Material 3 equivalent exists
