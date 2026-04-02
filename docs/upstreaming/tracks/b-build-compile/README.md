# Track B: Build & Compile

## Goal

Get BlueJ able to recognize, compile, and handle `.kt` files — without parsing them. Also introduce the `LanguageSupport` interface skeleton and `LanguageRegistry` that subsequent tracks build on.

## PRs

| PR | Summary | Status |
|----|---------|--------|
| [PR-3: Kotlin Build, Compilation, and Workbench](tasks/01-kotlin-build-compile/README.md) | `KotlinCompiler`, file handling, `LanguageSupport` skeleton, `LanguageRegistry` | Pending |

## Independence

This track is **independent of all other tracks** and can start immediately. It has no prerequisites — no dependency on Track A's source model or Track C's parser dispatch.

Other tracks depend on this track's output:
- **PR-5** (Track D: Lexer) needs PR-3 for Kotlin dependencies and `LanguageSupport`
- **PR-6a** (Track E: PSI Infrastructure) needs PR-3 for `LanguageRegistry`
- **PR-4** (Track C: Parser Dispatch) builds on the `LanguageRegistry` introduced here

## Shared context

This is mostly porting existing code from:
- The **PoC** (`repos/BlueJ-Greenfoot/kotlin-cleanup/`) — `KotlinCompiler`, file filters, roles, templates
- The **upstream `kotlin` branch** (Neil/Vitalij's work) — alternative versions of the same files

Minor cleanup and DI integration is needed to fit the `kotlin-support` branch's Guice-based architecture. The PoC uses static singletons; this track replaces them with `@ProjectScoped` DI-managed components.

## Key deliverables

1. **Kotlin compilation** — `KotlinCompiler` wrapping `K2JVMCompiler`, translating diagnostics to BlueJ's `Diagnostic` format
2. **File handling** — `KotlinSourceFilter`, `KotlinFileFacadeRole`, file templates
3. **Class context** — `KotlinContext`, `KotlinContextFactory`
4. **Reflection utilities** — `KotlinMetadataReader`, `KotlinReflective`
5. **`LanguageSupport` interface** — skeleton with `getCompiler()` initially
6. **`LanguageRegistry`** — `@ProjectScoped` registry mapping `SourceType` -> `LanguageSupport`
