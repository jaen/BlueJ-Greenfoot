# PR-7: BaseVisitor + Test Infrastructure

## Goal

Establish the core visitor infrastructure and the test framework that all subsequent visitor PRs build on. After this PR:
- Shared data extraction helpers and token creation machinery are in place
- Fragment-based test helpers allow testing individual visitors in isolation
- The Kotlin test corpus (76 `.kt` files from the PoC) is ported and available

## Prerequisites

- **PR-6c** (emit-range filtering) — the visitors need the callback adapter and token synchronization infrastructure.

## BaseVisitor (abstract helper base)

Extends `KtVisitorVoid` (Kotlin PSI visitor), implements `PsiVisitor` interface.

BaseVisitor provides all shared data extraction and token creation logic. It does **not** invoke callbacks itself (except `error()` via `visitErrorElement`). Subclasses call these helpers and then invoke the appropriate callbacks.

**Important**: Implement BaseVisitor critically — don't blindly copy the PoC. The PoC's BaseVisitor (~1354 lines) grew organically and has accumulated cruft, dead code, and suboptimal patterns. Build what the clean visitor implementations actually need, using the PoC as reference for *what* to extract, not *how* to structure it.

### Data extraction helpers

These return immutable record types but **never** invoke callbacks:

| Helper | Returns | Purpose |
|--------|---------|---------|
| `extractModifiers()` | `ModifierSet` | Handles ~20 Kotlin modifiers (visibility, inheritance, data, sealed, etc.) |
| `extractFunctionParameters()` | `FunctionParametersResult` | Parameter list with types and defaults |
| `extractFunctionBody()` | `FunctionBodyResult` | Block body vs expression body distinction |
| `extractFunctionSignature()` | `FunctionSignatureResult` | Combines modifiers + return type + name + parameters + body |
| `extractConstructorParameters()` | — | Primary/secondary constructor parameter extraction |
| `extractTypeTokens()` | — | Converts PSI type references to BlueJ tokens |
| `processPropertyType()` | — | Handles explicit vs inferred types |

### Token creation

All token creation uses `TokenTypeMapper` exclusively (the unified mechanism from PR-5). No `guessTokenType`.

| Method | Purpose |
|--------|---------|
| `createToken(PsiElement, int type)` | Create token with explicit BlueJ type |
| `createToken(PsiElement)` | Create token using `TokenTypeMapper` for type lookup |
| `createTokenWithText(PsiElement, String, int type)` | Create token with overridden text |
| `createEofToken()` | End-of-file sentinel token |

### Position calculation

Converts PSI element offsets (character-based) to line/column positions that BlueJ's callback protocol expects.

### Modifier mapping

A `Modifier` enum maps Kotlin modifiers to Java token types:

- Visibility: `public`, `private`, `protected`, `internal`
- Inheritance: `open`, `abstract`, `sealed`, `final`
- Class types: `data`, `enum`, `annotation`, `inner`
- Member: `override`, `lateinit`, `const`, `companion`
- Coroutines: `suspend`
- Etc.

### Error handling

`visitErrorElement` — shared error callback, invoked when PSI contains error nodes (syntax errors in the source).

### Token base and emit range management

From `PsiVisitor` interface — manages the base token position and emit range for incremental parsing coordination.

## Test infrastructure

This PR also establishes the test framework that PR-8, PR-9, and PR-10 build on.

### Test corpus

The test corpus (76 `.kt` files + `TestCorpus.java` loader) is already ported by the [Track A test corpus task](../../a-foundation/tasks/03-test-corpus/README.md). This PR builds on that foundation.

### Fragment-based test helpers

The Kotlin PSI parser always produces a `KtFile` — you cannot parse "just a class body." Fragment-based testing works by wrapping fragments in synthetic files and navigating the PSI tree:

```java
// Test helper: parse a class body fragment
protected ClassVisitorResult parseClassBody(String classBodyCode) {
    String wrapped = "class _Wrapper_ { " + classBodyCode + " }";
    KtFile ktFile = psiEnvironment.parseFile("_test_.kt", wrapped);
    KtClass ktClass = PsiTreeUtil.findChildOfType(ktFile, KtClass.class);
    CallbackRecorder recorder = new CallbackRecorder(sourceInput);
    ClassVisitor visitor = new ClassVisitor(recorder);
    ktClass.accept(visitor);
    return new ClassVisitorResult(recorder);
}

// Test helper: parse a method body fragment
protected CodeVisitorResult parseMethodBody(String methodBodyCode) {
    String wrapped = "class _Wrapper_ { fun _test_() { " + methodBodyCode + " } }";
    // ... navigate to KtBlockExpression, run CodeVisitor
}

// Test helper: parse a top-level function
protected CodeVisitorResult parseFunction(String functionCode) {
    String wrapped = functionCode; // top-level function is a valid file
    // ... navigate to KtNamedFunction, run CodeVisitor with FILE_LEVEL scope
}
```

These helpers enable PR-8 and PR-9 to test their visitors in isolation without FileVisitor.

## DI integration

### `@ProjectScoped KotlinVisitorFactory`

- Receives `PsiEnvironment`, callback adapter, and other dependencies via `@Inject`
- Creates visitor instances on demand per parse invocation
- Visitors are **not** DI-managed directly — they carry mutable per-parse state

```java
@ProjectScoped
public class KotlinVisitorFactory {
    @Inject private PsiEnvironment psiEnvironment;
    @Inject private Provider<CallbackAdapter> callbackAdapterProvider;

    public ClassVisitor createClassVisitor(/* per-invocation params */) { ... }
    public CodeVisitor createCodeVisitor(Scope scope, /* per-invocation params */) { ... }
    public FileVisitor createFileVisitor(/* per-invocation params */) { ... }
}
```

## PoC reference

| File | Location | Size | Notes |
|------|----------|------|-------|
| `BaseVisitor.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/.../psi/visitor/BaseVisitor.java` | ~1354 lines | Reference for what to extract, not how to structure |
| `PsiVisitor.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/.../psi/visitor/PsiVisitor.java` | ~46 lines | Interface |
| `TokenTypeMapper.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/.../psi/TokenTypeMapper.java` | — | Unified in PR-5 |
| `TestCorpus.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/.../psi/TestCorpus.java` | ~307 lines | Test file discovery |
| `BasePsiTest.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/.../psi/BasePsiTest.java` | ~59 lines | Current test base (full-file only) |
| Test corpus files | `repos/BlueJ-Greenfoot/kotlin-cleanup/.../test/resources/.../psi/test-corpus/` | 76 files | Curated Kotlin sources |

## Acceptance criteria

- [ ] `BaseVisitor` provides all data extraction helpers with clean record types
- [ ] Token creation uses `TokenTypeMapper` exclusively (no `guessTokenType`)
- [ ] `KotlinVisitorFactory` is `@ProjectScoped` and creates visitors with injected dependencies
- [ ] Fragment-based test helpers work: `parseClassBody(...)`, `parseMethodBody(...)`, `parseFunction(...)`
- [ ] Fragment helpers use `TestCorpus` (ported in Track A) for loading test data
- [ ] BaseVisitor data extraction helpers are unit-tested (modifier extraction, parameter extraction, type token extraction)
- [ ] All existing tests pass (zero regressions)

## Estimated effort

**Medium-large.** BaseVisitor is substantial even when implemented critically. The test infrastructure adds meaningful work (fragment helpers, corpus porting), but it pays dividends in PR-8/PR-9/PR-10 testing quality.

## Open questions

1. **`TokenFactory` vs inline position calculation.** Should `BaseVisitor` use `TokenFactory` (IntelliJ's `Document` API for line/column lookup) or inline position calculation? The PoC has both; pick one.

2. **Visitor lifecycle.** Should visitors be fully immutable after construction, or is mutable state (e.g., current position tracking) acceptable? The PoC uses mutable state extensively. A cleaner approach might thread position through method parameters, but that would diverge significantly.

3. **BaseVisitor scope.** How much do we actually need? The PoC's BaseVisitor has 1354 lines. Start by implementing what ClassVisitor and CodeVisitor need (since they're the next PRs), and add helpers as needed rather than porting everything up front.
