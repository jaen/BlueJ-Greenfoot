# PR-2: Token Infrastructure

## Goal

Extract Java-specific assumptions from parsing nodes into language-aware abstractions, so partial parsing nodes work for both Java and Kotlin.

## Prerequisites

None (independent of PR-1).

## Problem

Currently, parsing nodes like `MethodBodyNode` check `token.type == JavaTokenTypes.RCURLY` to decide if a scope ends. In Kotlin, scopes don't always end with `}` (expression-bodied functions, single-expression `when` branches, etc.). These hardcoded Java token type checks are scattered across the incremental parsing infrastructure.

This means the parsing node infrastructure -- which handles scope tracking, incremental re-parsing, and damage repair -- is implicitly Java-only. Before Kotlin tokens can flow through these nodes (PR-5: Kotlin lexer swap), the Java-specific checks need to be behind an abstraction.

## Approach

1. **Introduce language-aware token queries** -- e.g., `token.finishesScope()` or a `TokenClassifier` interface that parsing nodes query instead of comparing against specific `JavaTokenTypes` constants.

2. **Audit parsing nodes for hardcoded checks** -- systematically identify every direct `JavaTokenTypes` comparison in the parsing node classes listed below.

3. **Replace with abstraction calls** -- swap each hardcoded check with a call through the new abstraction.

4. **This is a pure Java refactor** -- no Kotlin code is introduced. The abstraction is designed so that PR-5 can later provide a Kotlin-specific implementation.

## Key files to audit

These files (on `kotlin-support` / `main`) contain the hardcoded token type checks:

| File | Lines | Notes |
|------|-------|-------|
| `bluej/src/main/java/bluej/parser/nodes/IncrementalParsingNode.java` | ~801 | Core incremental parsing logic |
| `bluej/src/main/java/bluej/parser/nodes/MethodBodyNode.java` | -- | Method body scope tracking |
| `bluej/src/main/java/bluej/parser/nodes/ParsedTypeNode.java` | -- | Type-level node |
| `bluej/src/main/java/bluej/parser/nodes/TypeInnerNode.java` | -- | Type inner body |
| `bluej/src/main/java/bluej/parser/nodes/MethodNode.java` | -- | Method-level node |

## Design consideration

The token infrastructure from this PR is consumed by **PR-5 (Kotlin lexer swap)** to ensure partial parsing nodes work with Kotlin tokens. The abstraction should be kept simple:

- We don't need to predict all future token queries.
- We just need to make the _current_ Java-specific ones pluggable.
- PR-5 will implement Kotlin-specific behavior against whatever interface we define here.

## Acceptance criteria

- [ ] No direct `JavaTokenTypes.RCURLY` (or similar) comparisons remain in parsing node classes
- [ ] All token type checks go through a language-aware abstraction
- [ ] All existing Java tests pass unchanged (zero regression)
- [ ] The abstraction is simple enough that PR-5 can implement Kotlin-specific behavior without rework

## Estimated effort

**Small-medium.** The scope depends on how many distinct hardcoded checks exist (see open questions). The refactor itself is mechanical once the abstraction is designed.

## Open questions

1. **Abstraction shape and contextuality**: The abstraction should likely be **contextual** — whether a token finishes a scope may depend on the parsing context (e.g., `}` always finishes a scope in Java, but in Kotlin an expression-bodied function has no `}`). Options:
   - A separate DI-injectable `TokenClassifier` — e.g., `classifier.canFinish(token, scope)` — swappable per-language. Most flexible and DI-friendly.
   - A field on the token: `token.classifier.canFinish(scope)` — ergonomic, keeps the classifier close to usage.
   - A method on the token: `token.finishesScope()` — simplest but not contextual.
   
   Start with the simplest approach that handles current needs. Don't over-engineer for hypothetical future contexts, but keep contextuality in mind so the abstraction doesn't need replacing when we encounter a case that requires it.

2. **Scope of hardcoded checks**: How many distinct hardcoded `JavaTokenTypes` comparisons actually exist across the parsing node classes? This needs an audit before implementation begins. The answer determines whether the abstraction needs 3 methods or 15.
