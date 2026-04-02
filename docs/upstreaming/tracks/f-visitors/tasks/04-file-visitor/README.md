# PR-10: FileVisitor

## Goal

Implement file-level dispatch and facade class synthesis. This is the integration layer that ties together ClassVisitor (PR-8) and CodeVisitor (PR-9), enabling full `.kt` file parsing.

After this PR, BlueJ can parse a complete `.kt` file — recognizing package declarations, imports, top-level classes, top-level functions, and top-level properties — and feed the results into its incremental parsing infrastructure.

## Prerequisites

- **PR-8** (ClassVisitor) — FileVisitor dispatches classifier declarations to ClassVisitor
- **PR-9** (CodeVisitor) — FileVisitor dispatches top-level functions/properties to CodeVisitor

## FileVisitor

### `visitKtFile()` entry point

1. Calls `callbacks.reachedCUstate(1)` — compilation unit started
2. Handles package declaration and imports
3. Iterates top-level declarations:
   - `KtClass` / `KtObjectDeclaration` → dispatches to `ClassVisitor`
   - `KtFunction` / `KtProperty` → dispatches to `CodeVisitor` with `FILE_LEVEL` scope (inside synthesized facade class)
4. Calls `callbacks.finishedCU(2)` — compilation unit complete

### Facade class synthesis

When a `.kt` file has top-level functions and/or properties, the Kotlin compiler produces a `FooKt` facade class containing them as static methods/fields. FileVisitor should synthesize callbacks that model this:

1. Detect that the file has top-level functions/properties
2. Emit `gotTypeDef` + `gotTypeDefName` for a synthetic `<FileName>Kt` class
3. Emit `beginTypeBody`
4. Dispatch top-level functions as methods and top-level properties as fields within this synthetic class
5. Emit `endTypeBody` + `gotTypeDefEnd`

This was attempted in the PoC (`BaseVisitor` lines 349-356) but abandoned due to corner cases. PR-1's multi-class `SourceInfo` now properly models multiple classes per file, making this feasible.

**Corner cases to handle:**
- File with only top-level functions (no explicit classes) → single facade class
- File with both classes and top-level functions → explicit classes + facade class
- File with only explicit classes (no top-level functions) → no facade class needed
- File with top-level properties but no functions → facade class with fields only
- Empty files, files with only imports/package declaration

## Testing

This PR uses **full-file integration tests** with the test corpus from PR-7:

```java
// Full-file test: class-only file
@Test
void classOnlyFile() {
    CallbackRecorder result = parseFile(TestCorpus.load("simple/BasicClass.kt"));
    assertThat(result).hasTypeDef("BasicClass");
    assertThat(result).hasNoFacadeClass();
}

// Full-file test: top-level functions
@Test
void topLevelFunctions() {
    CallbackRecorder result = parseFile(TestCorpus.load("simple/TopLevelFunctions.kt"));
    assertThat(result).hasFacadeClass("TopLevelFunctionsKt");
    assertThat(result).hasMethods("foo", "bar");
}

// Full-file test: mixed file
@Test
void mixedFile() {
    CallbackRecorder result = parseFile(TestCorpus.load("moderate/ProjectExample.kt"));
    assertThat(result).hasTypeDef("Project");
    assertThat(result).hasFacadeClass("ProjectExampleKt");
}
```

The corpus categories map to test progression:
- `simple/` → basic dispatch correctness
- `moderate/` → multi-feature integration
- `complex/` → advanced constructs
- `edge-cases/` → boundary conditions

## PoC reference

| File | Location | Size | Notes |
|------|----------|------|-------|
| `FileVisitor.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/.../psi/visitor/FileVisitor.java` | ~140 lines | File-level dispatch (no facade class synthesis) |
| `BaseVisitor.java` lines 349-356 | Same directory | — | Commented-out facade class synthesis attempt |

## Acceptance criteria

- [ ] FileVisitor correctly dispatches to ClassVisitor and CodeVisitor
- [ ] Package declaration and imports are handled
- [ ] Facade class synthesis works for files with top-level functions/properties
- [ ] No facade class synthesized for files with only explicit classes
- [ ] Mixed files (classes + top-level functions) produce both explicit classes and facade class
- [ ] Full-file corpus tests pass across all 4 categories (simple/moderate/complex/edge-cases)
- [ ] All existing tests pass (zero regressions)

## Estimated effort

**Medium.** FileVisitor dispatch is straightforward (~140 lines in PoC). Facade class synthesis is the main design challenge, but the mechanism is well-understood from the PoC's abandoned attempt and the multi-class `SourceInfo` from PR-1.

## Open questions

1. **Facade class synthesis mechanism.** Does FileVisitor emit `gotTypeDef` + `beginTypeBody` callbacks to create a synthetic class? Or is there a simpler way? Need to verify what callbacks BlueJ's downstream infrastructure actually needs.

2. **`@JvmName` annotation handling.** Kotlin allows customizing the facade class name with `@file:JvmName("CustomName")`. Should FileVisitor respect this, or is `<FileName>Kt` sufficient for BlueJ's needs?

3. **Package-level functions.** How do package-level functions interact with the facade class? The facade class should be in the same package as declared in the file's `package` statement.
