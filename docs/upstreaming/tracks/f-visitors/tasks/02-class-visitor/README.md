# PR-8: ClassVisitor

## Goal

Handle classifier body declarations — everything that lives inside a `class`, `interface`, `object`, or `enum` body. After this PR, BlueJ can fully parse Kotlin class structures.

## Prerequisites

- **PR-7** (BaseVisitor + FileVisitor) — ClassVisitor extends BaseVisitor and is dispatched by FileVisitor.

## Can run in parallel with PR-9 (CodeVisitor)

ClassVisitor and CodeVisitor depend only on PR-7. They interact at runtime (ClassVisitor dispatches method bodies to CodeVisitor), but the implementation work is independent — ClassVisitor can stub the CodeVisitor dispatch points during development and wire them once both PRs land.

## What ClassVisitor handles

From the Kotlin spec — `classBody` contents:

### Class header

- Modifiers (uses `extractModifiers()` from BaseVisitor)
- Class name
- Type parameters (generics)
- Primary constructor (if present)
- Supertypes: extends (superclass) and implements (interfaces)

### Primary constructor

- Parameter processing using shared `extractConstructorParameters()` from BaseVisitor
- Parameters that are also property declarations (`val`/`var` in constructor)
- Visibility modifiers on the constructor itself

### Secondary constructors

- Similar parameter processing + delegation call (`this(...)` or `super(...)`)
- Uses the same shared parameter-processing helper as primary constructor

### Init blocks

- `beginInitBlock` / `endInitBlock` callbacks
- Body dispatched to CodeVisitor with BODY_LEVEL scope

### Properties as fields

- `beginFieldDeclarations` / `gotField` / `endFieldDeclarations` / `endField` callbacks
- Explicit vs inferred types (uses `processPropertyType()` from BaseVisitor)
- `val` vs `var` distinction
- Initializer expressions
- Custom getters/setters (dispatched to CodeVisitor)

### Methods

- Dispatched to CodeVisitor with CLASS_LEVEL scope

### Companion objects

- Treated as nested object declarations
- Recursively handled by ClassVisitor

### Nested classifiers

- Nested classes, interfaces, objects
- Recursively handled by a new ClassVisitor instance

### Enum entries

- Currently **stubbed** in the PoC — needs proper implementation
- Each enum entry can have its own body (anonymous class)
- Enum entries with constructor arguments

### Object declarations

- Singleton objects (similar to class but with object-specific callbacks)
- Object expressions (anonymous classes) — handled by CodeVisitor, not ClassVisitor

## Key improvement over PoC: parameter deduplication

The PoC has the same `beginFormalParameter` → `gotTypeSpec` → `gotMethodParameter` → `gotAllMethodParameters` callback sequence written **3 times**:

1. `FunctionVisitor` — function parameters
2. `ClassVisitor` — primary constructor parameters
3. `ClassVisitor` — secondary constructor parameters

Extract this into a shared helper on BaseVisitor that all three call sites use. The helper takes the parameter list PSI node and emits the callback sequence.

## Callbacks fired (unique to ClassVisitor)

These callbacks are primarily fired by ClassVisitor (not FileVisitor or CodeVisitor):

| Callback | When |
|----------|------|
| `gotTypeDef` | Start of class/interface/object/enum declaration |
| `gotTypeDefName` | Class name token |
| `gotTypeDefEnd` | End of declaration |
| `beginTypeBody` / `endTypeBody` | Class body delimiters |
| `beginTypeDefExtends` / `endTypeDefExtends` | Superclass clause |
| `beginTypeDefImplements` / `endTypeDefImplements` | Interface list |
| `beginFieldDeclarations` / `gotField` / `endFieldDeclarations` / `endField` | Property-as-field declarations |
| `gotConstructorDecl` | Constructor declaration |
| `beginInitBlock` / `endInitBlock` | Init block delimiters |
| `reachedCUstate(2)` | Processing progress signal |

## PoC reference

| File | Location | Size | Notes |
|------|----------|------|-------|
| `ClassVisitor.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/psi/visitor/ClassVisitor.java` | ~930 lines | Full classifier body handling |

## Acceptance criteria

- [ ] Classes, interfaces, objects, and enums are correctly parsed
- [ ] Primary and secondary constructors work, including constructor `val`/`var` parameters
- [ ] Properties are recognized as fields with correct type handling
- [ ] Nested classes and companion objects are handled recursively
- [ ] Parameter-processing code is deduplicated into a shared BaseVisitor helper
- [ ] Enum entries are properly handled (not stubbed)
- [ ] Init blocks fire correct `beginInitBlock` / `endInitBlock` callbacks
- [ ] Supertype handling works (extends + implements)
- [ ] All existing tests pass (zero regressions)
- [ ] New tests: various class structures:
  - Simple class with properties and methods
  - Data class
  - Sealed class / sealed interface
  - Enum class with entries (including entries with bodies)
  - Object declaration (singleton)
  - Companion object
  - Nested and inner classes
  - Class with multiple constructors (primary + secondary)
  - Interface with default method implementations
  - Abstract class

## Estimated effort

**Large.** ~930 lines in the PoC, and the PoC's enum handling is incomplete. The parameter deduplication refactor touches both this PR and the shared BaseVisitor code from PR-7. The variety of Kotlin class types (class, data class, sealed class, enum, object, interface) each have quirks that need careful handling.

## Open questions

1. **Should enum entry handling be deferred to a follow-up PR if it proves complex?** Enum entries with bodies are essentially anonymous classes and may require significant work. If the basic enum entry case (name + optional constructor args) is straightforward but entries-with-bodies are complex, it may make sense to handle the simple case here and defer the complex case.

2. **How should `inner class` be distinguished from `class` (nested)?** The PoC's `ClassVisitor` doesn't clearly handle this distinction. In Kotlin, `inner class` has access to the outer class instance while plain nested `class` does not. BlueJ's Java model treats inner classes differently from static nested classes — we need to emit the right modifiers.

3. **Constructor `val`/`var` parameters.** These are simultaneously constructor parameters and property declarations. The PoC handles this, but the callback sequence may need care — BlueJ expects field declarations and constructor parameters to be separate, but in Kotlin they're syntactically unified.
