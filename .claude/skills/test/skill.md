---
name: test
description: Write unit tests following Android project conventions with JUnit and MockK
allowed-tools: Read, Grep, Glob, Edit, Write, Bash, Task
argument-hint: "[ViewModel, UseCase, Repository, or Mapper to test]"
---

# Android Test Writing Guide

Write tests following these patterns.

## Code Style
- Never add unnecessary comments
- Use descriptive test method names with backticks
- Follow Arrange-Act-Assert pattern
- Use MockK for mocking

## Test Location

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

## ViewModel Test Pattern

```kotlin
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    private lateinit var sut: ProductListViewModel
    private lateinit var mockGetProductsUseCase: GetProductsUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockGetProductsUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun createSut(): ProductListViewModel {
        // ViewModel calls loadProducts() in init, so create after configuring mocks
        return ProductListViewModel(getProductsUseCase = mockGetProductsUseCase)
    }

    @Test
    fun `init with success updates state products`() = runTest {
        // Arrange
        coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Success(mockProducts)

        // Act
        sut = createSut()
        advanceUntilIdle()

        // Assert
        assertEquals(mockProducts, sut.state.value?.products)
        assertEquals(false, sut.state.value?.isLoading)
        assertEquals(null, sut.state.value?.errorMessage)
    }

    @Test
    fun `init with error updates state errorMessage`() = runTest {
        // Arrange
        coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Error("Network error")

        // Act
        sut = createSut()
        advanceUntilIdle()

        // Assert
        assertEquals("Network error", sut.state.value?.errorMessage)
        assertEquals(false, sut.state.value?.isLoading)
    }

    @Test
    fun `onProductClicked emits PerformNavAction`() = runTest {
        // Arrange
        coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Success(emptyList())
        sut = createSut()
        advanceUntilIdle()
        val emittedCommands = mutableListOf<BaseCommand>()
        sut.baseCmd.observeForever { emittedCommands.add(it) }

        // Act
        sut.onProductClicked(productId = 42)

        // Assert
        assertTrue(emittedCommands.last() is BaseCommand.PerformNavAction)
    }

    @Test
    fun `onRefresh calls use case again`() = runTest {
        // Arrange
        coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Success(mockProducts)
        sut = createSut()
        advanceUntilIdle()

        // Act
        sut.onRefresh()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { mockGetProductsUseCase.executeNow(Unit) }
    }

    companion object {
        private val mockProducts = listOf(
            Product(id = 1, title = "Backpack", price = 42.5, description = "Spacious",
                category = "men's clothing", imageUrl = "https://example.com/img.jpg",
                rating = 4.5, ratingCount = 120)
        )
    }
}
```

## Use Case Test Pattern

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class GetProductsUseCaseTest {

    private lateinit var sut: GetProductsUseCase
    private lateinit var mockRepo: ProductRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk()
        sut = GetProductsUseCase(repo = mockRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `executeNow calls repository`() = runTest {
        // Arrange
        coEvery { mockRepo.getProducts() } returns mockProducts

        // Act
        sut.executeNow(Unit)

        // Assert
        coVerify(exactly = 1) { mockRepo.getProducts() }
    }

    @Test
    fun `executeNow with success returns Result Success`() = runTest {
        // Arrange
        coEvery { mockRepo.getProducts() } returns mockProducts

        // Act
        val result = sut.executeNow(Unit)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(mockProducts, (result as Result.Success).data)
    }

    @Test
    fun `executeNow with exception returns Result Error`() = runTest {
        // Arrange
        coEvery { mockRepo.getProducts() } throws IOException("Network error")

        // Act
        val result = sut.executeNow(Unit)

        // Assert
        assertTrue(result is Result.Error)
    }

    companion object {
        private val mockProducts = listOf(
            Product(id = 1, title = "Backpack", price = 42.5, description = "Spacious",
                category = "men's clothing", imageUrl = "https://example.com/img.jpg",
                rating = 4.5, ratingCount = 120)
        )
    }
}
```

## Repository Test Pattern

Repository tests verify API delegation and that the mapper is applied:

```kotlin
class ProductRepositoryImplTest {

    private lateinit var sut: ProductRepositoryImpl
    private lateinit var mockApi: ProductApi

    @Before
    fun setUp() {
        mockApi = mockk()
        sut = ProductRepositoryImpl(api = mockApi)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getProducts calls api`() = runTest {
        // Arrange
        coEvery { mockApi.getProducts() } returns mockDtos

        // Act
        sut.getProducts()

        // Assert
        coVerify(exactly = 1) { mockApi.getProducts() }
    }

    @Test
    fun `getProducts maps dtos to domain`() = runTest {
        // Arrange
        coEvery { mockApi.getProducts() } returns mockDtos

        // Act
        val result = sut.getProducts()

        // Assert
        assertEquals(mockDtos[0].image, result[0].imageUrl)  // verify field rename
        assertEquals(mockDtos[0].rating.rate, result[0].rating, 0.0)  // verify nested flattening
    }

    @Test
    fun `getProducts with api error throws exception`() = runTest {
        // Arrange
        coEvery { mockApi.getProducts() } throws IOException("Network error")

        // Act & Assert
        assertFailsWith<IOException> { sut.getProducts() }
    }

    companion object {
        private val mockDtos = listOf(
            ProductDto(id = 1, title = "Backpack", price = 42.5, description = "Spacious",
                category = "men's clothing", image = "https://example.com/img.jpg",
                rating = RatingDto(rate = 4.5, count = 120))
        )
    }
}
```

## Mapper Test Pattern

```kotlin
class ProductMapperTest {

    @Test
    fun `toDomain maps all fields including nested rating`() {
        val dto = ProductDto(
            id = 7,
            title = "Backpack",
            price = 42.5,
            description = "Spacious",
            category = "men's clothing",
            image = "https://example.com/img.jpg",
            rating = RatingDto(rate = 4.5, count = 120),
        )

        val product = dto.toDomain()

        assertEquals(7, product.id)
        assertEquals("Backpack", product.title)
        assertEquals(42.5, product.price, 0.0)
        assertEquals("https://example.com/img.jpg", product.imageUrl)  // field rename
        assertEquals(4.5, product.rating, 0.0)    // nested flattened
        assertEquals(120, product.ratingCount)
    }
}
```

## MockK Patterns

```kotlin
// Basic mock
private val mockUseCase: GetProductsUseCase = mockk()

// Relaxed mock (useful when you only care about call verification)
private val mockRepo: ProductRepository = mockk(relaxed = true)

// Stubbing Use Case — returns Result via executeNow
coEvery { mockUseCase.executeNow(Unit) } returns Result.Success(data)
coEvery { mockUseCase.executeNow(Unit) } returns Result.Error("Error message")

// Stubbing Repository — returns domain types directly
coEvery { mockRepo.getProducts() } returns mockProducts

// Verifying calls
coVerify(exactly = 1) { mockUseCase.executeNow(Unit) }
coVerify(exactly = 1) { mockRepo.getProducts() }

// Argument capture
val slot = slot<Int>()
coEvery { mockUseCase.executeNow(capture(slot)) } returns Result.Success(product)
```

## Test Naming Convention

Format: `methodName with condition returns expectedResult` (backticks)

```kotlin
fun `init with success updates state products`()
fun `init with error updates state errorMessage`()
fun `executeNow calls repository`()
fun `executeNow with exception returns Result Error`()
fun `onProductClicked emits PerformNavAction`()
fun `toDomain maps all fields including nested rating`()
```

## Required Test Coverage

For each ViewModel:
- [ ] All action handler methods tested
- [ ] Success and failure paths for async calls
- [ ] BaseCommand emissions verified (`baseCmd` observations)
- [ ] UiState transitions verified (isLoading, isRefreshing, errorMessage, data)

For each Use Case:
- [ ] Repository call verified with `coVerify`
- [ ] Success path returns `Result.Success` with correct data
- [ ] Exception path returns `Result.Error`

For each Repository Implementation:
- [ ] API call delegation verified
- [ ] Mapper applied — verify field renames and nested DTO flattening
- [ ] Exception propagation verified

For each Mapper:
- [ ] All field mappings verified
- [ ] Nested DTO flattening verified
- [ ] Any field renames verified

## Run Tests

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "*.ProductListViewModelTest"

# Run specific package
./gradlew testDebugUnitTest --tests "com.daria.kotlinbase.presentation.*"
```
