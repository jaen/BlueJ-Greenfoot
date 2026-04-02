# PR-6a: PsiEnvironment

## Goal

Introduce `PsiEnvironment` for creating Kotlin PSI trees from source code. DI-managed, `@ProjectScoped`.

## Prerequisites

- **PR-3** (Kotlin Build & Compile) — provides `kotlin-compiler-embeddable` dependency

## What it does

Wraps `KotlinCoreEnvironment` + `KtPsiFactory` to parse Kotlin source into `KtFile` PSI trees. This is the entry point for all PSI-based Kotlin parsing — every component that needs to analyze Kotlin source code goes through `PsiEnvironment.parseFile()`.

The PSI tree is Kotlin's native AST representation, used by the Kotlin compiler itself. By creating `KtFile` instances, we get access to the full Kotlin PSI API for walking declarations, expressions, type references, etc.

## DI migration

| Aspect | PoC | Upstream |
|--------|-----|----------|
| **Lifecycle** | Static `PsiEnvironment.getInstance()` singleton, shared across all projects | `@ProjectScoped` — each project gets its own instance |
| **Initialization** | Sets `idea.home.path` system property, creates disposable manually | DI-managed `@Inject` constructor, proper initialization |
| **Cleanup** | Manual `dispose()`, easy to leak | Lifecycle managed by DI — project close triggers cleanup |
| **Testing** | Difficult (static state, system properties) | Testable via scope injection |

This follows the same pattern as the `ViewFactory` migration already on the `kotlin-support` branch.

## Contents

### `PsiEnvironment.java`

`@ProjectScoped` service wrapping `KotlinCoreEnvironment`:

- `@Inject` constructor initializes the Kotlin compiler environment
- `parseFile(String fileName, String sourceCode)` -> `KtFile` — creates a PSI tree from source text
- Proper lifecycle management (dispose on project close)
- Error handling: PSI parsing failures produce `PsiParseException` rather than propagating raw Kotlin compiler exceptions

### `PsiParseException.java`

Exception type for PSI parsing failures. Wraps underlying Kotlin compiler exceptions with context (file name, source snippet).

### `PsiEnvironmentTest.java`

Tests covering:
- Basic parsing of valid Kotlin source
- Parsing source with syntax errors (should produce a PSI tree with error nodes, not throw)
- Multiple sequential parses (environment is reusable)
- Thread safety (if relevant — PSI operations may need to be on a specific thread)
- Lifecycle: creation and disposal

## Key change from PoC

Beyond the DI migration, the main cleanup is removing the `idea.home.path` system property hack. The PoC sets this system property globally to satisfy `KotlinCoreEnvironment`'s initialization. The upstream version should handle this through proper DI-managed initialization, either by:

- Setting the property in a controlled, scoped manner during environment creation
- Using `KotlinCoreEnvironment`'s configuration API to avoid the system property entirely
- Documenting the constraint if the system property is genuinely required

## PoC reference

| File | Location | Size |
|------|----------|------|
| `PsiEnvironment.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/psi/PsiEnvironment.java` | ~346 lines |

## Acceptance criteria

- [ ] `PsiEnvironment` is `@ProjectScoped` and injectable via `@Inject`
- [ ] `parseFile()` produces valid `KtFile` PSI trees from Kotlin source code
- [ ] PSI failures don't crash BlueJ (proper error handling via `PsiParseException`)
- [ ] Lifecycle is managed by DI (project close triggers cleanup)
- [ ] No global system property side effects (or documented if unavoidable)
- [ ] All existing tests pass (zero regressions)
- [ ] New tests for `PsiEnvironment` covering valid source, error source, reuse, and lifecycle

## Estimated effort

**Small.** The PoC has a working implementation at ~346 lines. The main work is the DI migration (replacing static singleton with `@ProjectScoped`), removing the system property hack, and writing tests. No complex logic changes.
