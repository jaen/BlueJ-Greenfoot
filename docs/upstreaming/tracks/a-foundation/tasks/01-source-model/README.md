# PR-1: Unified Source Model

## Goal

Finish the `common-source-representation` branch. Provide a unified `SourceFile` abstraction that bundles identity, language type, charset, package name, and content access. This gives BlueJ a first-class model for "a source file" that is language-aware from the start.

## Status

**In progress** -- 4 commits exist on `feature/common-source-representation` based on `kotlin-support`.

## Prerequisites

None.

## What exists

The following is already implemented on `feature/common-source-representation`:

- **`bluej.source` package**: `SourceFile`, `SourceId`, `SourceType` (Java/Stride/PlainText), `PackageName`
- **Multi-class `SourceInfo`** in `parser.symtab` -- supports multiple classes per file, which is needed for Kotlin (a single `.kt` file can declare multiple public classes)
- **Partial wiring** in `ClassTarget`, `FlowEditor`, `InfoParser`
- **~1100 lines of tests** across 8 test files

## What's left

1. **Add `Kotlin` variant to `SourceType` enum** -- trivial addition, but needed for any downstream Kotlin work.

2. **Resolve naming collision** between `pkgmgr.SourceInfo` and `parser.symtab.SourceInfo` -- two classes with the same simple name in different packages. Rename one or consolidate. The `parser.symtab` version is the new multi-class model; the `pkgmgr` version is a legacy data holder.

3. **Remove dual-path fallbacks where safe** -- `ClassTarget` currently has patterns like `sf != null ? newPath : legacyPath` to keep things working while migration is partial. Where the new `SourceFile` path is fully wired, remove the fallback.

4. **Consider Stride/`FrameEditor` wiring** -- decide whether Stride and `FrameEditor` need `SourceFile` wiring now or if this can be deferred. Stride is not on the critical path for Kotlin support, so deferral is likely fine.

5. **Wire `SourceFile` through the compiler pipeline** -- the compiler currently receives raw file paths. It should receive `SourceFile` instances so the compilation pipeline knows the language type.

## PoC reference

`common-source-representation` worktree at `repos/BlueJ-Greenfoot/common-source-representation/`

## Key files

| File | Lines | Role |
|------|-------|------|
| `bluej/src/main/java/bluej/source/SourceFile.java` | ~276 | Primary abstraction -- bundles identity, type, charset, package, content |
| `bluej/src/main/java/bluej/source/SourceId.java` | ~107 | Value type for source file identity |
| `bluej/src/main/java/bluej/source/SourceType.java` | ~90 | Language enum (Java, Stride, PlainText; Kotlin to be added) |
| `bluej/src/main/java/bluej/source/PackageName.java` | ~63 | Type-safe package name wrapper |
| `bluej/src/main/java/bluej/parser/symtab/SourceInfo.java` | ~123 | Multi-class model (supports N classes per file) |

## Acceptance criteria

- [ ] `SourceType.Kotlin` exists and is usable
- [ ] No naming collision between the two `SourceInfo` classes (either renamed or consolidated)
- [ ] `ClassTarget.toSourceFile()` works for both Java and Kotlin targets
- [ ] All existing tests pass
- [ ] All new tests pass (~1100 lines already exist across 8 test files)

## Estimated effort

**Small-medium.** Most of the infrastructure is already built. The remaining work is cleanup, wiring, and adding the Kotlin variant.
