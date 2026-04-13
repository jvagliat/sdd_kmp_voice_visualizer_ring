# Workflow

## Language
- The user speaks Spanish. Respond in Spanish unless the context requires English (code, logs, errors).

## Before Modifying Code
- Read relevant files before editing. Do not assume project state.
- Follow existing patterns in the same directory when creating new files.
- Use `git status` and recent commits to understand current state when unsure.

## Task Management
- `TASKS.md` es la fuente de verdad del estado activo (In Progress, Pending, Blockers, Estado vivo).
  Al retomar trabajo: leer `TASKS.md` y el último commit. No pedir re-explicación al usuario.
- `DONE.md` contiene el historial de tareas completadas. No leerlo en el flujo normal — solo para arqueología.
- Al completar una task: moverla a `DONE.md` con su commit hash. Borrarla de `TASKS.md`. No acumular tareas completadas.
- Usar TodoWrite para trackear progreso dentro de la sesión activa.
- Marcar cada tarea completa inmediatamente al terminar, no en batch.

## Code Changes
- One concern at a time. Do not refactor unrelated code.
- Do not touch code that does not interfere with the current task.
- Prefer minimal, focused changes over sweeping improvements.
