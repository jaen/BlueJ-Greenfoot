# Track D: Lexer

## Goal

Replace BlueJ's Java lexer (with Kotlin keyword additions) with Kotlin's own lexer for `.kt` files, and unify the token mapping into a single mechanism.

## PRs

| PR | Summary | Status |
|----|---------|--------|
| [PR-5: Kotlin Lexer Swap + Token Mapping Unification](tasks/01-kotlin-lexer/README.md) | Replace `JavaLexer` + `KotlinKeywords` with Kotlin's lexer, unify `TokenTypeMapper` | Pending |

## Dependencies

This track depends on:
- **PR-2** (Track A: Token Infrastructure) — language-agnostic token queries that parsing nodes use
- **PR-3** (Track B: Build & Compile) — `kotlin-compiler-embeddable` dependency and `LanguageSupport` interface

Other tracks depend on this track's output:
- **PR-6c** (Track E: Emit-range filtering) benefits from PR-5 — `IElementType`-based token matching simplifies token stream synchronization

## Key insight

Kotlin lexer tokens and PSI leaf node element types are the **same `IElementType` singletons**. When Kotlin's lexer produces `KtTokens.FUN_KEYWORD`, that is the exact same object reference you find on a PSI leaf node for `fun`. This means:

- `TokenTypeMapper` (which uses `==` reference equality against `KtTokens` constants) becomes the **single source of truth** for mapping Kotlin element types to BlueJ token types.
- `KotlinKeywords` (string-based mapping) becomes redundant — we have the real lexer now.
- `BaseVisitor.guessTokenType()` (debug-name string matching) becomes redundant — `TokenTypeMapper` handles it properly.

After this track, there is exactly **one** token mapping mechanism instead of three.

## Shared context

This is a relatively focused PR — the main work is implementing `KotlinTokenStream` as a wrapper around Kotlin's `KotlinLexer`, wiring it into `LanguageSupport.createLexer()`, and cleaning up the redundant mapping code. The complexity is moderate, concentrated in getting token positions (line/column) correct and ensuring `TokenTypeMapper` covers all token types cleanly.
