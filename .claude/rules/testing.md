# Testing Rules

## Test Organization

Tests live under `app/src/test/java/com/daria/kotlinbase/` and mirror the production package structure by feature area:

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
        // ViewModel loads products in init, so create after mocks are configured
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
    fun `onRefresh with success clears errorMessage`() = runTest {
        // Arrange - first load fails
        coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Error("Error")
        sut = createSut()
        advanceUntilIdle()

        // Arrange - refresh succeeds
        coEvery { mockGetProductsUseCase.executeNow(Unit) } returns Result.Success(mockProducts)

        // Act
        sut.onRefresh()
        advanceUntilIdle()

        // Assert
        assertEquals(null, sut.state.value?.errorMessage)
        assertEquals(mockProducts, sut.state.value?.products)
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

Repository tests verify that the implementation delegates to the API and applies the mapper:

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
        assertEquals(mockDtos.size, result.size)
        assertEquals(mockDtos[0].id, result[0].id)
        assertEquals(mockDtos[0].image, result[0].imageUrl)  // mapper field rename
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
        assertEquals(4.5, product.rating, 0.0)                         // nested flattened
        assertEquals(120, product.ratingCount)
    }
}
```

## MockK Patterns

```kotlin
// Basic mock
private val mockUseCase: GetProductsUseCase = mockk()

// Relaxed mock (returns default values — useful for repos/apis when you only care about calls)
private val mockRepo: ProductRepository = mockk(relaxed = true)

// Stubbing suspend functions (use case returns Result)
coEvery { mockUseCase.executeNow(Unit) } returns Result.Success(data)
coEvery { mockUseCase.executeNow(Unit) } returns Result.Error("Error message")

// Stubbing repository methods (return domain types directly)
coEvery { mockRepo.getProducts() } returns mockProducts

// Verifying calls
coVerify(exactly = 1) { mockUseCase.executeNow(Unit) }
coVerify(exactly = 1) { mockRepo.getProducts() }

// Argument capture
val slot = slot<Int>()
coEvery { mockUseCase.executeNow(capture(slot)) } returns Result.Success(product)
```

## Arrange-Act-Assert Pattern

Always structure tests with clear sections:

```kotlin
@Test
fun `methodName with condition returns expectedResult`() = runTest {
    // Arrange - Set up test data and mocks
    coEvery { mockUseCase.executeNow(Unit) } returns Result.Success(expectedData)

    // Act - Perform the action
    sut.loadProducts()
    advanceUntilIdle()

    // Assert - Verify the results
    assertEquals(expectedData, sut.state.value?.products)
}
```

## Test Naming Convention

Format: `methodName with condition returns expectedResult` (use backticks)

```kotlin
fun `loadProducts with success updates state products`()
fun `loadProducts with error updates state errorMessage`()
fun `executeNow calls repository`()
fun `onProductClicked emits PerformNavAction`()
fun `toDomain maps all fields including nested rating`()
```

## Coroutine Testing

Use `runTest` and `advanceUntilIdle` for coroutine tests. Set/reset the main dispatcher in `@Before`/`@After`:

```kotlin
private val testDispatcher = StandardTestDispatcher()

@Before
fun setUp() {
    Dispatchers.setMain(testDispatcher)
    // ... create mocks and SUT
}

@After
fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
}

@Test
fun `async operation completes`() = runTest {
    // Arrange
    coEvery { mockUseCase.executeNow(Unit) } returns Result.Success(data)

    // Act
    sut.loadProducts()
    advanceUntilIdle()  // drain all coroutines (viewModelScope.launch)

    // Assert
    assertEquals(data, sut.state.value?.products)
}
```

## Required Test Coverage

For each ViewModel:
- [ ] All action handler methods tested
- [ ] Success and failure paths for API calls
- [ ] Navigation commands verified (`baseCmd` emissions)
- [ ] UiState transitions verified (isLoading, isRefreshing, errorMessage)

For each Use Case:
- [ ] Repository call verified with `coVerify`
- [ ] Success path returns `Result.Success`
- [ ] Exception path returns `Result.Error`

For each Repository Implementation:
- [ ] API call verified
- [ ] Mapper applied (verify field renames/flattening)
- [ ] Exception propagation verified

For each Mapper:
- [ ] All field mappings verified
- [ ] Nested DTO flattening verified
- [ ] Field renames verified

## Run Tests

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "*.ProductListViewModelTest"

# Run all tests in a package
./gradlew testDebugUnitTest --tests "com.daria.kotlinbase.presentation.products.*"
```
