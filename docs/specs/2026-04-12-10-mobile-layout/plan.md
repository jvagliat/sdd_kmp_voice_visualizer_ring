# Plan: Mobile Layout + Platform Detection

## Group 1: Platform Detection

1. Add `expect fun isMobilePlatform(): Boolean` to `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/Platform.kt`. Implement actuals:
   - `Platform.android.kt` → `true`
   - `Platform.ios.kt` → `true`
   - `Platform.jvm.kt` → `false`
   - `Platform.wasmJs.kt` → `false`

2. Add `material-icons-extended` dependency to `composeApp/build.gradle.kts` for mobile layout icons.

---

## Group 2: Mobile Layout

3. Modify `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/App.kt`:
   - Use `BoxWithConstraints` to detect mobile (when `isMobilePlatform()` is true or aspect ratio is portrait).
   - Branch between `MobileLayout` and `DesktopLayout`:
     - **MobileLayout:** ring expanded to fill width, 2×3 metrics grid below, icon bar (Source, Effects, Palette), bottom sheets for source/effects/color.
     - **DesktopLayout:** existing layout with side panels at `TopEnd`/`BottomEnd` preserved unchanged.
   - Apply `safeDrawingPadding` to avoid overlap with system bars (status bar, navigation bar).

---

## Group 3: Color Picker

4. Add `ColorChipRow` composable with 5 preset colors:
   - Ámbar (amber/gold)
   - Cian (cyan, original)
   - Menta (mint green)
   - Violeta (violet/lavender)
   - Hielo (ice blue)
   - Desktop: chips below sliders in DebugEffectsPanel; source/band-energy moved to source icon bottom sheet.
   - Mobile: Palette icon in icon bar opens sheet with color chips.
   - `accentColor` derived from selected preset and propagated to all UI elements: FPS texts, sliders, icons, volume bar, switch, buttons.

---

## Group 4: Android Wiring

5. Create `composeApp/src/androidMain/kotlin/com/iattraxia/kmp_voice_ring/platform/AppContextHolder.kt` — holds application context reference.

6. Modify `composeApp/src/androidMain/kotlin/com/iattraxia/kmp_voice_ring/MainActivity.kt`:
   - Initialize `AppContextHolder` with application context in `onCreate`.
   - Initialize `kmp-audio-recorder-player` library.

7. Modify `composeApp/src/androidMain/kotlin/com/iattraxia/kmp_voice_ring/platform/CacheFile.android.kt` — implement `writeBytesToCache` using `AppContextHolder.context.cacheDir` for real file I/O on Android (replaces stub).

---

## Commits

- `f772a01` — isMobilePlatform expect/actual (5 platform files)
- `c2dfb89` — materialIconsExtended dep
- `affcddf` — MobileLayout + DesktopLayout in App.kt (327 additions)
- `11e7ae9` — Color picker with 5 presets, accentColor propagation
- `19d9bcb` — Android: initialize kmp-audio-recorder-player in MainActivity
- `3320e28` — Android: writeBytesToCache via AppContextHolder
- `a0980b1` — safeDrawingPadding for system bars
