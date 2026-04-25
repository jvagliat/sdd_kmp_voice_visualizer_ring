# Requirements: HTML Prototype Analysis

## Scope

Perform a detailed cold analysis of the client's HTML/JS prototype and produce two reference documents: an inventory of all visual effects (13 identified) and a gap analysis contrasting the prototype against the V4 Compose implementation. Also archive the original prototype source and client demo materials.

### Analysis document (`html_analysis.md`)

| Section | Content |
|---------|---------|
| Effect inventory | 13 effects listed with implementation details |
| Summary table | Effect number, name, brief description |
| Physical blocks | 5 blocks: silhouette (A), halo (B), multiplicity (C), audio reactivity (D), smoothing (E) |
| Synthesis | One-line summary of the complete visual system |

### The 13 effects to catalog

1. Dark radial-gradient background
2. Hard cyan border ring (5 px)
3. Outer glow × 3 levels (15/40/70 px box-shadow)
4. Inner glow × 3 levels (inset 15/40/70 px)
5. 3 stacked layers with stepped glow
6. border-radius morphing (8 radii, 3 keyframes)
7. Rotation 0°→360° in 8 s
8. Phase offset per layer (animation-delay −2.5 s / −5 s)
9. Sub-pixel blur (filter: blur(0.3 px))
10. Backdrop-filter blur (8 px)
11. Reactive scale (1.0 → ~1.5, lerp 0.15)
12. Reactive brightness per layer (opacity with ×1.0/×0.8/×0.6)
13. FFT mid-band (bins 2–14, smoothingTimeConstant 0.85)

### Contrast document (`html_vs_v4.md`)

Each effect rated against V4: ✅ equivalent, ≈ approximate, ❌ absent. Must include structural differences (DOM vs Canvas, Modifier.blur vs box-shadow, fill vs stroke, volume source).

### Reference materials

- Original HTML source preserved verbatim
- Client demo video and source WAV archived

## Decisions

- **Cold analysis first** — the prototype is analyzed without reference to the KMP implementation, then contrasted separately. This separation ensures objectivity.
- **13 effects** — enumerated from direct code reading of the HTML source, not from assumptions about what "should" be there.
- **Verbatim prototype** — the original HTML is preserved as-is in the repo so future analysis can re-examine it without relying on external links.

## Context

- See `SPEC.md` section 4: analysis of the HTML prototype.
- See `specs/techs-and-archi.md`: key decision about glow approximation.
- This analysis was the basis for identifying that V4 was missing FFT band-energy and a 3-level halo (leading to Task 8 / T14).

## Out of scope

- No code changes to the visualizer component.
- No implementation of missing effects (that is a future task).
- No mobile/web validation (that is a separate task).
