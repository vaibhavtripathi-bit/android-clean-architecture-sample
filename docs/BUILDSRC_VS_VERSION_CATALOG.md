# buildSrc vs Version Catalog (`libs.versions.toml`) — Detailed Comparison

This document compares two approaches to managing dependency versions in Android/Gradle projects. This branch (`feature/buildsrc-dependency-management`) uses the `buildSrc` approach, while the `main` branch uses Gradle Version Catalogs.

---

## Table of Contents

- [What is buildSrc?](#what-is-buildsrc)
- [What is Version Catalog?](#what-is-version-catalog)
- [Side-by-Side Code Comparison](#side-by-side-code-comparison)
- [Detailed Pros and Cons](#detailed-pros-and-cons)
- [Build Cache Impact — The Critical Difference](#build-cache-impact--the-critical-difference)
- [Tooling and Ecosystem Support](#tooling-and-ecosystem-support)
- [When to Use Which](#when-to-use-which)
- [The Modern Alternative: Convention Plugins via Composite Builds](#the-modern-alternative-convention-plugins-via-composite-builds)
- [Recommendation](#recommendation)
- [References](#references)

---

## What is buildSrc?

`buildSrc` is a special Gradle directory that is compiled **before** any build script is evaluated. Kotlin/Java code placed here is available to all `build.gradle.kts` files in the project.

### Structure in this project

```
buildSrc/
├── build.gradle.kts          # Enables kotlin-dsl plugin
└── src/main/kotlin/
    ├── AppConfig.kt           # compileSdk, minSdk, applicationId, etc.
    ├── Versions.kt            # All version strings as constants
    ├── Deps.kt                # Dependency coordinates organized in nested objects
    └── Plugins.kt             # Plugin IDs as constants
```

### How it looks in build.gradle.kts

```kotlin
// Plugin declaration
id(Plugins.ANDROID_APPLICATION)

// App config
namespace = AppConfig.NAMESPACE
compileSdk = AppConfig.COMPILE_SDK

// Dependencies — full IDE autocomplete
implementation(Deps.AndroidX.Lifecycle.RUNTIME_COMPOSE)
implementation(Deps.Hilt.ANDROID)
ksp(Deps.Hilt.COMPILER)
implementation(Deps.Network.RETROFIT)
```

---

## What is Version Catalog?

Gradle Version Catalogs (stable since Gradle 7.4.1) use a TOML file (`gradle/libs.versions.toml`) to declare versions, libraries, and plugins in a declarative format. Gradle generates type-safe accessors automatically.

### Structure on main branch

```
gradle/
└── libs.versions.toml         # Single declarative file
```

### How it looks in build.gradle.kts

```kotlin
// Plugin declaration
alias(libs.plugins.android.application)

// Dependencies — type-safe generated accessors
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.hilt.android)
ksp(libs.hilt.android.compiler)
implementation(libs.retrofit)
```

---

## Side-by-Side Code Comparison

### Declaring a version

| buildSrc (`Versions.kt`) | Version Catalog (`libs.versions.toml`) |
|---|---|
| `const val HILT = "2.53.1"` | `hilt = "2.53.1"` |

### Declaring a dependency

| buildSrc (`Deps.kt`) | Version Catalog (`libs.versions.toml`) |
|---|---|
| `const val ANDROID = "com.google.dagger:hilt-android:${Versions.HILT}"` | `hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }` |

### Using a dependency

| buildSrc | Version Catalog |
|---|---|
| `implementation(Deps.Hilt.ANDROID)` | `implementation(libs.hilt.android)` |

### Declaring a plugin

| buildSrc (`Plugins.kt`) | Version Catalog (`libs.versions.toml`) |
|---|---|
| `const val HILT_ANDROID = "com.google.dagger.hilt.android"` | `hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }` |

### Using a plugin

| buildSrc | Version Catalog |
|---|---|
| `id(Plugins.HILT_ANDROID) version Versions.HILT` | `alias(libs.plugins.hilt.android)` |

---

## Detailed Pros and Cons

### buildSrc — Pros

| Advantage | Details |
|---|---|
| **Full Kotlin power** | You can write functions, extension functions, conditional logic, helper methods. For example, a `fun DependencyHandler.addComposeDeps()` that adds all Compose dependencies at once. |
| **IDE autocomplete** | Typing `Deps.` triggers autocomplete with the full nested object hierarchy. Very discoverable for new team members. |
| **Compile-time safety** | A typo like `Deps.Hlt.ANDROID` is a compile error. With version catalogs, `libs.hlt.android` would also fail, but the error message is less clear. |
| **Shared app config** | `AppConfig.COMPILE_SDK` can be referenced from any module's build script. Version catalogs don't handle non-dependency config. |
| **Logical grouping** | Nested objects (`Deps.AndroidX.Compose.UI`) create a clear hierarchy that mirrors the library structure. |
| **Custom build logic** | You can define convention-like behavior: shared `android {}` blocks, common compiler flags, etc. |

### buildSrc — Cons

| Disadvantage | Details | Severity |
|---|---|---|
| **Build cache invalidation** | **Any change** to any file in `buildSrc/` invalidates the **entire project's build cache**. Change one version number → every module recompiles from scratch. In a multi-module project with 20+ modules, this can add 5-15 minutes to a build. | **Critical** |
| **Slower initial build** | `buildSrc` must compile before any build script is evaluated. Adds ~5-10 seconds to every clean build. | Medium |
| **Not a Gradle standard** | The `Deps`/`Versions` object pattern is a community convention, not a Gradle feature. Every project structures it differently. | Medium |
| **No Dependabot/Renovate support** | GitHub Dependabot and Renovate cannot parse custom Kotlin objects. You lose automated dependency update PRs entirely. | **High** |
| **No bundle support** | Version Catalogs support `[bundles]` to group dependencies declaratively. buildSrc needs custom extension functions to achieve the same. | Low |
| **Maintenance overhead** | You must manually keep `Versions.kt`, `Deps.kt`, and `Plugins.kt` in sync. A version catalog is a single source of truth. | Medium |
| **Community has moved on** | Google's official samples (Now in Android, architecture-samples) have all migrated away from buildSrc to version catalogs. New developers are less likely to encounter this pattern. | Medium |

### Version Catalog — Pros

| Advantage | Details |
|---|---|
| **No build cache invalidation** | Changing a version in `libs.versions.toml` does NOT invalidate the build cache. Only modules that actually use the changed dependency recompile. |
| **Gradle-native** | First-class Gradle feature with dedicated tooling, documentation, and long-term support. |
| **Dependabot/Renovate support** | Both tools natively parse TOML version catalogs and can auto-create PRs for dependency updates. |
| **IDE support** | Android Studio provides autocomplete, navigation, and refactoring for `libs.*` accessors. |
| **Declarative** | TOML is simple, readable, and requires zero Kotlin knowledge to update. |
| **Google-recommended** | Used in Now in Android, official project templates, and all Google codelabs. |
| **Bundles** | `[bundles]` section lets you group related dependencies: `compose = ["ui", "ui-graphics", "material3"]`. |
| **Single file** | One `libs.versions.toml` replaces 3-4 Kotlin files. |

### Version Catalog — Cons

| Disadvantage | Details | Severity |
|---|---|---|
| **No logic** | Cannot write conditional dependencies, helper functions, or shared build configuration. | Medium |
| **TOML syntax** | Less familiar than Kotlin for some developers. Naming conventions (kebab-case) differ from Kotlin (camelCase). | Low |
| **No app config** | Cannot store `compileSdk`, `minSdk`, `applicationId` etc. Those stay as literals in build scripts (or use `extra` properties). | Low |
| **Accessor naming** | Generated accessor names can be verbose: `libs.androidx.lifecycle.runtime.compose`. | Low |

---

## Build Cache Impact — The Critical Difference

This is the single most important factor in the decision.

### Scenario: You bump Hilt from 2.53.1 to 2.54.0

**With buildSrc:**
1. You edit `Versions.kt`: `const val HILT = "2.54.0"`
2. Gradle detects `buildSrc` has changed
3. Gradle recompiles `buildSrc` (~5s)
4. Gradle **invalidates the entire build cache** for all modules
5. Every module recompiles from scratch
6. **Total impact: Full rebuild** (could be 5-15 minutes in a large project)

**With Version Catalog:**
1. You edit `libs.versions.toml`: `hilt = "2.54.0"`
2. Gradle detects only the Hilt dependency changed
3. Only modules that depend on Hilt recompile
4. All other modules use cached outputs
5. **Total impact: Incremental rebuild** (typically seconds)

### Why this happens

`buildSrc` is treated as a **classpath dependency** of every build script. When it changes, Gradle cannot determine which parts of the build are affected, so it conservatively invalidates everything. Version catalogs are metadata files that Gradle can diff at the dependency level.

### Real-world impact

| Project Size | buildSrc bump | Version Catalog bump |
|---|---|---|
| 1 module (this project) | ~30s full rebuild | ~20s incremental |
| 10 modules | ~3-5 min full rebuild | ~30s incremental |
| 30+ modules | ~10-15 min full rebuild | ~1 min incremental |

For a single-module project like this one, the difference is small. For multi-module projects, it's a dealbreaker.

---

## Tooling and Ecosystem Support

| Tool | buildSrc | Version Catalog |
|---|---|---|
| **GitHub Dependabot** | Not supported | Natively supported |
| **Renovate** | Not supported | Natively supported |
| **Android Studio autocomplete** | Full (Kotlin objects) | Full (generated accessors) |
| **Android Studio navigation** | Ctrl+Click → Kotlin file | Ctrl+Click → TOML file |
| **Gradle dependency updates plugin** | Partial (needs custom config) | Full support |
| **Gradle build scan** | Shows buildSrc as classpath dep | Shows catalog metadata |

---

## When to Use Which

### Use Version Catalog (`libs.versions.toml`) when:

- You want the **current industry standard** approach
- You need **Dependabot/Renovate** for automated dependency updates
- You're building a **multi-module** project where build cache matters
- You want **minimal build infrastructure** maintenance
- You're following **Google's official recommendations**

### Use buildSrc when:

- You need **shared build logic** (not just dependency versions) — but consider convention plugins instead
- You're maintaining a **legacy project** that already uses it and migration cost is high
- You need **complex conditional dependency resolution** (rare)
- You want the **best possible IDE autocomplete** experience (marginal benefit)

### Use Convention Plugins via Composite Builds when:

- You need **shared build configuration** across modules (common `android {}` blocks, compiler flags)
- You want the **Kotlin power of buildSrc** without the **cache invalidation penalty**
- You're building a **large multi-module project**
- This is what **Now in Android** uses

---

## The Modern Alternative: Convention Plugins via Composite Builds

For completeness, here's the approach that combines the best of both worlds. This is NOT implemented in this project but is documented for reference.

### Structure

```
build-logic/
├── convention/
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       ├── AndroidApplicationConventionPlugin.kt
│       ├── AndroidLibraryConventionPlugin.kt
│       └── AndroidFeatureConventionPlugin.kt
```

### settings.gradle.kts

```kotlin
pluginManagement {
    includeBuild("build-logic")
}
```

### How it works

- `build-logic/` is an **included build** (not `buildSrc`)
- Changes to `build-logic/` only invalidate modules that use the affected plugin
- Dependency versions still come from `libs.versions.toml`
- Convention plugins handle shared `android {}` config, compiler flags, etc.

### Example convention plugin

```kotlin
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("com.google.dagger.hilt.android")
            }
            // Shared android config, Compose setup, etc.
        }
    }
}
```

Then in a feature module:

```kotlin
plugins {
    id("restaurant.android.feature")  // Your custom convention plugin
}
// No need to repeat android {}, compose setup, or common dependencies
```

---

## Recommendation

### For this project (single module, learning/showcase)

**`main` branch with Version Catalog is the better choice.** The buildSrc approach on this branch exists purely to demonstrate the pattern and understand the tradeoffs.

### For production projects

| Scenario | Recommendation |
|---|---|
| Single module app | Version Catalog only |
| Multi-module app (< 10 modules) | Version Catalog only |
| Multi-module app (10+ modules) | Version Catalog + Convention Plugins via composite builds |
| Legacy project with buildSrc | Migrate to Version Catalog when feasible |

### Migration path if you have buildSrc today

1. Create `gradle/libs.versions.toml` with all versions and dependencies
2. Update `build.gradle.kts` files to use `libs.*` accessors
3. If you have shared build logic in buildSrc, move it to `build-logic/` as convention plugins
4. Delete `buildSrc/`

---

## References

- [Gradle Version Catalogs documentation](https://docs.gradle.org/current/userguide/platforms.html)
- [Now in Android — build-logic](https://github.com/android/nowinandroid/tree/main/build-logic)
- [Migrate from buildSrc to Version Catalogs](https://developer.android.com/build/migrate-to-catalogs)
- [Gradle Convention Plugins](https://docs.gradle.org/current/samples/sample_convention_plugins.html)
- [Dependabot Version Catalog support](https://github.blog/changelog/2023-10-dependabot-now-supports-gradle-version-catalogs/)

---

## Summary Table

| Criteria | buildSrc | Version Catalog | Convention Plugins |
|---|---|---|---|
| Dependency versions | Yes | **Yes (best)** | No (uses catalog) |
| Shared build config | Yes | No | **Yes (best)** |
| Build cache safe | **No** | **Yes** | **Yes** |
| IDE autocomplete | Excellent | Good | Good |
| Dependabot/Renovate | No | **Yes** | N/A |
| Maintenance effort | High (3-4 files) | **Low (1 file)** | Medium |
| Google recommended | No | **Yes** | **Yes** |
| Kotlin logic support | **Yes** | No | **Yes** |
| Community adoption (2024+) | Declining | **Growing** | Growing |
