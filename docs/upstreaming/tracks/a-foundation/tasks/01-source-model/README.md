# PR-1: Unified Source Model + Target Hierarchy Refactor

## Goal

Finish the `common-source-representation` branch. Provide a unified `SourceFile` abstraction, refactor `SourceType` to be extension-aware, resolve naming collisions, and extract `CompilableTarget` from `ClassTarget` to prepare the target hierarchy for Kotlin support.

This PR is a **pure infrastructure refactor** — no Kotlin code is introduced. Kotlin recognition comes in PR-1b.

## Status

**In progress** — 4 commits exist on `feature/common-source-representation` based on `kotlin-support`.

## Prerequisites

None.

## What exists

The following is already implemented on `feature/common-source-representation`:

- **`bluej.source` package**: `SourceFile`, `SourceId`, `SourceType` (Java/Stride/PlainText), `PackageName`
- **Multi-class `SourceInfo`** in `parser.symtab` — supports multiple classes per file, which is needed for Kotlin (a single `.kt` file can declare multiple public classes)
- **Partial wiring** in `ClassTarget`, `FlowEditor`, `InfoParser`
- **~1100 lines of tests** across 8 test files

## What's left

### SourceType refactoring

The current `SourceType` enum has design issues that would break for Kotlin (and are already inconsistent):

- `getExtension()` exists but is rarely used in production. Most callers use `toString().toLowerCase()` to construct file extensions — this works for Java/Stride (where the enum name matches the extension) but would produce `"kotlin"` instead of `"kt"`.
- `tryParse()` has zero production callers (only tests).

**Refactor `SourceType` to:**
- Add a constructor field for the file extension (`Optional<String>`, null for `PlainText`)
- `getExtension()` returns `Optional<String>` from the field
- Add `fromFileName(String)` — factory method that determines source type from a file name by checking extensions. Single source of truth for file-to-type mapping.
- Add `sourceFileName(String baseName)` — builds a source file name (`"Foo" + ".java"` → `"Foo.java"`). Throws for `PlainText` (no canonical extension).
- Drop `tryParse()` — zero production callers
- Migrate all `toString().toLowerCase()` callers in `ClassTarget`, `Package`, `Import`, `ExportManager`, `ProjectJavadocResolver` to use `getExtension()`, `fromFileName()`, or `sourceFileName()`

### SourceInfo naming collision

Two classes share the simple name `SourceInfo`:
- `bluej.pkgmgr.SourceInfo` — legacy single-class data holder + lookup
- `bluej.parser.symtab.SourceInfo` — new multi-class model introduced by this branch

Rename one or consolidate to eliminate developer confusion. The `pkgmgr` version is smaller and more legacy; renaming it is likely the right call.

### Dual-path fallback cleanup

`ClassTarget` currently has patterns like `sf != null ? newPath : legacyPath` to keep things working while migration is partial. There's also an inconsistency between `analyseSource` (falls back to `getJavaSourceFile()` only) and `enforcePackage` (falls back to `getSourceFile()` which includes Stride). Unify and remove fallbacks where the new `SourceFile` path is fully wired.

### CompilableTarget extraction

Extract shared compilation infrastructure from `ClassTarget` into an abstract `CompilableTarget` superclass, informed by Neil Brown's [`target-classes` branch](https://github.com/k-pet-group/BlueJ-Greenfoot/compare/main...neilccbrown:BlueJ-Greenfoot:target-classes) on `main`.

**`CompilableTarget`** holds:
- Compilation state: `queued`, `compilationInvalid`
- Editor properties map
- `doubleClick()` (open editor), `scheduleCompilation()`, `isCompiled()`, `removeStepMark()`, `showDiagnostic()`
- Abstract methods: `analyseSource()` (returns `List<ClassInfo>`), `hasSourceCode()`, `reload()`, `reInitBreakpoints()`, `markCompiling()`

**`ClassTarget`** becomes `extends CompilableTarget` (was `extends DependentTarget`). All existing Java/Stride-specific behavior stays on `ClassTarget`.

**`Package.getTargets(Class<T>)`** — generic replacement for `getClassTargets()`. Returns targets of a given type. Callers that need "anything compilable" use `CompilableTarget.class`.

Note: Neil's version is on `main` and uses `bluej.extensions2.SourceType`. We redo this on `kotlin-support` using `bluej.source.SourceType` and integrating with the `SourceFile` model.

### Deferred to PR-1b

- `SourceType.Kotlin` — deferred because this PR introduces no Kotlin code
- `KotlinTarget` — deferred; needs `CompilableTarget` (from this PR) and `SourceType.Kotlin`
- `.kt` file discovery in `Package.loadTargets()`

### Deferred / out of scope

- **Stride/`FrameEditor` wiring** — Stride is not on the critical path for Kotlin support; deferral is fine.
- **Compiler pipeline wiring** (`SourceFile` through to the compiler) — may be addressed here if straightforward, otherwise deferred to PR-3 where `KotlinCompiler` is introduced.

## Key files

| File | Lines | Role |
|------|-------|------|
| `bluej/src/main/java/bluej/source/SourceFile.java` | ~276 | Primary abstraction — bundles identity, type, charset, package, content |
| `bluej/src/main/java/bluej/source/SourceId.java` | ~107 | Value type for source file identity |
| `bluej/src/main/java/bluej/source/SourceType.java` | ~90 | Language enum (Java, Stride, PlainText) with extension mapping |
| `bluej/src/main/java/bluej/source/PackageName.java` | ~63 | Type-safe package name wrapper |
| `bluej/src/main/java/bluej/parser/symtab/SourceInfo.java` | ~123 | Multi-class model (supports N classes per file) |
| `bluej/src/main/java/bluej/pkgmgr/target/ClassTarget.java` | ~2573 | Java/Stride target — to be refactored |
| `bluej/src/main/java/bluej/pkgmgr/target/CompilableTarget.java` | NEW | Abstract base for compilable targets |
| `bluej/src/main/java/bluej/pkgmgr/Package.java` | large | Target management — `getTargets(Class<T>)` |

## Acceptance criteria

- [ ] `SourceType` has extension as a field, `fromFileName()`, `sourceFileName()`, `Optional<String> getExtension()`
- [ ] `tryParse()` removed (zero production callers)
- [ ] All `toString().toLowerCase()` extension patterns migrated to new `SourceType` API
- [ ] No naming collision between the two `SourceInfo` classes
- [ ] Dual-path fallbacks in `ClassTarget` unified or removed where safe
- [ ] `CompilableTarget` extracted from `ClassTarget` with shared compilation infrastructure
- [ ] `ClassTarget extends CompilableTarget` (was `DependentTarget`)
- [ ] `analyseSource()` returns `List<ClassInfo>` (not single `ClassInfo`)
- [ ] `Package.getTargets(Class<T>)` replaces `getClassTargets()`
- [ ] All existing tests pass (zero regressions)
- [ ] All new tests pass (~1100 lines already exist across 8 test files)
- [ ] `SourceType` tests updated for new API (extension, `fromFileName`, etc.)

## Estimated effort

**Medium.** The `SourceFile` infrastructure is already built. The remaining work is `SourceType` refactoring, `SourceInfo` rename, fallback cleanup, `CompilableTarget` extraction, and `Package.getTargets()`. The `CompilableTarget` extraction is mechanical but touches many callers in `Package` and other files.
