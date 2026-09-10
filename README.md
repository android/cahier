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
        * All notes are saved locally, making the app fully functional without an internet
          connection.
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
    * Create your own custom brushes with the interactive Brush Designer UI. See below for
      a [guide](#brush-designer).

## Brush Designer

Cahier comes with an interactive, visual-scripting brush designer for creating custom brush
families which can be used in apps using AndroidX Ink.

### Getting Started

* Open Cahier, and on the left-hand side of the screen, navigate to "Settings".
* Under the section marked "Developer Tools", look for the "Ink Brush Designer".
* Press "Launch" to open Brush Designer.

The best way to learn how to use Brush Designer is through experience, which is why we recommend
starting with the in-app [interactive tutorial](#interactive-tutorial).

### Fundamentals

##### Graph View

In Brush Designer, all the pieces of a `BrushFamily` are represented as *nodes* in a *graph*.
Those nodes can be dragged around the screen. They can be connected via their *ports*
(the dots on either side of the node) by dragging from one port to another to create an *edge*. You
can pinch to zoom in/out, and pan around the nodes in this view.

##### Inspector Pane

Tapping on a node opens the *inspector* pane. Here you can modify individual fields on the part of
the `BrushFamily` represented by the selected node.

##### Test Canvas

At the bottom of the screen, is a canvas which you can draw on using the `BrushFamily` you are
designing. You can change the color and size of your `Brush`. You can clear the canvas, invert the
background color on the canvas, and configure whether the canvas will auto-update the `BrushFamily`
of all the strokes as you edit fields in the graph view. The size of the canvas can be adjusted by
dragging up and down on the top bar of the canvas, and it can be collapsed by tapping the top bar.

##### Notification Pane

At times, Brush Designer may want to notify you of something. To show you this, an icon will appear
in the upper right corner of the screen. If you tap it, it will open the *notification pane* where
the inspector usually is. Here, you can view the details of the notifications. There are three
notification severities:

* Debug
    * Represented by a gray "i" in a circle icon.
    * These messages convey general information helpful while debugging, such as "BrushFamily was
      successfully imported".
* Warning
    * Represented by a yellow "!" in a triangle icon.
    * These messages convey non-breaking antipatterns identified with your current brush,
      such as unused nodes in the graph view, or a `BrushBehavior` incompatible with a `SelfOverlap`
      setting in the `BrushPaint`.
* Error
    * Represented by a red "!" in a circle icon.
    * These messages convey breaking errors identified with your current brush, which are preventing
      it from passing validation, such as an invalid range on a `SourceNode`.
      A `BrushFamily` must pass validation in order to be used in the test canvas or exported.

Error and Warning severity notifications will also appear along the top bar of the test canvas, for
improved visibility.

##### Menu

In the top left corner of the screen is a floating action bar. From left to right, there is a
back arrow to exit Brush Designer and return to Cahier. A three-dots button to open the menu. A
"My Brushes" button to show brush families you've saved to this device. And a "Save" button, to save
the current `BrushFamily` to "My Brushes".

Inside the menu, there are many helpful features:

* *Select*: enter selection mode to select multiple nodes, move them together, duplicate them, or
  delete all of them.
* *Tutorial*: an interactive tutorial to help you learn more about Brush Designer. See more
  [below](#interactive-tutorial).
* *Export*: create a `.brushfamily` file representing the current `BrushFamily` you are designing.
  This is how to take your `BrushFamily` creations out of Brush Designer and into your app.
* *Import*: select a `.brushfamily` file to edit in Brush Designer.
* *Organize*: applies the organization algorithm to the nodes in the graph view to rearrange their
  positions.
* *Templates*: a collection of premade brush families to be used as examples or starting points for
  design. **WARNING: this will replace your current `BrushFamily`.**
* *Delete Brush*: deletes the current brush. **WARNING: No undo!**
* *Options*: configure options for the Brush Designer itself.
* *Feedback*: report bugs, request features, or just tell us what you think!

#### Interactive Tutorial

Try out the in-app tutorial to learn the structure of brush families,
and how to use Brush Designer to create them.

* Click the three dots in the upper left-hand corner to the left of "My Brushes".
* Click "Tutorial".
* Click "Start" to begin.
* Follow the instructions of the tutorial. The tutorial should advance automatically as you perform
  the instructions, but you can also advance/regress the tutorial with the buttons "Next"/"Got It"
  and "Back".
* To exit the tutorial early, click the "X" in the upper right-hand corner of the tutorial pane.

#### Templates

Another good way to get started is to explore one of the built-in brush templates. These are also
useful as a starting place for design. To open a template:

* Click the three dots in the upper left-hand corner to the left of "My Brushes".
* Click "Templates".
* Select the desired template. **WARNING: this will replace your current `BrushFamily`.**

### AI Brush Designer CLI

Cahier also has a CLI available to generate custom brush families with the help of AI. This
experience is not yet available solely in the app. The CLI can send generated brush families to the
app on your Android device using ADB though, and import them right into Brush Designer for you to
test and edit. Here's how to get started:

* Connect your Android device to your computer using your cable.
* Establish a connection between your computer and your Android device. This can be tested by
  running `adb devices` on the command line.
* Ensure you have Cahier installed on the Android device.
* Get a Gemini API key. If you don't have one, go to https://ai.google.dev/gemini-api/docs/api-key
  to create one.
* Run export `GOOGLE_API_KEY=<insert_api_key_here>` to put the API key in your environment.
* Compile AIBD with `./gradlew :aibd:cli:installDist`
* Run it with `./aibd/cli/build/install/cli/bin/cli`
* Type in your brush description and hit enter to send it to Gemini.
* Once Gemini finishes generating a custom `BrushFamily`, it should automatically launch the Cahier
  app with the custom `BrushFamily` loaded.
* Test out the brush in the app, and reprompt Gemini as needed to improve your custom `BrushFamily`.
* For advanced usage, including conversation management and multi-modal inputs, type `/help` for a
  list of valid commands.

## Tech Stack & Key APIs

Cahier is built with a focus on modern Android development:

* **Language:** [Kotlin](https://kotlinlang.org/) (100%)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Digital Ink:** [Android Ink API (
  `androidx.ink`)](https://developer.android.com/jetpack/androidx/releases/ink)
* **Dependency Injection:
  ** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
* **Asynchronous Programming:
  ** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
* & [Flow](https://kotlinlang.org/docs/flow.html)
* **Database:
  ** [Room Persistence Library](https://developer.android.com/jetpack/androidx/releases/room)
* **Navigation:
  ** [Jetpack Navigation for Compose](https://developer.android.com/jetpack/compose/navigation)
* **Adaptive Layouts:
  ** [Material 3 Adaptive Layouts](https://m3.material.io/foundations/layout/applying-layout/overview)
* **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
* **Serialization:** [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
* **System Integration:**
    * [RoleManager API](https://developer.android.com/reference/android/app/role/RoleManager)
    * [ActivityResultContracts](https://developer.android.com/training/basics/intents/result)

## Getting Started

### Prerequisites

* Android Studio (latest stable version recommended)
* Android SDK corresponding to `compileSdk = 35` and `minSdk = 26`
* Gradle version specified in `gradle-wrapper.properties` (currently 8.11.1)

### Installation

1. Clone the repository:
   ```sh
   git clone https://github.com/android/cahier
   ```
2. Open the project in Android Studio.
3. Let Android Studio sync Gradle dependencies.
4. Run the app on an Android device or emulator (API 26+). For testing the Notes Role feature,
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
