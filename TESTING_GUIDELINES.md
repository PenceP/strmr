# Testing Guidelines for Strmr

## Overview

This document establishes testing standards and best practices for the Strmr Android TV streaming application. These guidelines ensure consistent test quality, maintainability, and reliability throughout the development process.

## Testing Philosophy

### Core Principles

1. **Safety Net First**: Tests should catch regressions before they reach users
2. **Test Behavior, Not Implementation**: Focus on what the code does, not how it does it
3. **Fast Feedback**: Unit tests should run quickly to enable rapid development
4. **Clear Intent**: Test names and structure should clearly communicate what is being tested
5. **Reliable**: Tests should be deterministic and not flaky

### When to Write Tests

**ALWAYS write tests when:**
- 🆕 Adding new functionality
- 🔧 Fixing bugs (write test that reproduces bug first)
- 🔄 Refactoring existing code
- 🚨 Finding untested critical paths
- 📊 Coverage drops below minimum thresholds

## Test Categories & Coverage Requirements

### 1. Unit Tests (`/src/test/`)

**Required for:**
- All ViewModels (minimum 90% coverage)
- All Repositories (minimum 85% coverage)
- All business logic classes (minimum 80% coverage)
- Utility functions and extensions (minimum 70% coverage)

**Location:** `app/src/test/java/com/strmr/ai/`

### 2. Integration Tests (`/src/test/java/integration/`)

**Required for:**
- Navigation flows between ViewModels
- Multi-repository interactions
- Complex business workflows
- Cross-component communication

### 3. UI Component Tests (`/src/androidTest/`)

**Required for:**
- Custom Compose components
- Navigation behavior
- User interaction flows
- Accessibility compliance

## Test Structure & Naming

### Naming Convention

```kotlin
@Test
fun `methodName - given condition - should expected behavior`() {
    // Test implementation
}
```

**Examples:**
```kotlin
@Test
fun `loadMovie - given valid tmdbId - should load movie successfully`()

@Test
fun `loadMovie - given invalid tmdbId - should handle error gracefully`()

@Test
fun `getTrendingMovies - given empty database - should fetch from API`()
```

### Test Structure (Given-When-Then)

```kotlin
@Test
fun `example test`() = runTest {
    // Given - Set up test conditions
    val testData = createTestData()
    whenever(repository.getData()).thenReturn(testData)
    
    // When - Execute the behavior being tested
    val result = viewModel.performAction()
    
    // Then - Verify the expected outcome
    assertEquals(expectedResult, result)
    verify(repository).getData()
}
```

## Testing Frameworks & Tools

### Required Dependencies

```kotlin
// Unit Testing
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito:mockito-core:5.1.1")
testImplementation("org.mockito:mockito-inline:5.1.1")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

// UI Testing
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
```

### Mockito Best Practices

1. **Use `mock()` function** from mockito-kotlin
2. **Reset mocks in setup** when needed to avoid test interference
3. **Verify interactions** with `verify()` to ensure correct behavior
4. **Use `whenever().thenReturn()`** for stubbing return values
5. **Use `argumentCaptor`** for complex argument verification

```kotlin
// Good
private lateinit var repository: MovieRepository

@Before
fun setup() {
    repository = mock()
    whenever(repository.getMovie(any())).thenReturn(testMovie)
}

// Bad - Don't use @Mock annotations in Kotlin
```

### Coroutine Testing

```kotlin
@ExperimentalCoroutinesApi
class ViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `test suspend function`() = runTest {
        // Test implementation
    }
}
```

## Test Data Creation

### Helper Functions

Create reusable helper functions for test data:

```kotlin
// In test classes
private fun createTestMovieEntity(
    tmdbId: Int = 123,
    title: String = "Test Movie"
): MovieEntity {
    return MovieEntity(
        tmdbId = tmdbId,
        title = title,
        // ... other required fields with sensible defaults
    )
}
```

### Test Data Principles

1. **Use realistic data** that represents actual usage
2. **Provide sensible defaults** with option to override specific fields
3. **Create factory functions** for complex objects
4. **Use constants** for commonly used test values

```kotlin
object TestConstants {
    const val TEST_TMDB_ID = 123
    const val TEST_IMDB_ID = "tt1234567"
    const val TEST_TITLE = "Test Movie"
    const val TEST_DATE = "2024-01-01"
}
```

## ViewModels Testing

### Standard ViewModel Test Structure

```kotlin
@ExperimentalCoroutinesApi
class ExampleViewModelTest {
    
    // Dependencies (mocked)
    private lateinit var repository: ExampleRepository
    
    // System under test
    private lateinit var viewModel: ExampleViewModel
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        repository = mock()
        
        viewModel = ExampleViewModel(repository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadData - given successful repository call - should update state`() = runTest {
        // Given
        val testData = createTestData()
        whenever(repository.getData()).thenReturn(testData)
        
        // When
        viewModel.loadData()
        
        // Then
        assertEquals(testData, viewModel.data.value)
        assertFalse(viewModel.isLoading.value)
        verify(repository).getData()
    }
}
```

### ViewModel Testing Checklist

- [ ] Test initial state
- [ ] Test successful data loading
- [ ] Test error states
- [ ] Test loading states
- [ ] Test user interactions
- [ ] Test state transitions
- [ ] Verify repository interactions

## Repository Testing

### Standard Repository Test Structure

```kotlin
class ExampleRepositoryTest {
    
    private lateinit var dao: ExampleDao
    private lateinit var apiService: ApiService
    private lateinit var repository: ExampleRepository
    
    @Before
    fun setup() {
        dao = mock()
        apiService = mock()
        repository = ExampleRepository(dao, apiService)
    }
    
    @Test
    fun `getData - given cached data exists - should return cached data`() = runTest {
        // Given
        val cachedData = createTestData()
        whenever(dao.getData()).thenReturn(cachedData)
        
        // When
        val result = repository.getData()
        
        // Then
        assertEquals(cachedData, result)
        verify(dao).getData()
        verify(apiService, never()).getData() // Should not call API
    }
}
```

### Repository Testing Checklist

- [ ] Test caching behavior
- [ ] Test API fallback when cache empty
- [ ] Test error handling
- [ ] Test data transformation
- [ ] Test database operations
- [ ] Verify correct API calls

## Integration Testing

### Navigation Integration Tests

Test workflows that span multiple components:

```kotlin
@Test
fun `navigation flow - home to details - should load correctly`() = runTest {
    // Given - set up multiple ViewModels
    val homeViewModel = createHomeViewModel()
    val detailsViewModel = createDetailsViewModel()
    
    // When - simulate navigation flow
    homeViewModel.selectMovie(movieId)
    detailsViewModel.loadMovie(movieId)
    
    // Then - verify end-to-end behavior
    assertEquals(expectedMovie, detailsViewModel.movie.value)
    verify(movieRepository).getMovieByTmdbId(movieId)
}
```

## Performance Testing

### Response Time Benchmarks

```kotlin
@Test
fun `loadData - should complete within acceptable time`() = runTest {
    val startTime = System.currentTimeMillis()
    
    viewModel.loadData()
    
    val duration = System.currentTimeMillis() - startTime
    assertTrue("Operation took ${duration}ms, expected < 100ms", duration < 100)
}
```

## Test Coverage Requirements

### Minimum Coverage Thresholds

- **New ViewModels**: 90%
- **New Repositories**: 85%
- **New Business Logic**: 80%
- **Utility Functions**: 70%
- **Overall Project**: 60% (increasing to 75% over time)

### Coverage Commands

```bash
# Run unit tests with coverage
./gradlew testDebugUnitTestCoverage

# View coverage report
open app/build/reports/coverage/testDebugUnitTest/html/index.html

# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "MovieRepositoryTest"
```

## Error Testing

### Standard Error Scenarios

Test these common error conditions:

```kotlin
@Test
fun `loadData - given network error - should show error state`() = runTest {
    // Given
    whenever(repository.getData()).thenThrow(NetworkException("Connection failed"))
    
    // When
    viewModel.loadData()
    
    // Then
    assertTrue(viewModel.hasError.value)
    assertEquals("Connection failed", viewModel.errorMessage.value)
}
```

### Error Testing Checklist

- [ ] Network failures
- [ ] Database errors
- [ ] Invalid data formats
- [ ] Null/empty responses
- [ ] Timeout scenarios
- [ ] Permission denied
- [ ] Out of memory conditions

## Test Maintenance

### Regular Maintenance Tasks

1. **Weekly**: Review failing tests and fix flaky tests
2. **Monthly**: Check coverage reports and identify gaps
3. **Per Release**: Run full test suite and performance benchmarks
4. **Code Reviews**: Ensure all new code includes appropriate tests

### Test Debt Prevention

- Write tests as you write code, not afterwards
- Refactor tests when refactoring code
- Remove obsolete tests when removing features
- Update tests when changing behavior

## Common Anti-Patterns to Avoid

### ❌ Don't Do This

```kotlin
// Testing implementation details
verify(viewModel.privateMethod())

// Overly complex test setup
// 50 lines of setup for 2 lines of test

// Unclear test names
fun `test1`()

// Testing multiple behaviors in one test
fun `testEverything`()

// Not using runTest for coroutines
fun `test`() { // Missing runTest
    viewModel.loadData() // This won't work properly
}
```

### ✅ Do This Instead

```kotlin
// Test public behavior
assertEquals(expectedResult, viewModel.data.value)

// Simple, focused setup
val testData = createTestData()

// Clear, descriptive names
fun `loadData - given valid input - should return success`()

// One behavior per test
fun `loadData - should update loading state`()
fun `loadData - should update data on success`()

// Proper coroutine testing
fun `loadData - should complete successfully`() = runTest {
    viewModel.loadData()
    assertEquals(expectedData, viewModel.data.value)
}
```

## Tools and Commands

### Essential Testing Commands

```bash
# Run all unit tests
./gradlew test

# Run tests with coverage
./gradlew testDebugUnitTestCoverage

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean and test
./gradlew clean test

# Run specific test package
./gradlew test --tests "com.strmr.ai.viewmodel.*"
```

### Debugging Failed Tests

1. Check test output in `app/build/reports/tests/`
2. Use `println()` or `Log.d()` for debugging
3. Run single test in isolation
4. Check for timing issues in coroutine tests
5. Verify mock setup is correct

## Conclusion

These guidelines provide a foundation for consistent, reliable testing across the Strmr codebase. Following these practices will:

- Catch bugs before they reach users
- Enable confident refactoring
- Improve code quality
- Facilitate faster development
- Create living documentation of system behavior

Remember: **Good tests are an investment in code quality and developer productivity.**

---

*Last updated: Phase 0 Test Coverage Baseline - January 2025*