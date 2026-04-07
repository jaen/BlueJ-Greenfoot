# PR-1b: Kotlin Language Recognition + KotlinTarget

## Goal

Introduce Kotlin as a recognized language in BlueJ. After this PR, BlueJ can discover `.kt` files, create `KotlinTarget` instances for them, and represent them as `SourceFile` instances with `SourceType.Kotlin`. The actual Kotlin compilation, parsing, and editing are stubs — filled in by later PRs.

## Prerequisites

- **PR-1** (Source Model + Target Hierarchy) — needs `CompilableTarget` to extend, `SourceType` to add the Kotlin variant, `Package.getTargets()` for target management.

## Contents

### `SourceType.Kotlin`

Add the `Kotlin` variant to the `SourceType` enum with extension `"kt"`:

- `SourceType.Kotlin.getExtension()` → `Optional.of("kt")`
- `SourceType.fromFileName("Foo.kt")` → `Optional.of(Kotlin)`
- `SourceType.Kotlin.sourceFileName("Foo")` → `"Foo.kt"`
- `SourceType.Kotlin.isCode()` → `true`

### `KotlinTarget`

A new concrete class extending `CompilableTarget` (from PR-1). Handles `.kt` source files.

**Implemented:**
- Constructor: `KotlinTarget(Package pkg, String identifierName)`
- `getSourceFile()` → `new File(pkg.getPath(), baseName + ".kt")` (or use `SourceType.Kotlin.sourceFileName()`)
- `toSourceFile()` → creates `SourceFile` with `SourceType.Kotlin`
- `hasSourceCode()` → `true` (Kotlin targets only exist when source exists)
- `markCompiling()`, `reInitBreakpoints()`, `reload()` — basic implementations (similar to Neil's version)
- Blackbox recording methods — no-ops (Kotlin doesn't participate in data collection)

**Stubbed (TODO for later PRs):**
- `analyseSource()` → returns empty list (filled in by Track F visitors)
- `getEditor()` → returns null or creates a basic `FlowEditor` (filled in when editor support lands)
- `remove()` → deletes the `.kt` file
- `getContextOperations()` → minimal context menu

### `.kt` file discovery in `Package.loadTargets()`

When `Package.loadTargets()` scans the directory for source files, it currently creates `ClassTarget` instances for `.java` and `.stride` files. Extend this to:

1. Scan for `.kt` files using `SourceType.fromFileName()`
2. Create `KotlinTarget` instances for discovered `.kt` files
3. Handle the case where a `.kt` file and a `.java` file have the same base name (the `.kt` file should take precedence, or both should coexist as separate targets — needs investigation)

### Tests

- `SourceType.Kotlin` tests: `fromFileName`, `sourceFileName`, `getExtension`, `isCode`
- `KotlinTarget` unit tests: source file construction, `toSourceFile()`, `hasSourceCode()`
- `Package` integration test: `.kt` file discovery creates `KotlinTarget` (if testable)

## PoC reference

Neil Brown's [`target-classes` branch](https://github.com/k-pet-group/BlueJ-Greenfoot/compare/main...neilccbrown:BlueJ-Greenfoot:target-classes) has a `KotlinTarget` implementation to reference.

## Acceptance criteria

- [ ] `SourceType.Kotlin` exists with extension `"kt"` and `isCode() == true`
- [ ] `KotlinTarget` extends `CompilableTarget` and is a concrete class
- [ ] `KotlinTarget.getSourceFile()` returns the correct `.kt` file path
- [ ] `KotlinTarget.toSourceFile()` creates a `SourceFile` with `SourceType.Kotlin`
- [ ] `Package.loadTargets()` discovers `.kt` files and creates `KotlinTarget` instances
- [ ] All existing tests pass (zero regressions)
- [ ] New tests for `SourceType.Kotlin` and `KotlinTarget`

## Estimated effort

**Small-medium.** The `SourceType.Kotlin` addition is trivial. `KotlinTarget` is mostly stubs. The main work is wiring `.kt` discovery into `Package.loadTargets()` and ensuring it integrates correctly with the existing target management.

## Open questions

1. **Same-name `.kt` and `.java` files.** If `Foo.java` and `Foo.kt` both exist, should there be two separate targets? In Kotlin, this is legal — they can contain different classes. But BlueJ's target model assumes one target per base name. This may need a design decision.

2. **`KotlinTarget` editor.** Should we provide a basic read-only editor (plain `FlowEditor` with `SourceType.Kotlin`) in this PR, or leave `getEditor()` as null until full editor support lands? A basic editor lets developers view `.kt` files in BlueJ even before full Kotlin support is complete.

3. **Package default source type.** `Package.getDefaultSourceType()` currently returns Stride if any Stride files exist, otherwise Java. Should it consider Kotlin files? For now, probably not — the default is about what language to use for new classes, and Kotlin class creation isn't supported yet.
