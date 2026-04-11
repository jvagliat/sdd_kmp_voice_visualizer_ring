# Commit Practices

## When to Commit
- When se completa una tarea autocontenida (feature, bugfix, refactor), crear un commit inmediatamente.
- Una tarea autocontenida es aquella que deja el código en un estado coherente y funcional por sí sola.
- Si una tarea grande se compone de pasos independientes, cada paso que deje el código estable merece su propio commit.
- If unsure whether a change is self-contained, ask first.

### Un commit = una pieza conceptual
Si en un mismo working-tree change hay varias cosas que un lector del log necesitaría ver por separado (ej. una regla + un skill grande, una refactor + un feature, setup de tracking + contenido nuevo), **splitteá en commits separados** aunque sea tentador agruparlos porque "vienen juntos" en la misma sesión.

**Test antes de commitear**: ¿el subject del commit cuenta toda la historia del cambio? Si tenés que decir "y también…" al explicarlo, splittear. En la duda entre 1 o 2 commits, hacé 2.

## Commit Message Format

### Structure
- **Code changes**: `Module - Area: description`
- **Docs/config files**: `Filename: description`

Module names are **Capitalized** (e.g., `LinkedinImport`, `Variants`, `Models`).
Descriptions use imperative mood (e.g., "add", "fix", "update", not "added", "fixed").

### Choosing `Module`
El prefijo debe **discriminar**. Si todos los commits del repo empezarían con el mismo prefijo, está mal elegido — bajá un nivel a una agrupación conceptual (capa arquitectónica, dominio funcional, feature).

- **No uses el nombre de un subproyecto Gradle/paquete como `Module` si el repo tiene un único subproyecto activo** (e.g. no `ComposeApp`, `Backend`, `Frontend` cuando es lo único que hay). No aporta información.
- Preferí agrupaciones conceptuales sobre ubicaciones físicas. Ejemplos: `Audio`, `Presentation`, `View`, `Platform`, `Build`.
- Si dudás entre dos prefijos, elegí el que mejor responda "¿qué rol conceptual cumple este cambio?" — no "¿en qué carpeta vive?".

#### Prefijos canónicos para este repo
Lista viva — agregá nuevos cuando aparezcan capas/dominios estables:

- `Audio` — procesamiento de audio, parsers WAV, playback/recording infra (incluye implementaciones platform-specific de audio)
- `Presentation` — ViewModels, StateFlows, lógica MVVM sin Compose runtime
- `View` — Composables, pantallas, componentes UI (DebugPanel, RingView, etc.)
- `Platform` — helpers expect/actual genéricos no ligados a un dominio (filesystem, permisos, etc.)
- `Build` — Gradle, versiones, configuración de build
- `Docs` — cambios en `AGENTS.md`, `SPEC.md`, `CLAUDE.md` (alternativa al formato `Filename: description`)
- `Claude` — cambios en `.claude/` (meta-config del repo). Areas típicas: `Rules`, `CMP Skill`, `Commands`, `Agents`.

### Examples
```
LinkedinImport - Skills: add SKILLS_MAP with 67 categorized skills
LinkedinImport - Positions: fix _is_pre_2011 to include froodies GmbH
Variants - Config: add PositionMatch and PositionOverride models
Variants - Applicator: apply_variant filters positions by date range
Variants - AiMobile: add AI_MOBILE_CONFIG with rules 1-8
Variants - Mobile: add MOBILE_CONFIG with rules M1-M6
Cli - ImportLinkedin: add --csv-dir and --variant flags
Models - CVData: add websites field to PersonalInfo
Tests - Variants: add 88 tests for import and applicator
pyproject.toml: remove readme field
ARCHITECTURE.md: document pipeline replacement with importers+variants
TASKS.md: mark phase 1.3 as completed
```

### Anti-ejemplo: commits que deben splittearse
Incorrecto — un único commit mezclando setup de tracking, 3 rules files y un skill de 25 archivos:
```
.gitignore: track .claude/ config except settings.local.json
```
El subject no cuenta qué hay adentro; un lector del log no ve ni las rules ni el skill de CMP.

Correcto — dos commits, cada uno con su pieza conceptual:
```
Claude - Rules: start tracking .claude/rules with project conventions
Claude - CMP Skill: add jetpack-compose-expert reference bundle
```

### HEREDOC
Pass the message via HEREDOC to preserve formatting:
```bash
git commit -m "$(cat <<'EOF'
Variants - Applicator: apply_variant filters positions by date range
EOF
)"
```

## Staging
- Prefer adding specific files by name (`git add file1 file2`) over `git add -A` or `git add .`.
- Never commit files that may contain secrets (`.env`, credentials, API keys). Warn the user if they request it.
- Review `git status` and `git diff` before every commit to understand what will be staged.

## Safety Rules
- **Never** update git config.
- **Never** run destructive commands (`push --force`, `reset --hard`, `checkout .`, `clean -f`, `branch -D`) unless the user explicitly requests it.
- **Never** skip hooks (`--no-verify`, `--no-gpg-sign`) unless the user explicitly requests it.
- **Never** force push to `main`/`master` — warn the user if they request it.
- **Never** amend commits unless the user explicitly requests it. After a pre-commit hook failure, fix the issue and create a **new** commit.
- **Never** use interactive flags (`-i`) with git commands.

## Pre-Commit Hook Failures
- If a pre-commit hook fails, the commit did NOT happen.
- Fix the reported issue, re-stage the files, and create a **new** commit (do not `--amend`).

## Pull Requests
- Use `gh pr create` for all GitHub PR operations.
- PR title: under 70 characters, details go in the body.
- PR body format:
  ```
  ## Summary
  - Bullet points

  ## Test plan
  - [ ] Checklist items
  ```

## Workflow Before Committing
1. Run `git status` (never use `-uall` flag).
2. Run `git diff` to review staged and unstaged changes.
3. Run `git log --oneline -5` to match the repo's commit message style.
4. Stage specific files.
5. Create the commit.
6. Run `git status` to verify success.
