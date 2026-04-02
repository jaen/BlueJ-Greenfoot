# Spike S1: Parser Abstraction Approach

## Goal

Determine how far we can push parser abstraction before the effort outweighs the returns. The question is not whether separate classes for Java and Kotlin parsing make sense (they clearly do), but how much of the existing code we need to change to enable it — given that `EditorParser` inherits from `JavaParser`, incremental parsing relies on Java-specific details, and the 3467-line `JavaParser` is risky to modify.

## Time-box

1–2 days. This is investigation, not implementation.

## Prerequisites

None. Phase 1 — can start immediately.

## The core tension

We want to introduce Kotlin parsing alongside Java parsing. The ideal is clean separation via DI-managed dispatch. The constraint is that the existing Java parsing infrastructure has tight coupling:

- `EditorParser extends JavaParser` (inheritance, not composition)
- `InfoParser` and `CompletionParser` have their own parser usage patterns
- Incremental parsing nodes (`IncrementalParsingNode`, `MethodBodyNode`, etc.) assume Java-specific details — PR-2 (token infrastructure) and PR-5 (lexer) will mitigate some of this, but probably not all of it

The spike should determine: **what's the minimum-viable abstraction that lets us dispatch to a Kotlin parser without requiring invasive changes to the Java path?**

## Options to evaluate

### (A) Extract `ParserBehaviour` interface from `JavaParser`

As the PoC did: extract an interface with 18 entry points from `JavaParser`. `JavaParser` implements it, `KotlinPsiParser` is another implementation.

**Pros:** Polymorphic dispatch; callers don't care which parser they have.

**Cons:** 18 entry points is large; requires modifying `JavaParser` (risky); `EditorParser extends JavaParser` makes extraction messy; high regression risk.

### (B) Composition via `LanguageRegistry`

`JavaParser` stays untouched. `LanguageRegistry.parserFor(SourceFile)` dispatches to either the existing Java path or a new Kotlin path based on `SourceType`.

**Pros:** Zero changes to `JavaParser`; clean separation; fits existing DI pattern; incremental adoption.

**Cons:** Callers need updating; no shared interface so contract drift is possible.

### (C) Simplification of (A)

Audit which `ParserBehaviour` methods are actually called. Extract a smaller interface.

**Pros:** Smaller, more focused interface; polymorphic dispatch.

**Cons:** Still modifies `JavaParser`; still has the inheritance problem.

## Investigation steps

1. **Audit `EditorParser` and `InfoParser`** to understand which parser entry points they actually call. Map out the call graph from these two classes into `JavaParser`.

2. **Check how `SourceParser` (the callback target) is currently created and wired.** Who creates parsers today? How many callsites?

3. **Prototype the composition approach**: can we wrap `JavaParser` creation in `JavaLanguageSupport.createParser()` without changing `JavaParser` itself? Write a throwaway prototype to validate.

4. **Evaluate incremental parsing**: `EditorParser` needs parser state for incremental re-parsing. Does the composition approach handle this? `EditorParser` extends `JavaParser` (inheritance, not composition) — this is the key challenge.

## Key concern: `EditorParser` inheritance

`EditorParser` extends `JavaParser`. This is inheritance, not composition. The composition approach needs to handle this without refactoring `EditorParser`.

Expected resolution: `EditorParser` stays as-is for the Java path. A separate `KotlinEditorParser` handles the Kotlin path. Both are created via `LanguageSupport.createParser(...)` — the caller doesn't know or care which concrete class it gets. The key insight is that we don't need `JavaParser` and `KotlinPsiParser` to share an interface — they just need to be created by the same dispatch mechanism.

## Deliverable

Short write-up (~1 page) with:
- Recommendation and reasoning
- Call graph from `EditorParser` / `InfoParser` into `JavaParser` (the actual entry points used)
- List of callsites that currently construct parsers directly
- Assessment of the `EditorParser` inheritance issue
- Throwaway prototype if needed to validate the composition approach

Results feed directly into PR-4 planning.
