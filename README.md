# Cahier: Modern Android Productivity Sample

Cahier is a feature-rich, offline-first note-taking application built to showcase modern Android
development best practices using Kotlin, Jetpack Compose, Material 3, and a suite of Jetpack
libraries like the Ink API. It serves as a hero sample for building robust, adaptive, and engaging
productivity applications on Android.

## About The Sample

Cahier ("notebook" in French) allows users to capture and organize their thoughts through text
notes, drawings, and image attachments. This sample demonstrates how to build a high-quality
Android application leveraging the latest technologies for a seamless user experience across
various devices and form factors.

It is designed to be a learning resource for developers looking to understand and implement:
* Modern UI development with Jetpack Compose.
* Responsive and adaptive layouts for different screen sizes.
* Advanced features like digital ink with the Android Jetpack Ink API.
* Offline-first architecture with Room.
* Clean architecture principles with MVVM and Hilt.
* Integration with the Android ecosystem (e.g. Notes Role, etc...).

## Features

* **Versatile Note Creation:**
    * Create notes with rich text.
    * Freehand drawing and sketching with various brushes, colors, and an eraser tool.
  * **Image Attachments:** 
    * Add images from the device gallery to your notes.
* **Note Organization:**
    * Mark notes as favorites for quick access.
    * View all notes, or filter by favorites.
  * **Offline First:**
    * All notes are saved locally, making the app fully functional without an internet connection.
  * **Adaptive UI:** 
    * The user interface adapts to different screen sizes and orientations, providing an optimal
    experience on phones and tablets using `ListDetailPaneScaffold`.
* **Material Design 3:**
    * Modern Material 3 components and theming.
    * Support for Dynamic Color (Material You) on Android 12+.
    * Dark theme support.
* **Productivity Integrations:**
    * Ability to be set as the default notes app (Android 14+).
    * Responds to the `Notes` intent for integration with the system.
* **Ink Features:**
    * Integrates Ink API for the best latency and performance.
    * Undo/Redo functionality for drawings.
    * Variety of stock brushes (pen, marker, highlighter, dashed line).
    * Color picker for brush customization.

## Tech Stack & Key APIs

Cahier is built with a focus on modern Android development:

* **Language:** [Kotlin](https://kotlinlang.org/) (100%)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Digital Ink:** [Android Ink API (`androidx.ink`)](https://developer.android.com/jetpack/androidx/releases/ink)
* **Dependency Injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
* **Asynchronous Programming:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
* & [Flow](https://kotlinlang.org/docs/flow.html)
* **Database:** [Room Persistence Library](https://developer.android.com/jetpack/androidx/releases/room)
* **Navigation:** [Jetpack Navigation for Compose](https://developer.android.com/jetpack/compose/navigation)
* **Adaptive Layouts:** [Material 3 Adaptive Layouts](https://m3.material.io/foundations/layout/applying-layout/overview)
* **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
* **Serialization:** [Gson](https://github.com/google/gson)
* **System Integration:**
    * [RoleManager API](https://developer.android.com/reference/android/app/role/RoleManager)
    * [ActivityResultContracts](https://developer.android.com/training/basics/intents/result)

## Getting Started

### Prerequisites

* Android Studio (latest stable version recommended)
* Android SDK corresponding to `compileSdk = 35` and `minSdk = 26`
* Gradle version specified in `gradle-wrapper.properties` (currently 8.11.1)

### Installation

1.  Clone the repository:
    ```sh
    git clone https://github.com/android/cahier
    ```
2.  Open the project in Android Studio.
3.  Let Android Studio sync Gradle dependencies.
4.  Run the app on an Android device or emulator (API 26+). For testing the Notes Role feature, 
use a device/emulator running Android 14 (API 34) or higher.

## Code Highlights & Best Practices

This project aims to showcase various best practices for modern Android development:

* **Kotlin-First:** Leveraging the full power of Kotlin, including coroutines, Flow, data classes,
and sealed classes (implicitly).
* **Declarative UI with Jetpack Compose:** Building the entire UI with Compose, emphasizing
state-driven UI and reusability.
* **Modular Architecture (MVVM):** Clear separation of concerns between UI (Composable functions), 
ViewModels (handling UI logic and state), Repositories (data abstraction), and data sources
(Room Database).
* **Dependency Injection with Hilt:** Simplifying dependency management and improving code
testability.
* **Offline-First:** Using Room to ensure data is always available locally.
* **Responsive & Adaptive Design:** Employing Material 3 adaptive components like
`ListDetailPaneScaffold` to provide an optimal layout on various screen sizes.
* **State Management:** Using `StateFlow` and `collectAsStateWithLifecycle()` for managing and
* observing UI state in a reactive way.
* **Navigation Graph:** Defining clear navigation paths using Jetpack Navigation for Compose.
* **Material Design 3:** Implementing the latest Material Design guidelines, components, and
theming (including dynamic color).
* **Handling Custom Types in Room:** Using `TypeConverters` to store complex objects like Ink
`Stroke` data and `List<String>` in the Room database.
* **Android Ink API Integration:** Demonstrates setup and usage of `InProgressStrokesView`
for capturing ink, `CanvasStrokeRenderer` for displaying strokes, and managing brush properties.
* **System Integration:** Showing how an app can register for system roles
(like the Notes role) and respond to system intents (`Notes`).


## Contributing

See [Contributing](CONTRIBUTING.md).

## License

Cahier is licensed under the [Apache License 2.0](LICENSE). See the `LICENSE` file for
details.
