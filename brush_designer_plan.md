# Brush Designer Implementation Plan

This document outlines the step-by-step plan and verification walkthroughs for implementing the Brush Designer feature in Cahier.

## Context
The Brush Designer allows users to tweak Ink brush properties. Due to internal API restrictions in `androidx.ink.brush`, we use Google Protobuf to build an `ink.proto.BrushFamily.Builder`, serialize it to a byte array, and decode it using the public `androidx.ink.brush.BrushFamily.decode(ByteArray)` API.

---

## Step 1: Gradle Configuration for Protobuf

### Plan
1.  **Update `gradle/libs.versions.toml`**: Add the Protobuf plugin and `protobuf-javalite` library versions.
2.  **Update `build.gradle.kts` (Project-level)**: Add the Protobuf plugin alias.
3.  **Update `app/build.gradle.kts`**:
    *   Apply the `com.google.protobuf` plugin.
    *   Add `protobuf-javalite` to the `dependencies` block.
    *   Add the `protobuf` configuration block to define the `protoc` artifact and configure the `generateProtoTasks` to use the `lite` plugin for Android.

### Verification Walkthrough
1.  Run `./gradlew clean build` to ensure the project compiles successfully.
2.  Verify that the Protobuf plugin generates Java Lite classes from the existing `brush_family.proto` and `color.proto` files (these classes should be available under `app/build/generated/source/proto/...`).
3.  In a test or scratch file, attempt to import and use `ink.proto.BrushFamily.newBuilder()` to ensure the generated classes are accessible to the Kotlin compiler.

---

## Step 2: The ViewModel (`BrushDesignerViewModel`)

### Plan
1.  Create `BrushDesignerViewModel.kt` annotated with `@HiltViewModel`.
2.  **State Management**:
    *   Create a `MutableStateFlow` to hold the raw Protobuf state: `ink.proto.BrushFamily.Builder` or `ink.proto.BrushFamily`. Initialized to `ink.proto.BrushFamily.getDefaultInstance()`.
3.  **Transformation (The Workaround)**:
    *   Create a `StateFlow<androidx.ink.brush.BrushFamily?>` named `previewBrushFamily`.
    *   Use `map` on the proto state flow: convert the proto to a `ByteArray` (`proto.toByteArray()`), then call `androidx.ink.brush.BrushFamily.decode(bytes)`.
    *   Wrap the decoding in a `try-catch` block. If an `IllegalArgumentException` or `InvalidProtocolBufferException` occurs (due to incomplete configuration), emit `null`.
4.  **UI Interactors**:
    *   Provide `fun updateBrushProto(updateBlock: (ink.proto.BrushFamily.Builder) -> Unit)` to safely mutate the proto state and emit the new version.
5.  **Import/Export**:
    *   `saveBrushToFile(uri: Uri)`: Use `ContentResolver.openOutputStream(uri)` to write `proto.toByteArray()`.
    *   `loadBrushFromFile(uri: Uri)`: Use `ContentResolver.openInputStream(uri)` to read bytes, then `ink.proto.BrushFamily.parseFrom(bytes)`, and update the state.

### Verification Walkthrough
1.  Write a unit test for `BrushDesignerViewModel`.
2.  Verify that calling `updateBrushProto` with valid parameters causes `previewBrushFamily` to emit a non-null `androidx.ink.brush.BrushFamily`.
3.  Verify that setting an invalid configuration in the proto emits `null` in `previewBrushFamily` without crashing the app.
4.  Test `saveBrushToFile` and `loadBrushFromFile` using mocked URIs/Streams to ensure byte serialization works correctly.

---

## Step 3: The Compose UI Layout (`BrushDesignerScreen`)

### Plan
1.  Create `BrushDesignerScreen.kt`.
2.  **Layout Structure**: Use a `Row` to split the screen for tablet/large-screen optimization.
3.  **Left Pane (Controls - `Modifier.weight(0.35f)`)**:
    *   Use a `Column` with `verticalScroll`.
    *   Top app bar with a close button and screen title.
    *   Add placeholder UI controls (Sliders, Dropdowns) that map to basic brush properties (e.g., size, epsilon, coats). *Note: Full UI mapping of all proto fields can be complex, so we will start with core placeholders to establish the flow.*
    *   Add "Import" and "Export" buttons. These will trigger `rememberLauncherForActivityResult` with `ActivityResultContracts.CreateDocument` and `OpenDocument` (mime: `application/octet-stream`).
4.  **Right Pane (Preview - `Modifier.weight(0.65f)`)**:
    *   Use a `Box` with a border to define the preview area.
    *   Collect `previewBrushFamily` from the ViewModel.
    *   If `null`, show a `Text` warning: "Invalid Brush Configuration".
    *   If not null, create a `Brush` using `Brush.createWithColorIntARGB(...)` and the decoded family.
    *   Pass the `Brush` to a `DrawingSurface` composable configured to fill the Box, allowing the user to draw and test the live brush.

### Verification Walkthrough
1.  Launch the app and navigate to the Brush Designer screen.
2.  Verify the side-by-side layout renders correctly.
3.  Use the left pane controls to modify a property.
4.  Observe the right pane: drawing on the `DrawingSurface` should instantly reflect the new brush characteristics.
5.  Set a control to an extreme/invalid value (if possible) and verify the "Invalid Brush Configuration" overlay appears without crashing the app.
6.  Click "Export", save the `.brush` file to device storage.
7.  Change the brush settings, click "Import", select the saved file, and verify the settings revert to the exported state.

---

## Step 4: Navigation Integration

### Plan
1.  Update `NavigationDestination.kt` with a new object `BrushDesignerDestination`.
2.  Update `CahierNavGraph.kt`.
3.  Add a `composable(route = BrushDesignerDestination.route)` block.
4.  Inside the block, use `hiltViewModel<BrushDesignerViewModel>()` to instantiate the ViewModel.
5.  Call `BrushDesignerScreen(viewModel = viewModel, onClose = { navController.popBackStack() })`.
6.  Add an entry point (e.g., an Icon or Menu Item) in `SettingsScreen` or `CahierHomeScreen` to navigate to this new route.

### Verification Walkthrough
1.  Launch the app.
2.  Navigate to the designated entry point (e.g., Settings menu).
3.  Click the "Brush Designer" button.
4.  Verify the app successfully transitions to the `BrushDesignerScreen` and that the `ViewModel` is correctly injected and operational.
5.  Click the "Close" button in the TopAppBar and verify it correctly navigates back to the previous screen.