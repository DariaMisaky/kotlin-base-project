---
name: claude-memory
description: Show all Claude Code configuration for this project (.claude directory contents, CLAUDE.md)
allowed-tools: Read, Glob, Bash
---

# Claude Memory & Configuration Overview

Show the user a complete overview of all Claude Code configuration for this project.

## Steps

1. **List `.claude/` directory structure** — run `find .claude/ -type f -not -name '.DS_Store' | sort` from the project root to show all rules, skills, and settings files.

2. **List memory files** — memory files are stored in a session-specific directory under `~/.claude/projects/`. The directory name is the project path with slashes replaced by hyphens. For this project the expected path would be:
   `/Users/wolfpackdigital/.claude/projects/-Users-wolfpackdigital-Documents-MyProjects-BaseXmlProject/memory/`
   Run `ls -la` on that path. If it does not exist yet, note that Claude Code creates it on first memory save.

3. **Show CLAUDE.md location** — confirm that `CLAUDE.md` exists in the project root: `ls -la CLAUDE.md`

4. **Present a structured summary** to the user with:
   - **Rules** (`.claude/rules/`) — list each file with a one-line description
   - **Skills** (`.claude/skills/`) — list each skill with its description from the frontmatter
   - **Settings** (`.claude/settings.json` or `.claude/settings.local.json` if present)
   - **Memory files** — list each file from the memory directory (if it exists)
   - **CLAUDE.md** — confirm its presence and location

Keep the output concise and well-formatted. Do NOT read the full contents of every file — just list them with brief descriptions based on filenames and any frontmatter already known.

## Rules Quick Reference

| File | Purpose |
|------|---------|
| `repositories.md` | Interface+impl split, mapper layer (`data/mappers/`), `toDomain()` pattern |
| `networking.md` | ApiProvider, DTOs with `@SerializedName`, `ProductApi`, mapper layer |
| `viewmodels.md` | `BaseViewModel`, `UiState` data class, `viewModelScope.launch` + `copy()`, `_baseCmd` |
| `use-cases.md` | `BaseUseCase`, `executeNow()`, CancellationException rethrow, flat `domain/usecases/` |
| `navigation.md` | SafeArgs, `main_navigation.xml`, `BaseCommand` types (`PerformNavAction`, `GoBack`, etc.) |
| `dependency-injection.md` | Koin 4.1.1, `AppModules.kt`, interface binding for repos, `parametersOf` for VMs |
| `testing.md` | JUnit + MockK, `StandardTestDispatcher`, `runTest` + `advanceUntilIdle`, AAA pattern |
| `localization.md` | `values/strings.xml` (EN only, no `values-de/`), snake_case keys |
| `databinding.md` | `<layout>` wrapper, BindingAdapters (`isVisible`, `items`, `imageUrl`, etc.), thin Fragment |

## Skills Quick Reference

| Skill | Command | Purpose |
|-------|---------|---------|
| `develop` | `/develop` | Implement a new feature end-to-end |
| `test` | `/test` | Write unit tests for a ViewModel, UseCase, or Repository |
| `code-review` | `/code-review` | Review code for bugs, architecture violations, style |
| `finish` | `/finish` | Run KtLint, build, tests, lint before completing |
| `claude-memory` | `/claude-memory` | Show this overview |
