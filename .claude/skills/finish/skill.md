---
name: finish
description: Run Android quality checks before completing a feature (ktlint, lint, tests, build). Auto-fix errors.
allowed-tools: Bash, Read, Edit, Write, Glob, Grep
---

# Android Feature Completion Checklist

Run all quality checks before marking a feature complete. **If any step fails, fix the errors yourself and re-run until it passes.**

## Step 1: Code Formatting (KtLint)

```bash
./gradlew ktlintFormat
```

This auto-fixes formatting issues. Then verify no remaining violations:

```bash
./gradlew ktlintCheck
```

Fix any issues that `ktlintFormat` could not auto-fix. Key rules to check:
- Import ordering
- Indentation (4 spaces)
- Trailing commas
- Spacing
- Line length

## Step 2: Build Check

Ensure the project builds without errors:

```bash
./gradlew assembleDebug 2>&1 | grep -E "(error:|warning:|BUILD SUCCESSFUL|BUILD FAILED)"
```

All builds must succeed. Fix any compiler errors or warnings before proceeding.

Common build failures:
- Missing imports after adding new files
- Type mismatches in DataBinding expressions
- Unresolved references (usually missing Koin registration in `AppModules.kt`)
- SafeArgs generated class not found (check navigation graph argument names)

## Step 3: Unit Tests

Run the test suite:

```bash
./gradlew testDebugUnitTest
```

All tests must pass. If tests fail, fix them before proceeding.

Common test failures:
- Missing `Dispatchers.setMain(testDispatcher)` in `@Before`
- Missing `advanceUntilIdle()` after async operations (ViewModel init, `viewModelScope.launch`)
- Mock not configured before ViewModel is created (ViewModel calls use case in `init`)
- Using `invoke()` instead of `executeNow()` in `coEvery` stub

## Step 4: Android Lint

Run Android lint analysis:

```bash
./gradlew lintDebug
```

**If it fails:** Read the error output, fix the reported issues, and re-run until it passes.

Common fixes:
- Hardcoded strings → extract to `values/strings.xml`
- Deprecated API usage → replace with recommended alternative
- Missing `contentDescription` on ImageView → add `@string/...` description
- Missing permissions → add to `AndroidManifest.xml`
- Unused resources → remove them

## Step 5: Architecture Verification

Manually verify architecture rules:

### Layer Dependencies
- [ ] `presentation/` imports from `domain/` only — NOT from `data/` (no DTO imports in Fragments or ViewModels)
- [ ] ViewModels import Use Cases only — NOT Repositories or APIs
- [ ] Use Cases import Repository interfaces — NOT concrete impls or APIs
- [ ] Domain layer (`domain/entities/`, `domain/abstractions/`, `domain/usecases/`) has NO Android imports

### Naming Conventions
- [ ] ViewModels: `<Feature>ViewModel` extends `BaseViewModel`
- [ ] UiState: `<Feature>UiState` — data class with default values
- [ ] Use Cases: `<Action><Resource>UseCase` extends `BaseUseCase` in `domain/usecases/` (flat)
- [ ] Repository interface: `<Feature>Repository` in `domain/abstractions/`
- [ ] Repository impl: `<Feature>RepositoryImpl` in `data/repositories/`
- [ ] DTO: `<Resource>Dto` in `data/remote/dto/`
- [ ] Mapper: `fun <Resource>Dto.toDomain(): <Resource>` in `data/mappers/`
- [ ] APIs: `<Feature>Api` in `data/remote/api/`
- [ ] Fragments: `<Feature>Fragment` extends `BaseFragment`

### Koin Registration
- [ ] New ViewModels registered in `viewModels` module
- [ ] New Use Cases registered in `useCases` module
- [ ] New Repositories registered as interface binding: `single<XxxRepository> { XxxRepositoryImpl(get()) }`
- [ ] New APIs registered in `apiModule` via `ApiProvider.provideXxxApi()`

### Use Case Safety
- [ ] Every `run()` implementation catches `CancellationException` first and rethrows it before the general `Throwable` catch

### DataBinding
- [ ] Every feature layout wrapped in `<layout>` with `<data><variable name="viewModel" .../></data>`
- [ ] `binding.lifecycleOwner = viewLifecycleOwner` is set (done by BaseFragment — verify it's not overridden)
- [ ] Fragment sets `binding.viewModel = viewModel` in `onViewCreated`
- [ ] No hardcoded strings in XML layouts

## Step 6: Localization Check

Verify no hardcoded user-facing strings in XML:

```bash
grep -rn 'android:text="[^@{]' app/src/main/res/layout --include="*.xml" | grep -v 'tools:' | head -20
```

All user-facing strings must use `@string/key_name` (English only — `values/strings.xml`).

## Quick Summary Commands

Run all checks in sequence:

```bash
./gradlew ktlintFormat && \
./gradlew ktlintCheck && \
./gradlew assembleDebug 2>&1 | grep -E "(error:|BUILD SUCCESSFUL|BUILD FAILED)" && \
./gradlew testDebugUnitTest && \
echo "All checks passed" && \
./gradlew lintDebug
```

**On any failure: fix the errors → re-run that step → continue only when it passes.**

## Common Issues

### KtLint Violations
- Import ordering: Let `ktlintFormat` fix automatically
- Indentation: Use 4 spaces (not tabs)
- Trailing commas: Add where appropriate
- Wildcard imports: Replace with explicit imports

### Build Failures
- Missing imports: Add the required `import` statement
- Type mismatches in DataBinding: Check that the field type in UiState matches the binding adapter expected type
- Unresolved reference: Check `AppModules.kt` for missing Koin registration
- SafeArgs class not found: Clean + rebuild (`./gradlew clean assembleDebug`)

### Test Failures
- Missing `Dispatchers.setMain()` in setUp
- Missing `advanceUntilIdle()` after async operations
- ViewModel created before mock is configured (ViewModel calls use case in `init`)
- `CancellationException` test: verify the use case rethrows it

### Lint Failures
- Hardcoded string: move to `values/strings.xml`
- Missing `contentDescription`: add a descriptive `@string/` value
- `ObsoleteSdkInt`: remove unnecessary min-version checks (minSdk is 26)

## Final Checklist

- [ ] `./gradlew ktlintCheck` passes (after running `ktlintFormat`)
- [ ] `./gradlew assembleDebug` succeeds
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintDebug` passes
- [ ] No hardcoded strings in layouts
- [ ] Architecture layers respected (no DTO in presentation, no repo in ViewModel)
- [ ] All new code has tests (ViewModel, UseCase, RepositoryImpl, Mapper)
- [ ] All new components registered in `AppModules.kt`
- [ ] Repository registered with interface binding (`single<Interface> { Impl(get()) }`)
- [ ] CancellationException rethrown in every UseCase `run()` method
