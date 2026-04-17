---
name: adr-review-guidelines
description: >
  Audits Android code samples and codelabs against ADR MAD (Modern Android Development)
  review guidelines before launch. Use when reviewing a PR or feature branch for
  compliance with Architecture, Compose, Large Screens, Camera/Media, and Build standards.
  Produces a structured report with Block / Needs SLO / Advisory findings per issue.
license: Proprietary
metadata:
  author: android/adr
  source: go/mad-arch-review-governance
  status: Approved
---

# ADR Samples Review Guidelines

Audits code against the ADR MAD review rubric. For each finding, classify it as:

- **Block** — must be fixed before launch
- **Needs SLO** — prefer to fix before launch; may launch with justification + agreed date
- **Advisory** — may launch without fixing

## How to Use This Skill

1. Identify the scope: **Application sample** (large, repo-level) or **Feature sample** (single screen / codelab).
2. Walk each guideline table below and flag any violations.
3. Produce a report grouped by section with a **Launch readiness** status: **Ready** or **Not ready**.
4. For each **Block** or **Needs SLO** finding, note the file, line (if available), and recommended fix.

See [references/process.md](references/process.md) for the full review process, terminology, and action steps.

---

## Guidelines: Architecture & Testing

Based on [external recommendations](https://developer.android.com/topic/architecture/recommendations) and [internal best practices](http://go/architecture-devrel-best-practices).

| Issue | Type | App sample | Feature sample |
| ----- | ----- | :---: | :---: |
| **Layered architecture** | | | |
| Use a clearly defined [data layer](https://developer.android.com/jetpack/guide/data-layer). | Maintainability | **Block** | Advisory |
| Use a clearly defined [UI layer](https://developer.android.com/jetpack/guide/ui-layer). | Maintainability | **Block** | Advisory |
| The data layer should expose application data using a repository. | Best practices | **Block** | Advisory |
| Use coroutines and flows for async work. | Best practices | **Block** | Advisory |
| Use a domain layer. | Maintainability | Advisory | Advisory |
| **UI layer** | | | |
| Follow [Unidirectional Data Flow (UDF)](https://developer.android.com/jetpack/compose/architecture#udf). | Best practices | **Block** | **Block** |
| Use AAC ViewModels if their benefits apply. | Best practices | **Block** | Needs SLO |
| Use lifecycle-aware UI state collection (`collectAsStateWithLifecycle`). | Best practices | **Block** | **Block** |
| Do not send events from the ViewModel to the UI. | Best practices | **Block** | Needs SLO |
| Use a single-activity application with Jetpack/Compose Navigation. | Best practices | **Block** | Advisory |
| Use [Jetpack Compose](https://developer.android.com/jetpack/compose). | ADR priorities | Needs SLO | Needs SLO |
| **ViewModel** | | | |
| ViewModels should be agnostic of the Android lifecycle (no Android imports). | Best practices | **Block** | Needs SLO |
| Use coroutines and flows in ViewModels. | Best practices | **Block** | Advisory |
| Use ViewModels at screen level only. | Best practices | **Block** | Advisory |
| Use plain state holder classes in reusable UI components. | Best practices | **Block** | Advisory |
| Do not use `AndroidViewModel`. | Maintainability | Needs SLO | Advisory |
| Expose a UI state from the ViewModel. | Best practices | Needs SLO | Advisory |
| **Lifecycle** | | | |
| Do not override lifecycle methods in Activities or Fragments. | Best practices | **Block** | Advisory |
| **Dependency injection** | | | |
| Use [dependency injection](https://developer.android.com/training/dependency-injection). | Maintainability | **Block** | Advisory |
| Scope to a component when necessary. | Best practices | Needs SLO | Advisory |
| Use [Hilt](https://developer.android.com/training/dependency-injection/hilt-android). | Maintainability | Needs SLO | Advisory |
| **Testing** | | | |
| Know what to test. | Maintainability | **Block** | Advisory |
| Prefer fakes to mocks. | Best practices | **Block** | Advisory |
| Test StateFlows. | Maintainability | **Block** | Advisory |
| Repositories are interfaces. | Maintainability | Needs SLO | Advisory |
| Clear test names: `testSubject_condition_expectedResult` (no spaces/backticks). | Best practices | Needs SLO | Advisory |
| **Models** | | | |
| Create a model per layer in complex apps. | Maintainability | Needs SLO | *(N/A)* |
| **Naming conventions** | | | |
| Method names | Best practices | Needs SLO | Needs SLO |
| Property names | Best practices | Needs SLO | Needs SLO |
| Streams of data | Best practices | Needs SLO | Needs SLO |
| Interface implementations | Best practices | Needs SLO | Needs SLO |

---

## Guidelines: Compose

| Issue | Type | App sample | Feature sample |
| ----- | ----- | :---: | :---: |
| **General** | | | |
| A `@Composable` returning `Unit` must be PascalCase and a noun. | Best practices | **Block** | **Block** |
| `@Composable` functions either emit content OR return a value, not both. | Best practices | **Block** | **Block** |
| Single Activity; use Compose Navigation for more than one screen. | Best practices | **Block** | **Block** |
| Prefer `ComponentActivity` over `AppCompatActivity` when possible. | Best practices | Needs SLO | Needs SLO |
| `remember{}`-backed functions that return a mutable object must be prefixed `remember`. | Best practices | Needs SLO | Needs SLO |
| Use `@Preview` composables. | Best practices | Needs SLO | Needs SLO |
| Separate `@Preview`s from composable implementation files. | Best practices | Advisory | Advisory |
| Use trailing lambda syntax for `content` parameters. | Best practices | Advisory | Advisory |
| Use `items {}` instead of `for` loops in lazy lists. | Performance | Advisory | Advisory |
| Break up large composables into smaller sub-composables. | Maintainability | **Block** | **Block** |
| Compose code handles UI; state-holder classes handle logic. | Maintainability | **Block** | **Block** |
| Composable names describe what they render (e.g. `DeleteButton` not `Button`). | Maintainability | **Block** | **Block** |
| Split large files — move unrelated functions/classes to separate files. | Maintainability | **Block** | **Block** |
| Value-returning composables use lowercase (standard Kotlin conventions). | Maintainability | **Block** | **Block** |
| Application supports [edge-to-edge display](https://developer.android.com/develop/ui/views/layout/edge-to-edge). | Best practices | **Block** | Advisory |
| **State** | | | |
| Screen-level composables separated into stateless and stateful versions. | Maintainability | **Block** | Advisory |
| Do not pass mutable objects as parameters. | Best practices | **Block** | **Block** |
| Do not mutate state in composable scope — only in event handlers or side effects. | Best practices | **Block** | **Block** |
| Do not pass Compose state objects as parameters. | Best practices | **Block** | **Block** |
| Defer state reads with lambdas for frequently-changing state (animation, scroll). | Performance | **Block** | **Block** |
| Collect flows with `collectAsStateWithLifecycle` (not `collectAsState`). | Performance | Needs SLO | Needs SLO |
| Use `by` keyword with State delegates. | Fit and finish | Advisory | Advisory |
| **Material 3** | | | |
| App has a Material theme from [Material Theme Builder](https://material-foundation.github.io/material-theme-builder/). | Best practices | **Block** | Advisory |
| Colors sourced from `Color.kt` — no hex codes or constants in UI code. | Best practices | **Block** | **Block** |
| All components are Material 3 unless creating a custom component. | Best practices | **Block** | **Block** |
| App theme set in `setContent` of `MainActivity.kt`. | Best practices | **Block** | **Block** |
| Avoid custom colors outside the generated Material Theme. | Best practices | Needs SLO | Needs SLO |
| **Layout** | | | |
| `Column`/`Row` only used with multiple children. | Best practices | **Block** | Advisory |
| Each composable emits one top-level composable at the root. | Best practices | **Block** | **Block** |
| Avoid `ConstraintLayout` unless unavoidable. | Performance | Needs SLO | Needs SLO |
| **Parameters** | | | |
| Parameter order: required → `Modifier` → optional → trailing `content` lambda. | Best practices | **Block** | **Block** |
| Provide default values for styling/customization parameters. | Best practices | **Block** | **Block** |
| **Modifiers** | | | |
| Every UI-emitting composable has a `modifier: Modifier = Modifier` parameter. | Best practices | **Block** | **Block** |
| The passed-in `Modifier` is applied only to the outermost layout element. | Best practices | **Block** | **Block** |
| `Modifier` is the first optional parameter. | Best practices | **Block** | **Block** |

---

## Guidelines: Large Screens

Based on [large screen quality guidelines](https://developer.android.com/docs/quality-guidelines/large-screen-app-quality).

| Issue | Type | App sample | Feature sample |
| ----- | ----- | :---: | :---: |
| **Orientation & resizability** | | | |
| Do not lock orientation in the manifest. | Best practices | **Block** | **Block** |
| Do not lock orientation at runtime (Camera viewfinder exception applies). | Best practices | **Block** | **Block** |
| Do not restrict Activity aspect ratio (`maxAspectRatio`, `minAspectRatio`). | Best practices | **Block** | **Block** |
| Every Activity must be resizable. | Best practices | **Block** | **Block** |
| App state preserved through configuration changes. | Best practices | **Block** | **Block** |
| **Input** | | | |
| All interactive components are focusable. | Best practices | Needs SLO | Advisory |
| Interactive components visually indicate keyboard focus. | Best practices | Needs SLO | Advisory |
| Keyboard focus moves with Tab/arrow keys in z-order. | Best practices | Needs SLO | Advisory |
| Support mouse wheel scrolling. | Best practices | Needs SLO | Advisory |
| Support hover states (trackpad, mouse, stylus) with correct visual treatment. | Best practices | Needs SLO | Advisory |
| Support stylus handwriting input in text fields. | ADR priorities | Advisory | Advisory |
| Support common hardware keyboard shortcuts. | ADR priorities | Advisory | Advisory |
| Support cross-app drag-and-drop for images and content. | ADR priorities | Advisory | Advisory |
| **Adaptive layouts** | | | |
| Adapt navigation UI by window size (e.g. `NavigationSuiteScaffold`). | Fit and finish | Needs SLO | Advisory |
| Text fields, buttons, and bottom sheets do not fill max width on large screens. | Fit and finish | Needs SLO | Advisory |
| Use grids for large scrolling feeds. | Fit and finish | Advisory | Advisory |
| Use two-pane layouts for list/detail UI (e.g. `ListDetailPaneScaffold`). | Fit and finish | Advisory | Advisory |
| Use `WindowManager WindowSizeClass` types over Material 3. | ADR priorities | Needs SLO | Needs SLO |

---

## Guidelines: Camera / Media

| Issue | Type | App sample | Feature sample |
| ----- | ----- | :---: | :---: |
| **Preview** | | | |
| Use CameraX. | Best practices | Advisory | Advisory |
| Camera preview is not distorted or upside down. | Fit and finish | **Block** | **Block** |
| Camera previews are not letterboxed. | Fit and finish | **Block** | **Block** |
| **Large screens** | | | |
| Correctly handle multi-resume with exclusive resources. | Best practices | Needs SLO | Advisory |
| Camera supported in multi-window and free-form windowing modes. | Best practices | Needs SLO | Advisory |
| Media projection supported in multi-window and free-form windowing modes. | Best practices | **Block** | **Block** |
| Support dual display capture on foldable devices. | ADR priorities | Advisory | Advisory |
| Support tabletop capture on foldable devices. | ADR priorities | Advisory | Advisory |
| Support tabletop playback/consumption on foldable devices. | ADR priorities | Advisory | Advisory |

---

## Guidelines: Build

See [App Build Best Practices and Common Mistakes](https://docs.google.com/document/d/1HWUNYyUyLMtlZCXjXODU9Bo53kh54sjMx1wOBWeOOsA/) for full details.

| Issue | Type | App sample | Feature sample |
| ----- | ----- | :---: | :---: |
| Write build scripts in KTS. | Best practices | **Block** | **Block** |
| Use version catalog (`libs.versions.toml`). | Best practices | **Block** | **Block** |
| Add Kotlin files to the Java source folder. | Best practices | **Block** | **Block** |
| Only apply Google/Gradle/JetBrains plugins. | Best practices | **Block** | **Block** |
| Set proper JDK. | Best practices | **Block** | Advisory |
| Document build steps. | Best practices | Needs SLO | Advisory |
| Use a Gradle check task for pre-commit hooks. | Best practices | Advisory | Advisory |
| Root `build.gradle.kts` is empty or contains only `apply false` plugins. | Best practices | **Block** | Advisory |
| Submodule `build.gradle.kts` contains only plugins, extensions, and dependencies. | Best practices | **Block** | **Block** |
| Use `sourceCompat`, `targetCompat`, and `jvmTarget` instead of toolchain. | Best practices | **Block** | Advisory |
| Prefer KSP over kapt/apt. | Best practices | **Block** | **Block** |
| Do not use unstable AGP on `main`. | Best practices | **Block** | **Block** |
| Do not use `+` on dependency versions. | Best practices | **Block** | **Block** |
| Prefer `implementation` over `api` configuration. | Best practices | **Block** | **Block** |
| Prefer `@OptIn` annotations over compiler opt-in options. | Best practices | Needs SLO | Advisory |
| Do not use `buildscript` block. | Best practices | **Block** | **Block** |
| Ensure project isolation. | Best practices | **Block** | Advisory |
| Do not use `allprojects` or `subprojects`. | Best practices | **Block** | **Block** |
| Do not use `ext` block. | Best practices | **Block** | **Block** |
| Do not call private AGP APIs. | Best practices | **Block** | **Block** |
| Do not use a different directory name from project name. | Best practices | **Block** | **Block** |
| Do not use Gradle dependency helpers. | Best practices | **Block** | **Block** |
| Consider the settings plugin for Android/AGP config. | Best practices | Advisory | Advisory |
| Ensure correct `settings.gradle.kts` content. | Best practices | **Block** | **Block** |
| Declare repositories only in `settings.gradle.kts`. | Best practices | **Block** | **Block** |
| Download snapshot artifacts first. | Best practices | **Block** | **Block** |
| Enable caching, parallel execution, and configuration cache. | Best practices | **Block** | Advisory |
| Do not enable configure-on-demand. | Best practices | **Block** | **Block** |
| Do not work around `minSdk`. | Best practices | **Block** | **Block** |
| Always use binary Gradle distribution. | Best practices | **Block** | **Block** |
| Apply spotless only from init script. | Best practices | **Block** | **Block** |
| Do not write plugins inside Gradle scripts. | Best practices | **Block** | **Block** |
| Use convention plugins instead of `buildSrc` or custom tasks. | Best practices | **Block** | **Block** |
| Use GitHub Actions to build on `push:`, `pull_request:`, and `workflow_dispatch:` with `./gradlew build --stacktrace`. | Best practices | **Block** | **Block** |
| Use GitHub Actions to run `./gradlew spotlessCheck --stacktrace`. | Best practices | **Block** | **Block** |
