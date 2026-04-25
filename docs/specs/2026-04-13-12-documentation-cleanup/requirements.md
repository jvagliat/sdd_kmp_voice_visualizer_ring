# Requirements: Documentation Cleanup

## Scope

Prepare the repository for client delivery by organizing obsolete code into `experiments/`, centralizing implementation history in a bitácora, documenting the public API contract, adding developer-facing inline documentation to V9, and renaming `useBandEnergy` to the clearer `filterVoiceFrequencies`.

### experiments/ Organization

| Item | Source | Destination | Notes |
|------|--------|-------------|-------|
| V1-V5 .kt files | root package | `experiments/` | Obsolete iterations |
| html_analysis.md | `docs/` | `experiments/` | Historical analysis |
| html_vs_v4.md | `docs/` | `experiments/` | Historical comparison |
| html_para_desarrollador_final.html | `docs/` | `experiments/` | Reference material |
| Prototype demo .mov/.wav | `docs/` | `experiments/` | Large binary assets |
| V6Gml51Cloudy.kt | root | root (stays) | V8's direct base |
| V8.kt | root | root (stays) | V9's direct base |

### Bitácora

| File | Content |
|------|---------|
| `docs/bitacora_estrategias.md` | V1..V9: hypothesis, approach, drawPath count, problems, conclusion per version |

### Public API Documentation

| File | Content |
|------|---------|
| `VoiceVisualizerRing.kt` | Purpose, audio-reactive behavior, parameter descriptions, ranges, defaults |

### Developer Documentation

| File | Content |
|------|---------|
| `VoiceVisualizerRingV9.kt` | Purpose, visual behavior, parameter docs, internals (constants, animation loop, smoothing, canvas structure, blob construction) |

### Rename

| Old name | New name | Scope |
|----------|----------|-------|
| `useBandEnergy` | `filterVoiceFrequencies` | PlayerViewModel, App.kt, all call sites, UI labels |

### Visibility / Behavior

- No functional changes — pure documentation and rename refactor.
- All targets continue to compile without regression.
- `experiments/` code is not referenced by active dispatcher or App.
- V9 inline documentation targets a Kotlin/Compose developer with no prior repo context.

## Decisions

- **V1-V5 to experiments/, V6+V8 stay in root** — V6Gml51Cloudy is V8's base, V8 is V9's base; only truly obsolete versions are moved.
- **Bitácora as separate doc** — keeps source files clean while preserving full implementation history for reference.
- **Block comments over KDoc** — matches existing file conventions; the component is a standalone composable, not a library API.
- **filterVoiceFrequencies rename** — `useBandEnergy` is implementation jargon; `filterVoiceFrequencies` describes what it does from the user perspective.

## Context

- See `AGENTS.md`: existing comment style and conventions.
- See `SPEC.md`: parameter descriptions and behavioral specs to document in code.
- See `TASKS.md`: T25 tracks this documentation refactor.
- `docs/bitacora_estrategias.md` consolidates content from V1..V8 file headers.

## Out of scope

- Code logic changes (purely documentation and rename).
- Removing or archiving any active code (V6, V8, V9 stay in root).
- Changing the dispatcher pattern or composable signatures (beyond the rename).
- iOS-specific documentation or platform-specific deployment guides.
