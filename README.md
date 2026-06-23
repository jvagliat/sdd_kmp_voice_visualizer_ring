# KMP Voice Visualizer Ring

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20Desktop%20%7C%20Web%20%7C%20iOS-2ea44f.svg)](#stack)
[![Live Demo](https://img.shields.io/badge/Live-Demo-FF6F00.svg)](https://iattraxia.com/lab/ssd-kmp-voice-visualizer-ring/)

An audio-reactive visualizer component for Kotlin Multiplatform — 3 organic blobs forming a glowing ring that pulse and morph in sync with audio input.

Built with **Spec-Driven Development (SDD)** + **Claude Code** on a stack that LLMs don't dominate: **Kotlin Multiplatform / Compose Multiplatform**.

<img src="docs/demo_landscape.gif" width="100%" />

**Try it now (no install):** [https://iattraxia.com/lab/ssd-kmp-voice-visualizer-ring/](https://iattraxia.com/lab/ssd-kmp-voice-visualizer-ring/)

---

## Use It in Your App

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iattraxia.kmp_voice_ring.VoiceVisualizerRing

@Composable
fun MyScreen(volume: Float) {
    VoiceVisualizerRing(
        volume = volume,        // 0f..1f from your audio source (mic, file, FFT band)
        color = Color.Cyan,
        modifier = Modifier.size(300.dp),
    )
}
```

Optional parameters (with defaults): `intensity`, `thickness`, `glowSpread`, `blurRadius`, `inputSmoothing`, `responsiveness`, `layerFalloff`, `relativeMotion`, `lowPerformanceMode`.

---

## Why This Repo Is Different

Most "AI writes code" demos run on stacks that LLMs know inside out: React, Python, Node. This project tackles the harder case.

**Kotlin Multiplatform has low representation in LLM training data.** Claude Code doesn't "just know" how to build this. Without clear specs, it gets lost. With well-written specs, it moves fast. The method — not the model — is what makes the difference.

> The specs in this repo were formalized after the initial development to document the SDD process applied. The commit log reflects the actual progression; the specs reconstruct the reasoning behind each step. See [About This Project](#about-this-project) for the full context.

This repo documents that method in full:

- **13 task spec folders** (`docs/specs/`) with `plan.md`, `requirements.md`, and `validation.md` for every development phase — from scaffolding to final documentation.
- **A full spec constitution**: [`mission.md`](docs/specs/mission.md), [`techs-and-archi.md`](docs/specs/techs-and-archi.md), and [`roadmap.md`](docs/specs/roadmap.md) that governed the entire build.
- **A reusable SDD workflow** (`.claude/skills/sdd-workflow-skill/`) and templates (`docs/specs/_task_templates/`) so the process can be replicated on other projects.

### The Journey: 9 Versions to Get the Glow Right

The visual core of this project — the audio-reactive ring — went through **9 iterations** to faithfully reproduce the reference HTML prototype in Compose Canvas:

| Version | Approach | Why it didn't work |
|---------|----------|--------------------|
| V1 | Filled blobs + halo strokes | Onion ring — fills are discs, not rings |
| V2 | Hollow ring via clipPath | Hard bands — no gaussian falloff, visible steps |
| V3 | Modifier.blur + CSS-normalized radii | Filled center — blur on filled blobs stays solid |
| V4 | Modifier.blur on strokes, 3 canvases | Frozen halos on Android — hardware layer caching |
| V5 | Multi-pass strokes, no blur | Works everywhere, zero deps — archived as fallback |
| V6 | Cloudy library + `key(tick)` trick | First version with real gaussian glow that animates |
| V7 | Circular keyframes + relativeMotion | Archived — different visual direction |
| V8 | V6 + two-stage smoothing + dynamic glow | Organic motion matching the HTML prototype |
| **V9** | **V8 + parametric smoothing** | **Active version — adjustable in real-time** |

Full details in [`docs/research/bitacora_estrategias.md`](docs/research/bitacora_estrategias.md).

---

## Spec Structure

> Folders are ordered by task index, not date — some tasks ran in parallel, so dates don't always increase monotonically.

```
docs/specs/
├── mission.md                          # Project purpose and audience
├── techs-and-archi.md                  # Stack, architecture, key decisions
├── roadmap.md                          # 13 tasks across 7 phases
├── _task_templates/                    # Reusable templates for SDD workflow
│   ├── plan.md
│   ├── requirements.md
│   └── validation.md
├── 2026-04-10-0-scaffolding-kmp/       # KMP setup, target cleanup, audio dep
├── 2026-04-11-1-audio-infrastructure/  # WAV parser, PlayerViewModel, test harness
├── 2026-04-11-2-html-prototype-analysis/  # 13-effect breakdown of HTML prototype
├── 2026-04-11-3-visualizer-v1-to-v4/   # First iteration cycle (onion ring → frozen halos)
├── 2026-04-12-4-fake-blur-v5/          # Portable multi-pass glow without blur
├── 2026-04-12-5-cloudy-blur-v6/        # Real gaussian glow via Cloudy + key trick
├── 2026-04-12-6-circular-keyframes-v7/ # Archived circular keyframe experiment
├── 2026-04-13-7-smoothing-dynamic-glow-v8/  # Two-stage smoothing + dynamic brightness
├── 2026-04-12-8-fft-band-energy/       # FFT voice-band parser replicating AnalyserNode
├── 2026-04-13-9-smoothing-params-v9/   # Exposed smoothing as composable parameters
├── 2026-04-12-10-mobile-layout/        # Responsive mobile layout + color picker
├── 2026-04-13-11-wasmjs-audio/         # Web audio via HTMLAudioElement + Firebase deploy
└── 2026-04-13-12-documentation-cleanup/  # Experiments archive + API docs
```

---

## How It Was Built

| Tool | Role |
|------|------|
| **Spec-Driven Development** | Specs written before code. Constitution → task breakdown → plan/requirements/validation per task. |
| **Claude Code (Opus 4.7)** | Implementation engine. Directed by specs, not free-form prompting. |
| **Compose Multiplatform Skill** | Custom skill (`.claude/skills/jetpack-compose-expert-skill/`) providing Compose source code references to ground the model's output. |
| **SDD Workflow Skill** | Custom skill (`.claude/skills/sdd-workflow-skill/`) that automates the task iteration cycle. |

---

## Stack

| Target | Status |
|--------|--------|
| **Desktop** (JVM) | Audio + ring verified |
| **Android** | Ring verified, audio wired |
| **Web** (wasmJs) | Audio via HTMLAudioElement, deployed to Firebase Hosting |
| **iOS** | Code compatible, runtime validation pending |

**Key dependencies:** Compose Multiplatform, `kmp-audio-recorder-player`, `skydoves/Cloudy` (blur). The visualizer component itself uses only `androidx.compose.*` + `kotlin.math.*`.

---

## Build & Run

**Desktop (JVM):**
```shell
.\gradlew.bat :composeApp:run
```

**Android:**
```shell
.\gradlew.bat :composeApp:assembleDebug
```

**Web (wasmJs):**
```shell
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
```

**iOS:** Open `iosApp/` in Xcode (compiles, runtime not yet validated on device).

---

## About This Project

This project was developed using **Spec-Driven Development (SDD)**, directed via Claude Code with a Compose Multiplatform skill, in a short timeframe.

The specs in `docs/specs/` document the SDD process applied. They were formalized after the initial development to serve as a reference for how SDD works on a stack that LLMs don't dominate (Kotlin Multiplatform / Compose Multiplatform). The development history is honest — the commit log reflects the actual progression; the specs reconstruct the reasoning behind each step.

---

## Feedback & Contributions

Issues, questions and PRs are welcome — especially around applying SDD to stacks that LLMs don't dominate. If you replicate this approach on another project, I'd love to hear about it.

---

## License

[MIT](LICENSE) © Pablo Vagliati
