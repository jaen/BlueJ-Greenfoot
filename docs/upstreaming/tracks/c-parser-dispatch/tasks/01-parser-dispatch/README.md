# PR-4: Parser Dispatch

## Goal

Implement the parser dispatch mechanism chosen by Spike S1. Expected approach: composition via `@ProjectScoped LanguageRegistry`.

## Prerequisites

- **Spike S1** results (determines the approach)
- Builds on `LanguageRegistry` from **PR-3** (Track B)

## Expected approach (composition)

Based on the recommended direction from the track overview and upstreaming plan. Final approach subject to S1 results.

### `LanguageSupport` gains `createParser()`

The `LanguageSupport` interface (introduced as a skeleton in PR-3) gains a `createParser(...)` method:

```java
public interface LanguageSupport {
    Compiler getCompiler();          // from PR-3
    SourceParser createParser(...);  // added by this PR
}
```

### Java path — wrapped, not modified

- `JavaLanguageSupport.createParser()` wraps the existing `JavaParser` / `EditorParser` creation logic
- `JavaParser` itself is **NOT modified** — zero regression risk
- `EditorParser` stays as-is, created through `JavaLanguageSupport` for the Java path

### Kotlin path — stub for now

- `KotlinLanguageSupport.createParser()` exists but returns a stub (or throws `UnsupportedOperationException`)
- The actual `KotlinPsiParser` implementation comes in later PRs (Track E/F)
- This PR just sets up the dispatch mechanism

### Caller updates

- Callers that currently create `JavaParser` / `EditorParser` directly are updated to go through `LanguageRegistry`
- `LanguageRegistry` dispatches based on `SourceType` (determined from the file being parsed)

## Key challenge: `EditorParser` inheritance

`EditorParser` extends `JavaParser` via inheritance. This means the Java parsing path is tightly coupled.

The composition approach handles this by:
- `EditorParser` stays as-is for Java — it's created by `JavaLanguageSupport.createParser()`
- The Kotlin path uses a completely separate class (`KotlinEditorParser` or similar) that implements the same contract but via PSI visitors instead of recursive descent
- Both are resolved through `LanguageRegistry` — the caller doesn't know which concrete class it gets
- There is no shared `Parser` interface between Java and Kotlin — just a shared dispatch mechanism

## DI integration

### Interface changes

```java
public interface LanguageSupport {
    Compiler getCompiler();          // PR-3
    SourceParser createParser(...);  // this PR
}
```

### `LanguageRegistry` updates

- `LanguageRegistry` (from PR-3) is updated to dispatch parser creation
- Callers inject `LanguageRegistry` and call `registry.getSupport(sourceType).createParser(...)`

### Caller migration

Callsites that currently do:
```java
new JavaParser(...);
// or
new EditorParser(...);
```

Are updated to:
```java
languageRegistry.getSupport(sourceType).createParser(...);
```

The exact number of callsites is determined by Spike S1's investigation.

## Acceptance criteria

- [ ] `JavaParser` / `EditorParser` source files are unchanged (zero diff)
- [ ] Parser creation goes through `LanguageRegistry` for all callsites
- [ ] `LanguageSupport` interface has `createParser(...)` method
- [ ] `JavaLanguageSupport.createParser()` correctly creates `JavaParser` / `EditorParser` as before
- [ ] `KotlinLanguageSupport.createParser()` exists (can return a stub or throw for now)
- [ ] All existing Java tests pass unchanged (zero regressions)
- [ ] The dispatch mechanism is tested:
  - Java source files dispatch to `JavaLanguageSupport`
  - Kotlin source files dispatch to `KotlinLanguageSupport`
  - Unknown source types produce a clear error

## Estimated effort

**Medium.** The dispatch mechanism itself is straightforward. The work is in:
1. Understanding the current parser creation callsites (from S1)
2. Wiring them through `LanguageRegistry` without breaking anything
3. Handling the `EditorParser` inheritance situation

## Open questions

1. **How many callers directly instantiate `JavaParser` / `EditorParser`?** This determines the scope of the wiring changes. S1 should answer this.

2. **Should `InfoParser` and `EditorParser` both go through `LanguageRegistry`, or only `EditorParser`?** `InfoParser` is used for non-interactive analysis (class info extraction). It may not need multi-language dispatch in the first pass — Kotlin class info might be extracted from compiled metadata instead of parsing source.

3. **What parameters does `createParser(...)` need?** The signature depends on what `EditorParser` and `InfoParser` need to be constructed. S1's call graph analysis should clarify this.

4. **Should the return type be a common interface or `Object`?** If Java and Kotlin parsers don't share an interface, callers need to know which type they're getting — or the dispatch needs to happen at a higher level (e.g., `LanguageSupport.parseFile(...)` instead of `createParser(...)`).
