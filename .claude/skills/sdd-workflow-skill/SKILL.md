---
name: sdd-workflow
description: >
  Spec-Driven Development workflow skill. Guides the agent through Pablo's SDD process:
  read constitution specs, identify next roadmap task, create dated task folder with
  plan.md/requirements.md/validation.md, iterate with user approval, implement, validate,
  and close. Trigger when the user says "siguiente tarea del roadmap", "next task",
  "vamos con la siguiente", "arranca la tarea", or any phrase indicating they want to
  advance to the next task in the roadmap. Also trigger when the user asks to start SDD,
  set up specs, or initialize the spec-driven workflow for a project.
---

# SDD Workflow Skill

Guides Spec-Driven Development task execution. Based on the process defined in the project's `pablo-ssd-workflow.md` (or equivalent).

## Trigger

Activate when the user signals they want to advance to the next task in the roadmap, or when they ask to set up or start the SDD process.

## Pre-flight

Before starting any task, read these files in order:

1. `AGENTS.md` (or `CLAUDE.md` → `AGENTS.md` redirect) — project-specific rules and constraints.
2. `docs/specs/mission.md` — project purpose and audience.
3. `docs/specs/techs-and-archi.md` — stack and architectural decisions.
4. `docs/specs/roadmap.md` — current task list and state.

If any of these files don't exist, the constitution is incomplete. Ask the user to create them before proceeding.

## Workflow

### Step 1: Identify next task

Read `docs/specs/roadmap.md`. Find the first task (or subtask) with `[ ]`. Announce it to the user and confirm before proceeding.

If all tasks are `[x]`, tell the user the roadmap is complete.

### Step 2: Create task folder

```
docs/specs/YYYY-MM-DD-{task_i}-{task_name}/
```

- **Date**: today's date (task start date).
- **`task_i`**: global number from the roadmap. Decimals allowed for insertions (e.g., 2.5).
- **`task_name`**: kebab-case from the roadmap entry.

### Step 3: Draft the three spec files

Create all three files as drafts. They are NOT final until the user approves.

#### `requirements.md` — WHAT to build

Sections:
- **Scope** — 1-2 sentence description of what this task covers.
- **Detailed requirements** — acceptance criteria, field definitions, behavior rules.
- **Decisions** — design choices for this task and why.
- **Context** — references to constitution files and existing patterns to follow.
- **Out of scope** — what is explicitly excluded.

#### `plan.md` — HOW to build it

Sections:
- Ordered steps, grouped logically.
- Each step specifies: file to create/modify, what to add/change.
- Steps reference `requirements.md` acceptance criteria.

#### `validation.md` — HOW TO VERIFY

Sections:
- **Automated checks** — test commands and specific test cases.
- **Manual checks** — things a human must verify.
- **Definition of Done** — explicit checklist.

### Step 4: Present to user for approval

Show the user a summary of the three specs. Iterate until the user explicitly approves.

### Step 5: Implement

Write code following `plan.md`. Reference `requirements.md` for details.

Rules during implementation:
- One concern per commit.
- Minimal, focused changes.
- Don't refactor unrelated code.
- Follow existing patterns in the codebase.

### Step 6: Validate

Run through `validation.md`:
- Execute automated checks.
- Flag manual checks for the user to verify.

### Step 7: Close (user-only)

- The agent does NOT self-close tasks.
- When the user confirms the task is done, mark `[x]` in `docs/specs/roadmap.md`.
- Commit with message referencing the task number.

## Templates

Blank templates live in `docs/specs/_task_templates/`:
- `plan.md` — HOW to build it (grouped steps, files to touch).
- `requirements.md` — WHAT to build (scope, decisions, context, out-of-scope).
- `validation.md` — HOW TO VERIFY it's done (automated + manual checks + DoD).

Use these as the starting point for every new task folder. Do not deviate from the section structure unless the task clearly requires it.

## Branching

- Work on `main` with progressive, descriptive commits.
- Do not fake git history.
- If specs are reconstructed post-hoc, note it transparently.

## Agent Behavior

- **Read before acting.** Always read constitution + task specs before code.
- **One task at a time.** Don't implement parts of future tasks.
- **Don't re-ask decided questions.** Respect recorded decisions.
- **User in the loop.** Agent proposes, user approves. No self-closing.
- **Language.** Respond in the language the user speaks.
