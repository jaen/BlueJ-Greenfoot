# PR-5: Kotlin Lexer Swap + Token Mapping Unification

## Goal

Replace `JavaLexer` + `KotlinKeywords` with Kotlin's own lexer for `.kt` files. Unify the three redundant token mapping mechanisms into one.

## Prerequisites

- **PR-2** (Token Infrastructure) — language-agnostic token queries used by parsing nodes
- **PR-3** (Kotlin Build & Compile) — `kotlin-compiler-embeddable` dependency, `LanguageSupport` interface with `createLexer()` method

## Problem

The PoC has three parallel token mapping mechanisms:

1. **`KotlinKeywords.java`** — string-based keyword -> `JavaTokenTypes` mapping, used by BlueJ's `JavaLexer` to recognize Kotlin keywords. This is a workaround: it bolts Kotlin keyword recognition onto the Java lexer rather than using Kotlin's actual lexer.

2. **`TokenTypeMapper.java`** — `IElementType`-based, uses `==` reference equality against `KtTokens` constants. This is the correct approach, but has dead code: an unused `LEAF_TOKEN_MAP` HashMap sits alongside the runtime if-chains that actually do the work.

3. **`BaseVisitor.guessTokenType()`** — debug-name string matching fallback covering only ~14 types. Fragile and incomplete — relies on `IElementType.toString()` output, which is not part of the public API.

All three exist because they were built incrementally during PoC development. With Kotlin's actual lexer in place, only `TokenTypeMapper` is needed.

## Approach

### 1. Implement `KotlinTokenStream`

A new class that wraps Kotlin's `KotlinLexer` and produces `LocatableToken` instances:

- Advances Kotlin's lexer token-by-token
- Maps each `IElementType` to a BlueJ-compatible token representation (see open question on token types below)
- Tracks line/column positions for each token (Kotlin's lexer provides character offsets; we convert to line/column)

### 2. Wire into `LanguageSupport`

Add `KotlinTokenStream` as the return from `KotlinLanguageSupport.createLexer(SourceFile source)`. The `LanguageSupport` interface takes `SourceFile` (from PR-1), not `Reader` — the common source representation is the API boundary; internals create readers as needed.

### 3. Clean up `TokenTypeMapper`

Choose one approach — either the HashMap or the if-chains — and remove the other. The current state has both, with only the if-chains actually used at runtime. Options:

- **Keep if-chains, remove HashMap**: Simpler, the if-chains are already tested in practice. O(1) via JVM's tableswitch for sequential constants.
- **Keep HashMap, remove if-chains**: Cleaner initialization, O(1) lookup, easier to extend. But the HashMap uses identity keys (`IElementType` singletons), so this is safe.

Either way, remove all dead code paths.

### 4. Remove `KotlinKeywords.java`

No longer needed — we use Kotlin's actual lexer instead of teaching BlueJ's Java lexer about Kotlin keywords.

### 5. Remove `BaseVisitor.guessTokenType()`

Superseded by proper `TokenTypeMapper` lookups. All callers should go through `TokenTypeMapper` instead.

## Key design point

Since `IElementType` singletons are shared between the Kotlin lexer and PSI tree, the token stream from the lexer and the PSI tree leaf nodes can be correlated by **element type identity**. When the lexer produces `KtTokens.FUN_KEYWORD` and a PSI leaf node has element type `KtTokens.FUN_KEYWORD`, they are literally the same object (`==` returns true).

This property is exploited by PR-6c (emit-range filtering) for `skipToToken` synchronization — the token stream position can be matched to a PSI node's position by checking element type identity, rather than requiring fragile text offset arithmetic.

## PoC reference

| File | Location | Size | Notes |
|------|----------|------|-------|
| `KotlinKeywords.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/lexer/KotlinKeywords.java` | ~103 lines | String-based mapping; to be removed |
| `TokenTypeMapper.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/psi/TokenTypeMapper.java` | -- | Has both HashMap and if-chains; clean up |
| `BaseVisitor.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/psi/visitor/BaseVisitor.java` | -- | See `guessTokenType` method; to be removed |

## Acceptance criteria

- [ ] `.kt` files are lexed by Kotlin's `KotlinLexer`, not BlueJ's `JavaLexer`
- [ ] `.java` files continue to use `JavaLexer` unchanged
- [ ] `TokenTypeMapper` is the single token type mapping mechanism (no redundant alternatives)
- [ ] `KotlinKeywords.java` is removed
- [ ] `BaseVisitor.guessTokenType()` is removed
- [ ] Token positions (line/column) are correct for all token types
- [ ] All existing Java tests pass (zero regressions)
- [ ] New tests for `KotlinTokenStream` covering: keywords, operators, string literals, whitespace/comments, multi-line tokens, position tracking

## Estimated effort

**Medium.** The `KotlinTokenStream` implementation is the main work. `TokenTypeMapper` cleanup and removal of redundant code is straightforward. Position tracking (offset -> line/column conversion) requires care but is well-understood.

## Open questions

1. **Should we convert to `JavaTokenTypes` at all?** The current `JavaTokenTypes` integer constants are Java-specific (`LITERAL_class`, `RCURLY`, etc.). An alternative: keep `LocatableToken` as the common token type, but let Java and Kotlin tokens carry their internal type information differently. The Java path continues using `JavaTokenTypes` integers; the Kotlin path could carry `IElementType` natively and only convert at the boundaries where BlueJ infrastructure requires an integer type. This would reduce lossy mapping and let each language's parser work with its natural token representation. Investigate during implementation.

2. **Should `KotlinTokenStream` implement `TokenStream` directly or wrap it?** Direct implementation is simpler and likely sufficient.

3. **Whitespace/comment handling**: For Kotlin it's probably better to keep whitespace/comments in the token stream (rather than filtering them out as `JavaTokenFilter` does for Java), but this should be verified when implementing. The PSI tree retains whitespace, so keeping it in the token stream may simplify synchronization.
