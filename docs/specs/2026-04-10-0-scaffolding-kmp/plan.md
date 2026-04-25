# Plan: Scaffolding KMP

## Group 1: Initial Import

1. Import KMP project from the Compose Multiplatform wizard template. Creates `composeApp/build.gradle.kts` with Android, iOS, JVM, js, wasmJs targets; `App.kt` with dummy Greeting; `Platform.kt` expect/actual per target; `libs.versions.toml`; Gradle wrapper; `iosApp/` Xcode project; `settings.gradle.kts` → `718e309`.

---

## Group 2: Legacy Cleanup

2. Drop the `js` target from `composeApp/build.gradle.kts`. Delete `composeApp/src/jsMain/` entirely. Move `composeApp/src/webMain/` to `composeApp/src/wasmJsMain/` (including `resources/index.html` and `styles.css`). Update the `main.kt` entry point inside the moved directory to reference wasmJs → `8ab2c2e`.

---

## Group 3: Audio Dependency

3. Add `io.github.hyochan:kmp-audio-recorder-player:1.0.0-alpha04` to `gradle/libs.versions.toml` and `composeApp/build.gradle.kts` commonMain dependencies → `6629bac`.
