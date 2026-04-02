# Track A: Foundation

## Goal

Establish language-agnostic foundations that subsequent Kotlin PRs build on. These PRs prepare BlueJ's infrastructure to be language-aware without committing to any Kotlin-specific implementation.

## PRs in this track

| PR | Focus | Status | Branch |
|----|-------|--------|--------|
| [PR-1: Source Model](tasks/01-source-model/README.md) | Unified `SourceFile` abstraction | In progress | `feature/common-source-representation` |
| [PR-2: Token Infrastructure](tasks/02-token-infrastructure/README.md) | Language-agnostic token queries | Pending | -- |
| [Test Corpus: Fix, Curate, Port](tasks/03-test-corpus/README.md) | Prepare Kotlin test corpus for all subsequent PRs | Pending | -- |

## Independence

All tasks in this track are **independent of each other and of all other tracks**. All can start immediately with no prerequisites.

## Shared context

Both PRs are **pure Java refactors**. No Kotlin code is introduced. They restructure existing BlueJ infrastructure so that:

- Source files have a first-class identity that includes language type (PR-1)
- Parsing nodes don't hardcode Java-specific token type checks (PR-2)

These foundations are consumed by later PRs:

- PR-1's `SourceFile` and `SourceType` are used throughout Tracks B-F whenever code needs to know what language a file is written in.
- PR-2's token abstractions are used by PR-5 (Kotlin lexer swap) to make partial parsing nodes work with Kotlin tokens.
- Test corpus files and `TestCorpus.java` loader are used by PR-5 onwards for Kotlin-specific tests.

## Target branch

`kotlin-support` (has DI + threadchecker already merged).
