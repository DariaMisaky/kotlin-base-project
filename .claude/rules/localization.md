# Localization Rules

## Android String Resources

All user-facing strings use Android string resources. The project supports **English only** (single `values/strings.xml`).

## Usage Pattern

### In XML layouts

```xml
<!-- CORRECT - Reference string resource -->
<TextView android:text="@string/products_title" />

<!-- Data binding with string resource -->
<TextView android:text="@{@string/hello_user(viewModel.userName)}" />

<!-- WRONG - Hardcoded strings -->
<TextView android:text="Products" />  <!-- NO! -->
```

### In Kotlin code

```kotlin
// CORRECT - Use getString in Fragment/Activity context
getString(R.string.error_generic)

// CORRECT - Via BaseCommand for ViewModel-driven messages
_baseCmd.value = BaseCommand.ShowSnackbar(getString(R.string.error_generic))
_baseCmd.value = BaseCommand.ShowToast(getString(R.string.product_saved))

// WRONG - Hardcoded strings
_baseCmd.value = BaseCommand.ShowToast("Saved")  // NO!
Toast.makeText(context, "Error occurred", Toast.LENGTH_SHORT).show()  // NO!
```

## String File Organization

Single locale — English only:

```
app/src/main/res/
└── values/
    └── strings.xml    # English (only file needed)
```

There is no `values-de/` or any other locale directory. Do not create one.

## strings.xml Naming Patterns

Use snake_case with content-descriptive names:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- App name -->
    <string name="app_name">Kotlin Base</string>

    <!-- Generic/common strings -->
    <string name="continue_action">Continue</string>
    <string name="confirm">Confirm</string>
    <string name="cancel">Cancel</string>
    <string name="ok">OK</string>
    <string name="done">Done</string>
    <string name="retry">Retry</string>
    <string name="loading">Loading…</string>

    <!-- Generic errors -->
    <string name="error_generic">Something went wrong</string>
    <string name="error_no_internet">No internet connection</string>

    <!-- Products feature -->
    <string name="products_title">Products</string>
    <string name="products_empty">No products found</string>
    <string name="product_detail_title">Product Detail</string>
    <string name="product_category_label">Category</string>
    <string name="product_rating_label">Rating</string>

    <!-- Format strings -->
    <string name="product_price_format">$%1$.2f</string>
    <string name="rating_count_format">%1$d reviews</string>
</resources>
```

## String Interpolation

Use positional format strings for dynamic content:

```xml
<string name="product_price_format">$%1$.2f</string>
<string name="hello_user">Hi, %1$s!</string>
<string name="rating_count_format">%1$d reviews</string>
```

```kotlin
// In Fragment/Activity
getString(R.string.product_price_format, product.price)
getString(R.string.hello_user, userName)

// In XML with data binding
android:text="@{@string/hello_user(viewModel.userName)}"
```

## Key Naming Convention

Use snake_case with content-descriptive names:

```
<context_or_feature>_<element_or_action>

Examples:
products_title           # Screen/section label
products_empty           # Empty state message
product_price_format     # Format string for price
error_generic            # Generic error message
error_no_internet        # Specific error type
continue_action          # Button label (suffix to avoid conflict with Kotlin keyword)
rating_count_format      # Dynamic format string
```

## Adding New Strings

1. Add to `values/strings.xml` (English)
2. Use via `getString(R.string.key_name)` in Kotlin or `@string/key_name` in XML

That's it — no second file needed.

## Error Messages

Use string resources for all user-facing error messages:

```kotlin
// CORRECT - Via BaseCommand with string from Fragment context
_baseCmd.value = BaseCommand.ShowError(getString(R.string.error_generic))

// CORRECT - Error from API result (already a string)
is Result.Error -> _state.value = _state.value?.copy(errorMessage = result.error)
// (The Fragment reads errorMessage and shows it directly or via a string resource fallback)

// WRONG - Hardcoded error strings
BaseCommand.ShowError("Network unavailable")  // NO! Extract to string resource
```

## Where NOT to Use String Resources

- Internal logging messages
- Debug-only strings
- Analytics event names
- API keys or identifiers

```kotlin
// OK - Internal logging doesn't need string resources
Log.d(TAG, "Loading products for screen: $screenId")

// OK - Analytics events are not user-facing
analytics.logEvent("product_list_viewed")
```

## Naming Conventions Summary

| Element | Convention | Example |
|---------|------------|---------|
| Feature string | `<feature>_<element>` | `products_title`, `product_detail_title` |
| Generic strings | descriptive or `generic_<type>` | `error_generic`, `continue_action` |
| Error strings | `error_<description>` | `error_no_internet`, `error_generic` |
| Format strings | `<context>_<element>_format` | `product_price_format`, `rating_count_format` |
| Empty state | `<feature>_empty` | `products_empty` |
