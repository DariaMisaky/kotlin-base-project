---
name: code-review
description: Review code changes for bugs, performance, security, and project conventions. Compare branches or review uncommitted changes.
allowed-tools: Bash, Read, Grep, Glob, Task
---

# Code Review Skill

Review code changes locally by comparing branches or reviewing uncommitted changes.

## Usage

```
/code-review                           # Review uncommitted changes
/code-review main                      # Compare current branch to main
/code-review main feature-branch       # Compare main to feature-branch
```

## Review Process

### Step 1: Determine What to Review

Parse arguments to determine review scope:
- No arguments: Review uncommitted changes (`git diff` and `git diff --cached`)
- One argument: Compare current branch to specified branch (`git diff <branch>...HEAD`)
- Two arguments: Compare first branch to second (`git diff <branch1>...<branch2>`)

### Step 2: Get the Diff

```bash
# For uncommitted changes
git diff && git diff --cached

# For branch comparison
git diff <target_branch>...HEAD --name-only  # List changed files
git diff <target_branch>...HEAD              # Full diff
```

### Step 3: Review Against All Criteria

Review each changed file against the following criteria:

---

## Review Criteria

### 1. Memory Management & Leaks
- [ ] Missing proper cleanup in ViewModel `onCleared()`
- [ ] Coroutine jobs not cancelled in `viewModelScope`
- [ ] LiveData observers not cleaned up (use `viewLifecycleOwner` in Fragments)
- [ ] Context leaks (storing Activity/Fragment context in long-lived objects)
- [ ] Not using `viewLifecycleOwner` when observing in Fragments
- [ ] Long-lived references to Views

### 2. Performance Issues
- [ ] Heavy work on main thread (network calls, file I/O, parsing)
- [ ] Missing `Dispatchers.IO` for IO operations (via `viewModelScope.launch` context)
- [ ] Large images not properly sized or cached via Coil
- [ ] Missing pagination for large data sets
- [ ] Repeated API calls that should be cached
- [ ] N+1 style patterns in data fetching

### 3. UI Best Practices (XML/DataBinding)
- [ ] Binding adapters with side effects
- [ ] Complex logic in XML binding expressions (should be in ViewModel)
- [ ] Missing null safety in data binding expressions
- [ ] Hardcoded dimensions instead of dimen resources
- [ ] Missing `lifecycleOwner` on binding (prevents LiveData observation in XML)
- [ ] Fragment not setting `binding.viewModel = viewModel` in `onViewCreated`

### 4. Architecture Violations (MVVM + Clean Architecture)
- [ ] Fragment directly accessing repositories or APIs (must go through ViewModel → UseCase)
- [ ] ViewModel directly accessing network layer (must go through UseCase → Repository)
- [ ] Business logic in Fragments (belongs in ViewModel or UseCase)
- [ ] UseCase doing more than one responsibility
- [ ] ViewModel injecting Repository or API directly (must inject UseCase)
- [ ] Dependency rule violation: `presentation` imports `data` DTOs directly

### 5. Repository Pattern Violations
- [ ] Repository is a concrete class without an interface (this project uses `interface + impl`)
- [ ] No mapper applied — DTO returned directly from repository as domain type
- [ ] Result wrapping inside repository (belongs in UseCase)
- [ ] Error handling inside repository (exceptions must propagate to UseCase)
- [ ] Missing interface in `domain/abstractions/`
- [ ] Missing implementation in `data/repositories/`

### 6. Mapper Layer
- [ ] No `data/mappers/<Feature>Mapper.kt` file for a new DTO → domain mapping
- [ ] Mapper function not named `fun XxxDto.toDomain(): Xxx`
- [ ] Mapper lives inside DTO or domain entity file (should be in `data/mappers/`)
- [ ] Domain entity has `@SerializedName` (should only be in DTOs)
- [ ] DTO used in domain layer or UI layer instead of the mapped domain entity

### 7. Dependency Injection (Koin)
- [ ] Direct instantiation instead of Koin injection
- [ ] Missing registration in `AppModules.kt`
- [ ] Wrong scope (`single` vs `viewModel`)
- [ ] Repository registered as concrete binding instead of interface binding
  - Wrong: `single { ProductRepositoryImpl(get()) }`
  - Correct: `single<ProductRepository> { ProductRepositoryImpl(get()) }`
- [ ] Wrong layer injection (ViewModel injecting API or Repository directly)

### 8. Coroutines & Flow
- [ ] Missing `suspend` on methods that should be suspend
- [ ] Not using `viewModelScope` for ViewModel coroutines
- [ ] `CancellationException` caught and not rethrown in UseCase `run()` method
- [ ] Race conditions in shared mutable state (`_state.value = _state.value?.copy(...)`)
- [ ] Blocking calls on main thread
- [ ] Missing error handling in coroutines

### 9. Security Issues
- [ ] Sensitive data stored insecurely (use DataStore with encryption for tokens)
- [ ] Hardcoded API keys or secrets in source code
- [ ] Logging sensitive information (tokens, passwords, PII)
- [ ] Missing input validation for user-provided data
- [ ] Insecure network configuration (cleartext traffic)

### 10. Error Handling
- [ ] Swallowed exceptions (empty catch blocks)
- [ ] Force unwrapping (`!!`) that could crash
- [ ] Missing error state in UiState (no `errorMessage` field)
- [ ] User not informed of errors (no `ShowError`, `ShowSnackbar`, or `errorMessage` update)
- [ ] `Throwable` catch without prior `CancellationException` rethrow in UseCase
- [ ] Not using `when (val result = ...) { is Result.Success -> ... is Result.Error -> ... }` pattern

### 11. Missing Tests
- [ ] Missing ViewModel test for new action handler
- [ ] Missing UseCase test for success and error paths
- [ ] Missing RepositoryImpl test for API delegation and mapper application
- [ ] Missing Mapper test for field renames or nested DTO flattening
- [ ] No `advanceUntilIdle()` after async operations in tests
- [ ] Missing `Dispatchers.setMain(testDispatcher)` in `@Before`

### 12. Code Style & KtLint
- [ ] Lines exceeding max length
- [ ] Deep nesting (> 3 levels)
- [ ] Unused imports or variables
- [ ] Missing visibility modifiers
- [ ] Inconsistent naming conventions

### 13. Clean Code Violations
- [ ] Methods exceeding 30 lines
- [ ] Classes exceeding 400 lines
- [ ] Poor naming (unclear variable/method names)
- [ ] Too many method parameters (> 5)
- [ ] Magic numbers/strings (extract to constants or string resources)
- [ ] Duplicated code

### 14. Networking
- [ ] Missing loading state in UiState (`isLoading` field)
- [ ] Missing error handling for API calls in UseCase
- [ ] Hardcoded URLs (must come from `BuildConfig.BASE_URL`)
- [ ] DTO used directly as domain entity (must map via `toDomain()`)
- [ ] Missing `@SerializedName` on DTO fields

### 15. Project-Specific Patterns

**ViewModels:**
- [ ] Not extending `BaseViewModel`
- [ ] Using Hilt annotations (project uses Koin — no `@HiltViewModel`, no `@Inject`)
- [ ] Direct Repository or API injection instead of UseCase
- [ ] Using `_baseFragmentCmd` instead of `_baseCmd` (this project uses `_baseCmd`)
- [ ] Using `BaseFrViewModel` instead of `BaseViewModel` (this project uses `BaseViewModel`)
- [ ] Screen-specific commands not using nested `sealed class Command` + `LiveEvent`

**Use Cases:**
- [ ] Not extending `BaseUseCase<P, R>`
- [ ] Missing try/catch in `run()` method
- [ ] `CancellationException` not caught and rethrown before `Throwable` catch
- [ ] Direct API injection instead of Repository interface
- [ ] Using `invoke()` instead of `executeNow()` in ViewModel
- [ ] Located in `shared/usecases/` subfolders (should be flat in `domain/usecases/`)

**Repositories:**
- [ ] Concrete class without interface (must use `interface + impl` split)
- [ ] No mapper applied (`api.getProducts()` returned directly without `.map { it.toDomain() }`)
- [ ] `KoinComponent` on repository (this project does not extend KoinComponent on repos)
- [ ] Registered as `Repo` suffix instead of `Repository`/`RepositoryImpl`

**DataBinding:**
- [ ] Layout missing `<layout>` wrapper
- [ ] `binding.lifecycleOwner` not set (prevents LiveData observation in XML)
- [ ] Hardcoded strings in XML instead of `@string/` references
- [ ] Complex logic in XML binding expressions (should be a field/method in ViewModel)

**Navigation:**
- [ ] Not using SafeArgs for arguments (raw Bundle instead of generated Args class)
- [ ] Not using `BaseCommand` for navigation via `_baseCmd`

**Localization:**
- [ ] Hardcoded user-facing strings (must use `@string/key`)
- [ ] Strings missing from `values/strings.xml`
- [ ] `values-de/` directory created (this project is English only)

---

## Output Format

For each issue found, report:

````
## [CATEGORY] File: path/to/file.kt:line_number

**Issue:** Brief description of the problem

**Code:**
```kotlin
// problematic code
```

**Suggestion:**
```kotlin
// suggested fix
```

**Severity:** Critical | Major | Minor | Style
````

---

## Severity Levels

- **Critical:** Security vulnerabilities, crashes, memory leaks, data loss risks
- **Major:** Architecture violations, missing error handling, CancellationException swallowed, coroutine bugs, mapper not applied
- **Minor:** Missing tests, minor refactoring opportunities
- **Style:** KtLint formatting, naming, readability improvements

---

## Summary

At the end, provide:

1. **Summary Statistics:**
   - Files reviewed: X
   - Issues found: X (Critical: X, Major: X, Minor: X, Style: X)

2. **Top Priority Fixes:** List the most important issues to address first

3. **Positive Observations:** Note any well-written code or good practices observed
