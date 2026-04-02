# PR-6c: Emit-Range Filtering + Token Stream Synchronization

## Goal

Add the emit-range filtering layer that enables incremental parsing. When only part of a Kotlin file changes, callbacks are only fired for the affected range, while the token stream advances through the entire file.

## Prerequisites

- **PR-6a** (PsiEnvironment) — PSI tree creation
- **PR-6b** (Callback Adapter Base) — MethodHandle-based callback adapter that this PR wraps
- Benefits from **PR-5** (Kotlin Lexer) — `IElementType`-based token matching simplifies synchronization

## How it works

BlueJ's incremental parser checkpoints parser state and re-parses from a specific position when text changes. For Java, this is handled internally by `JavaParser`'s incremental logic. For Kotlin, the situation is different:

- **Kotlin's PSI tree is always complete.** The Kotlin compiler re-parses the full file on every change (PSI trees are immutable snapshots).
- **BlueJ only needs callbacks for the changed region.** Re-firing all callbacks for the entire file would **corrupt the AST** — callbacks would arrive in an unexpected parser state (the incremental parser has checkpointed state expecting to resume from a specific point), most likely causing exceptions or a broken parse tree.
- **Solution: emit-range filtering.** The adapter wraps every callback with a range check. Only callbacks whose associated token falls within the emit range actually fire. Outside the range, the adapter silently advances the token stream to stay in sync.

The core pattern:

```java
@Override
public void someCallback(LocatableToken token) {
    skipToToken(token);
    if (isInEmitRange(token)) {
        super.someCallback(token);
    }
}
```

## Contents

### `JavaParserCallbacksAdapter.java`

Interface extending `JavaParserCallbacks` with emit-range state management:

- Emit-range boundaries (start line/column, end line/column)
- Token stream access: `getTokenStream()`, `getLastToken()`
- Token synchronization: `skipToToken()` default methods that advance the token stream to match the PSI visitor's current position
- Range checking: `isInEmitRange(LocatableToken token)` determines whether a token falls within the emit range

### `KotlinParserCallbacksAdapterImpl.java`

Extends `JavaParserCallbacksAdapterImpl` (from PR-6b), implements `JavaParserCallbacksAdapter`. Overrides every callback to add emit-range filtering + token stream synchronization.

## Boilerplate analysis

From the PoC investigation, the ~120 callback overrides break down into patterns:

| Pattern | Count | Description |
|---------|-------|-------------|
| Standard single-token | ~65 | `skipToToken(token); if (inRange) super.callback(token);` |
| Token + included flag | ~25 | Same as above, with a boolean `included` parameter |
| List-based | ~8 | Token list parameter; skip to first/last token in list |
| No-token (null check) | ~15 | Callback has no token parameter; use position state to decide |
| Non-trivial | ~8-10 | Custom token selection (e.g., `gotImport` uses `semiColonToken`, `beginInitBlock` uses `lcurly` param) |

If Spike S2 (from PR-6b) showed Proxy is viable, the same Proxy approach may work for the ~65 standard single-token overrides here. The ~8-10 non-trivial methods always need explicit implementations.

## Potential simplification from PR-5

If the Kotlin lexer (PR-5) is already merged, tokens in the stream carry `IElementType` references. This means `skipToToken` can match PSI positions to token stream positions by **element type identity** rather than text offset arithmetic:

```java
void skipToToken(LocatableToken target) {
    while (tokenStream.hasNext()) {
        LocatableToken current = tokenStream.peek();
        if (current.getElementType() == target.getElementType()
                && current.getPosition().equals(target.getPosition())) {
            break;
        }
        tokenStream.advance();
    }
}
```

Without PR-5, synchronization would require character offset matching, which is more fragile across whitespace and comment variations. This is why PR-6c "benefits from" PR-5 even though it doesn't strictly depend on it.

**Additional benefit**: `IElementType` identity matching also makes it easier to **find the starting point in the PSI tree** for incremental parsing. Currently, locating which PSI element corresponds to the BlueJ parser's checkpoint position is fiddly (offset-based matching). With shared `IElementType` tokens, we can correlate the lexer's token stream position to the PSI tree more reliably.

## PoC reference

| File | Location | Size | Notes |
|------|----------|------|-------|
| `JavaParserCallbacksAdapter.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/psi/JavaParserCallbacksAdapter.java` | ~238 lines | Interface with emit-range state |
| `KotlinParserCallbacksAdapterImpl.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/psi/KotlinParserCallbacksAdapterImpl.java` | ~2022 lines | Full emit-range filtering implementation |

## Testing

### `ForwardingCallbackRecorder`

A testing utility that records callback invocations for verification. It implements `JavaParserCallbacks` and captures each call (method name, arguments, order) into a list for assertion. This enables tests like:

```java
// Parse with emit range covering lines 5-10
adapter.setEmitRange(5, 0, 10, Integer.MAX_VALUE);
visitor.visit(psiTree);

// Verify only callbacks within the range were fired
assertThat(recorder.getCallbacks())
    .allMatch(cb -> cb.getLine() >= 5 && cb.getLine() <= 10);
```

### Test scenarios

- Full-file emit range (all callbacks fire — regression baseline)
- Restricted emit range (only range callbacks fire)
- Empty emit range (no callbacks fire, token stream still advances)
- Range at file boundaries (start of file, end of file)
- Range spanning multi-token constructs (e.g., class declaration across multiple lines)
- Token stream synchronization correctness (after filtering, stream position matches expectations)

## Acceptance criteria

- [ ] Emit-range filtering works: callbacks only fire within the specified range
- [ ] Token stream synchronization keeps the BlueJ token stream in sync with PSI visitor position
- [ ] `ForwardingCallbackRecorder` (testing utility) works for capturing callback sequences
- [ ] All existing tests pass (zero regressions)
- [ ] New tests for emit-range filtering with various range configurations

## Estimated effort

**Medium-large.** Similar to PR-6b in volume — ~120 overrides following patterns. The complexity is in the token synchronization logic and ensuring the emit-range boundaries are correctly applied for all callback patterns. The non-trivial ~8-10 methods require careful analysis of which token parameter determines range membership.

## Open questions

1. **Can the emit-range state (start/end line/column) be simplified?** The PoC uses a negative-value trick for the end range — setting `endLine` to a negative value signals "emit everything from startLine to end of file." A cleaner representation might use `OptionalInt` or a dedicated `EmitRange` value type with explicit `unbounded()` semantics.

2. **Should `ForwardingCallbackRecorder` be in this PR or deferred to the visitor PRs (Track F)?** It's a testing utility needed here for emit-range tests, but it's also heavily used by the visitor tests. Including it here means Track F gets it for free; deferring means this PR's tests are simpler but less thorough.

3. **Test corpus cleanup and setup.** The `kotlin-cleanup` branch has test infrastructure that needs cleaning up (~50 failing tests). Consider whether an explicit task for cleaning up and setting up a Kotlin test corpus is worthwhile — corpus cleanup is independent work, while actually adding corpus-based tests belongs either here (PR-6c) or in PR-7 (visitors). At minimum, a curated set of Kotlin source snippets for testing the emit-range filtering should be established.

4. **Thread safety of emit-range state.** The emit range is set before parsing begins and read during parsing. If parsing is always single-threaded per project (which it appears to be), no synchronization is needed. But if the DI-managed `@ProjectScoped` adapter could be accessed concurrently, the range state needs protection. Verify the threading model.
