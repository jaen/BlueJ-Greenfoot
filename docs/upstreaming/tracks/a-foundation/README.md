# Track A: Foundation

## Goal

Establish language-agnostic foundations that subsequent Kotlin PRs build on. These PRs prepare BlueJ's infrastructure to be language-aware without committing to any Kotlin-specific implementation (PR-1, PR-2, test corpus), then introduce Kotlin as a recognized language (PR-1b).

## PRs in this track

| PR | Focus | Status | Branch |
|----|-------|--------|--------|
| [PR-1: Source Model + Target Hierarchy](tasks/01-source-model/README.md) | Unified `SourceFile` abstraction, `SourceType` refactoring, `CompilableTarget` extraction, `Package.getTargets()` | In progress | `feature/common-source-representation` |
| [PR-1b: Kotlin Language Recognition](tasks/01b-kotlin-target/README.md) | `SourceType.Kotlin`, `KotlinTarget` stub, `.kt` file discovery | Pending (needs PR-1) | -- |
| [PR-2: Token Infrastructure](tasks/02-token-infrastructure/README.md) | Language-agnostic token queries | Pending | -- |
| [Test Corpus: Fix, Curate, Port](tasks/03-test-corpus/README.md) | Prepare Kotlin test corpus for all subsequent PRs | Pending | -- |

## Dependencies within this track

- PR-1, PR-2, and test corpus are **independent of each other and of all other tracks**.
- PR-1b depends on PR-1 (needs `CompilableTarget` to extend, and `SourceType` to add the Kotlin variant).

## Shared context

PR-1 and PR-2 are **pure Java refactors**. No Kotlin code is introduced. PR-1b is the first PR that introduces Kotlin-specific code (the `KotlinTarget` class and `SourceType.Kotlin` enum variant).

These foundations are consumed by later PRs:

- PR-1's `SourceFile` and `SourceType` are used throughout Tracks B-F whenever code needs to know what language a file is written in.
- PR-1's `CompilableTarget` is the base class that `KotlinTarget` (PR-1b) extends.
- PR-1b's `KotlinTarget` is the integration point for Kotlin compilation (Track B), parsing (Tracks E-F), and editing.
- PR-2's token abstractions are used by PR-5 (Kotlin lexer swap) to make partial parsing nodes work with Kotlin tokens.
- Test corpus files and `TestCorpus.java` loader are used by PR-5 onwards for Kotlin-specific tests.

## Target branch

`kotlin-support` (has DI + threadchecker already merged).
