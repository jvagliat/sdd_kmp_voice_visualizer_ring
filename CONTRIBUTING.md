# Contributing

Thanks for your interest in this project! Contributions are welcome — especially
around applying **Spec-Driven Development (SDD)** to stacks that LLMs don't
dominate, like Kotlin Multiplatform / Compose Multiplatform.

## Ways to Contribute

- **Report bugs or visual glitches** — open an issue with your target (Android,
  Desktop, Web, iOS), platform version, and a screenshot or recording if possible.
- **Improve the visualizer** — glow accuracy, performance, new parameters.
- **Validate untested targets** — iOS runtime validation is still pending (see the
  [Stack](README.md#stack) table). Reports from real devices are valuable.
- **Share SDD experiences** — if you replicate the spec-driven approach on another
  project, open an issue and tell us how it went.

## Development Setup

1. Clone the repo and open it in your IDE (IntelliJ IDEA or Android Studio).
2. Build and run a target:

   ```shell
   .\gradlew.bat :composeApp:run                                # Desktop (JVM)
   .\gradlew.bat :composeApp:assembleDebug                      # Android
   .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun        # Web (wasmJs)
   ```

   For iOS, open `iosApp/` in Xcode.

## Spec-Driven Workflow

This project is built spec-first. Before implementing a non-trivial change:

1. Check the constitution in [`docs/specs/`](docs/specs/) —
   [`mission.md`](docs/specs/mission.md), [`techs-and-archi.md`](docs/specs/techs-and-archi.md),
   and [`roadmap.md`](docs/specs/roadmap.md).
2. For a new task, create a dated folder using the templates in
   [`docs/specs/_task_templates/`](docs/specs/_task_templates/) with `plan.md`,
   `requirements.md`, and `validation.md`.
3. Implement against the spec, then fill in `validation.md` with the results.

## Pull Requests

- Keep each PR focused on a single conceptual change.
- Use clear, imperative commit messages (e.g. `View - RingView: fix glow falloff`).
- Make sure the project builds on the targets your change touches before opening
  the PR.
- Describe **what** changed and **why**, and note which targets you tested.

## Code of Conduct

Be respectful and constructive. We want this to be a welcoming space for anyone
exploring SDD and Kotlin Multiplatform.
