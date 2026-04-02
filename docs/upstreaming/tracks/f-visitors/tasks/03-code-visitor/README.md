# PR-9: CodeVisitor

## Goal

Handle function declarations, method bodies, and all statements/expressions. This is the merged `FunctionVisitor` + `MethodBodyVisitor` with scope tracking. After this PR, BlueJ can fully parse Kotlin executable code.

## Prerequisites

- **PR-7** (BaseVisitor + FileVisitor) — CodeVisitor extends BaseVisitor and is dispatched by FileVisitor (for top-level functions) and ClassVisitor (for methods).

## Can run in parallel with PR-8 (ClassVisitor)

CodeVisitor and ClassVisitor depend only on PR-7. ClassVisitor dispatches to CodeVisitor at runtime, but the implementation work is independent.

## Scope tracking

CodeVisitor carries a scope enum that replaces the PoC's separate `FunctionVisitor` + `MethodBodyVisitor` classes:

| Scope | Context | Callback behavior |
|-------|---------|-------------------|
| `FILE_LEVEL` | Top-level function (static method on facade class `FooKt`) | Emits function declaration callbacks (`gotMethodDeclaration`, `endMethodDecl`) |
| `CLASS_LEVEL` | Class method | Emits function declaration callbacks |
| `BODY_LEVEL` | Nested block, lambda body, local function | Local variable callbacks, nested expression handling |

The scope affects callback selection:

- At `FILE_LEVEL` or `CLASS_LEVEL`: emit function declaration callbacks
- Property at `FILE_LEVEL`: handled by FileVisitor as facade class field, or by CodeVisitor as a local variable at `BODY_LEVEL`
- The `topLevelFunction` flag from the PoC (which was dead code — always `false`) is replaced by this enum

## What CodeVisitor handles

### Function signature processing

- Modifiers (uses `extractModifiers()` from BaseVisitor)
- Return type
- Type parameters (generics)
- Function name
- Parameters (uses shared helper from BaseVisitor — the deduplicated version from PR-7/PR-8)
- Body delegation: block body (`{ ... }`) vs expression body (`= expr`)

### Statements

| Statement | Kotlin syntax | Callback pattern |
|-----------|---------------|------------------|
| If/else | `if (cond) { ... } else { ... }` | `beginIfStmt` / chained else-if handling |
| For loop | `for (item in collection) { ... }` | `beginForLoop` (Kotlin for-in, not C-style) |
| While | `while (cond) { ... }` | `beginWhileLoop` |
| Do-while | `do { ... } while (cond)` | `beginDoWhile` |
| When | `when (subject) { ... }` | Mapped to `beginSwitchStmt` callbacks |
| Try/catch/finally | `try { ... } catch (e: Ex) { ... }` | `beginTryCatchStmt` |
| Return | `return expr` | Return statement handling |
| Throw | `throw expr` | Throw statement handling |
| Break/continue | `break@label` / `continue@label` | With optional label support |
| Local variables | `val x = ...` / `var y = ...` | `beginVariableDecl` / `gotVariableDecl` / `endVariable` / `endVariableDecls` |
| Block expressions | `{ ... }` (standalone blocks) | Block scope handling |

### Expressions

| Expression | Kotlin syntax | Notes |
|------------|---------------|-------|
| Literals | `42`, `"hello"`, `true`, `null` | `gotLiteral` callback |
| Identifiers | `foo`, `bar` | `gotIdentifier` callback |
| Binary operators | `a + b`, `a && b`, `a ?: b` | `gotBinaryOperator` (includes Elvis `?:`) |
| Unary operators | `!a`, `-b`, `a++` | Prefix and postfix |
| Call expressions | `foo(a, b)` | `gotMethodCall` + `beginArgumentList` |
| Trailing lambdas | `list.map { it * 2 }` | Lambda as last argument |
| Dot-qualified | `a.b.c` | Member access chains |
| Safe-qualified | `a?.b` | Null-safe member access |
| Array access | `a[i]` | Index operator |
| Lambda expressions | `{ x, y -> x + y }` | `beginLambdaBody` + parameter handling |
| String templates | `"Hello, $name"` / `"${expr}"` | May need special token handling |
| This/super | `this`, `super`, `this@Outer` | With optional label |
| Type casts | `x as Type`, `x is Type` | Cast and type check |
| Object literals | `object : Interface { ... }` | Anonymous class creation |
| Parenthesized | `(expr)` | Grouping |

## Callbacks unique to CodeVisitor

These callbacks are primarily fired by CodeVisitor (not FileVisitor or ClassVisitor):

**Function declarations:**
- `gotMethodDeclaration`, `endMethodDecl`

**Local variables:**
- `beginVariableDecl`, `gotVariableDecl`, `endVariable`, `endVariableDecls`

**Statements:**
- `beginIfStmt`, `beginForLoop`, `beginWhileLoop`, `beginDoWhile`
- `beginSwitchStmt` (for `when` expressions)
- `beginTryCatchStmt`

**Expressions:**
- `beginExpression`, `endExpression`
- `gotLiteral`, `gotIdentifier`
- `gotBinaryOperator`
- `gotMethodCall`, `beginArgumentList`
- `beginLambdaBody`

## PoC reference

| File | Location | Size | Notes |
|------|----------|------|-------|
| `FunctionVisitor.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/psi/visitor/FunctionVisitor.java` | ~220 lines | Function signature processing; merges into CodeVisitor |
| `MethodBodyVisitor.java` | `repos/BlueJ-Greenfoot/kotlin-cleanup/bluej/src/main/java/bluej/parser/psi/visitor/MethodBodyVisitor.java` | ~1121 lines | Method body, statements, expressions; merges into CodeVisitor |

Combined PoC size: ~1341 lines. The merged CodeVisitor should be smaller due to eliminated duplication and the dead `topLevelFunction` flag removal, but this is still the largest single visitor.

## Acceptance criteria

- [ ] Function declarations work at both file level and class level
- [ ] Scope tracking correctly affects callback selection (FILE_LEVEL vs CLASS_LEVEL vs BODY_LEVEL)
- [ ] All statement types handled:
  - [ ] `if` / `else` (including chained else-if)
  - [ ] `for` loops (Kotlin for-in)
  - [ ] `while` and `do-while` loops
  - [ ] `when` expressions (mapped to switch callbacks)
  - [ ] `try` / `catch` / `finally`
  - [ ] `return`, `throw`
  - [ ] `break` / `continue` (with labels)
- [ ] All expression types handled (at minimum):
  - [ ] Literals, identifiers
  - [ ] Binary and unary operators
  - [ ] Call expressions with argument lists
  - [ ] Member access (dot-qualified and safe-qualified)
  - [ ] Lambda expressions with parameters
  - [ ] Type casts (`as` / `is`)
- [ ] Local variable declarations work (`val` / `var` in body context → variable callbacks, not field callbacks)
- [ ] Expression-bodied functions (`fun foo() = expr`) handled correctly
- [ ] All existing tests pass (zero regressions)
- [ ] New tests: comprehensive coverage of Kotlin statement and expression types:
  - Function with block body
  - Function with expression body
  - All statement types
  - Nested expressions (call within call, member access chains)
  - Lambda with parameters and trailing lambda syntax
  - Local variable declarations with various types

## Estimated effort

**Large.** Combined PoC size is ~1341 lines across two files. The merge should reduce this, but the sheer variety of statement and expression types makes this the largest single PR in the track. Expression handling (especially call expressions with trailing lambdas and member access chains) is where most of the complexity lives.

## Open questions

1. **Should `when` expressions map to switch callbacks, or do they need their own callback type?** The PoC maps `when` to switch callbacks, which works but is a simplification — Kotlin's `when` is more expressive than Java's `switch` (arbitrary boolean conditions, no fall-through, pattern matching). If BlueJ only needs basic structural information, switch callbacks may be sufficient. If downstream consumers need to distinguish `when` from `switch`, a new callback type may be needed.

2. **How should expression-bodied functions (`fun foo() = expr`) be handled?** The PoC treats them as a method body with a single expression, which seems reasonable. But should the expression body be wrapped in synthetic `beginMethodBody` / `endMethodBody` callbacks, or handled differently?

3. **Should local function declarations (functions inside functions) be handled in this PR or deferred?** Kotlin allows declaring functions inside other functions. These are not common in typical BlueJ usage, but they exist in the language. A recursive CodeVisitor call with BODY_LEVEL scope would handle them, but the callback implications need investigation.

4. **String template handling.** The PoC has some support for string templates, but string interpolation (`"Hello, $name"` / `"${expr}"`) may need special token handling. Simple `$name` interpolation produces different PSI structure than `${expr}` interpolation. Clarify whether these need distinct handling or if treating the entire string as a single literal token is sufficient for BlueJ's needs.

5. **Trailing lambda desugaring.** When a function call has a trailing lambda (`list.map { ... }`), the PSI represents the lambda outside the argument list. The callback protocol expects arguments in a `beginArgumentList` / `endArgumentList` pair. Need to decide whether to desugar trailing lambdas into the argument list or handle them separately.
