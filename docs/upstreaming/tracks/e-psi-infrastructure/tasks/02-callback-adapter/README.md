# PR-6b: Callback Adapter Base (MethodHandle Binding)

## Goal

Introduce the MethodHandle-based callback adapter that allows PSI visitors to invoke BlueJ's parser callback methods without triggering threadchecker issues. The exact target of the MethodHandle bindings (currently `SourceParser` in the PoC) depends on the parser abstraction chosen in Spike S1 / PR-4 — we may end up binding to a different or refactored callback target.

## Prerequisites

- **PR-4** (Parser Dispatch) — defines the callback interface shape via parser abstraction

## Why MethodHandles

BlueJ's `JavaParserCallbacksBase` methods are package-private/protected, and invoking them from the PSI visitor thread would trigger the threadchecker (which enforces that certain methods run on the GUI thread). The MethodHandle approach solves this:

1. **At construction time**: Use `MethodHandles.privateLookupIn(targetClass, lookup)` to get a lookup with access to private/protected members. Then `findVirtual(...)` + `bindTo(targetInstance)` to create pre-bound method handles for each of the ~120 callback methods.

2. **At runtime**: `mhCallback.invokeExact(args)` invokes the method directly — no access checks, no boxing, no threadchecker annotation scanning. The JIT compiles this to a near-native virtual call.

This is a **deliberate design choice** preserved from the PoC. The alternatives are worse:

- **Direct calls**: Trigger threadchecker on the wrong thread.
- **`Method.invoke()`**: Per-call overhead for access checks + boxing. With ~120 methods invoked thousands of times per parse, this is measurable.
- **Thread marshalling**: Moving every callback to the GUI thread would serialize parsing and destroy performance.

## Contents

### Test utilities (for use by PR-6c and Track F)

This PR also introduces the core test utilities that depend on the `JavaParserCallbacks` interface:

- **`CallbackRecorder`** — captures callback invocations (method name, arguments, order) for assertion
- **`PairingValidator`** — validates that begin/end callback pairs are balanced
- **`ForwardingCallbackRecorder`** (~1006 lines) — dual-path adapter that records callbacks AND forwards them to a delegate. Enables simultaneous test validation and integration testing.

These are ported from the PoC and are first needed by PR-6c (emit-range filtering tests), then used extensively by all visitor PRs in Track F.

### `JavaParserCallbacks.java`

Interface extracted from `JavaParserCallbacksBase`, defining ~100+ callback methods. This is the contract that PSI visitors program against:

```java
public interface JavaParserCallbacks {
    void gotPackage(LocatableToken keyword, List<LocatableToken> nameTokens, LocatableToken semi);
    void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken semi);
    void beginTypeBody(LocatableToken lcurly);
    void endTypeBody(LocatableToken rcurly, boolean included);
    // ... ~100+ more
}
```

### `JavaParserCallbacksAdapterImpl.java`

Implements `JavaParserCallbacks` via pre-bound MethodHandles. Each of the ~120 override methods follows the pattern:

```java
@Override
public void gotPackage(LocatableToken keyword, List<LocatableToken> nameTokens, LocatableToken semi) {
    try {
        mhGotPackage.invokeExact(keyword, nameTokens, semi);
    } catch (Throwable t) {
        handleCallbackError("gotPackage", t);
    }
}
```

The constructor uses a `findAndBind()` helper to look up and pre-bind each method:

```java
private MethodHandle findAndBind(String name, MethodType type) {
    return lookup.findVirtual(targetClass, name, type).bindTo(target);
}
```

### Spike S2: Proxy-based alternative

**Embedded in this PR** — before writing ~120 boilerplate overrides, investigate whether `java.lang.reflect.Proxy` with a `Map<Method, MethodHandle>` dispatch can replace the boilerplate:

```java
JavaParserCallbacks proxy = (JavaParserCallbacks) Proxy.newProxyInstance(
    classLoader,
    new Class<?>[] { JavaParserCallbacks.class },
    (proxyObj, method, args) -> methodHandleMap.get(method).invokeWithArguments(args)
);
```

The ~8-10 methods with non-trivial logic (custom argument transformation, conditional dispatch, etc.) get explicit overrides; everything else goes through the proxy.

**Evaluation criteria for Spike S2:**
- Performance: Is `Proxy.invoke` + `invokeWithArguments` fast enough? (Compare to `invokeExact` — `invokeWithArguments` boxes primitives)
- Complexity: Is the Proxy setup actually simpler than the boilerplate?
- Debuggability: Can we get meaningful stack traces through the proxy?
- Maintainability: Is it easier to add/modify callbacks?

If Proxy has unacceptable overhead or complexity, accept the verbosity — ~120 mechanical overrides are tedious but correct and fast.

## DI integration

The adapter can be `@ProjectScoped` with `@Inject` dependencies, eliminating manual wiring in the parser. The callback target instance is injected, and the adapter pre-binds its methods at construction time. The exact target type depends on Spike S1 / PR-4 — it may remain `SourceParser` or become a new abstraction.

## PoC reference

| File | Location | Size | Notes |
|------|----------|------|-------|
| `JavaParserCallbacks.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/psi/JavaParserCallbacks.java` | ~634 lines | Interface definition |
| `JavaParserCallbacksBase.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/JavaParserCallbacksBase.java` | ~540 lines | Existing base class with the methods to bind |
| `JavaParserCallbacksAdapterImpl.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/psi/JavaParserCallbacksAdapterImpl.java` | ~1927 lines | MethodHandle adapter implementation |

## Acceptance criteria

- [ ] `JavaParserCallbacks` interface exists, extracted from `JavaParserCallbacksBase`
- [ ] `JavaParserCallbacksAdapterImpl` correctly binds all callback methods via MethodHandles
- [ ] Spike S2 results documented (Proxy viable or not, with performance/complexity analysis)
- [ ] If Proxy viable: most methods go through proxy, ~8-10 have explicit overrides
- [ ] If Proxy not viable: all ~120 methods have explicit MethodHandle-based overrides
- [ ] All existing tests pass (zero regressions)
- [ ] New tests verifying adapter correctly forwards callbacks (round-trip: invoke adapter method -> verify target method was called with correct args)

## Estimated effort

**Medium-large.** The design work (interface extraction, MethodHandle binding pattern, Spike S2) is moderate. The implementation is largely mechanical — ~120 override methods following a fixed pattern — but the volume is significant. If Spike S2 shows Proxy is viable, the implementation shrinks considerably.

## Open questions

1. **Should `JavaParserCallbacks` be extracted as a pure interface, or should it extend/replace `JavaParserCallbacksBase`?** A pure interface is cleaner (no coupling to the existing class hierarchy), but extending `JavaParserCallbacksBase` would avoid breaking existing code that references the base class. The PoC uses a pure interface — likely the right call.

2. **Can we reduce the ~120 `findAndBind()` calls in the constructor with reflection over the interface methods?** Something like:
   ```java
   for (Method m : JavaParserCallbacks.class.getMethods()) {
       handles.put(m.getName(), findAndBind(m.getName(), methodType(m)));
   }
   ```
   This would eliminate the manual listing of every method in the constructor. The downside is that `findAndBind` calls with explicit `MethodType` arguments are type-safe at compile time, while reflective discovery defers type checking to runtime.

3. **Should `JavaParserCallbacks` define the non-trivial ~8-10 methods as `default` methods on the interface?** This would let the Proxy handle the simple forwarding while non-trivial methods get real implementations. However, `default` methods on the interface would bake in implementation details that may differ between Java and Kotlin paths.
