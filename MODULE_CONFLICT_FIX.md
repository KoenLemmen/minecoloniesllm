# Module Conflict Fix

## Problem
When launching Minecraft with the mod, the following error occurred:
```
java.lang.module.ResolutionException: Modules midnightcontrols and llmconversations export package org.jetbrains.annotations to module forgivingworld
```

This is a **module conflict** error. Multiple mods were trying to export the same `org.jetbrains.annotations` package, which is not allowed in the Java Platform Module System (JPMS).

## Root Cause
The mod was bundling OkHttp (for HTTP requests to the LLM API) using the `shade` configuration. OkHttp has a transitive dependency on JetBrains annotations, which was being included in the final JAR. Other mods in the environment also included this same package, causing a conflict.

## Solution
We excluded the JetBrains annotations from both:
1. The shaded dependency (at the dependency level)
2. The final JAR (at the jar task level)

### Changes Made to `build.gradle`

#### 1. Excluded from Dependency
```groovy
// Before:
shade 'com.squareup.okhttp3:okhttp:4.12.0'

// After:
shade('com.squareup.okhttp3:okhttp:4.12.0') {
    exclude group: 'org.jetbrains', module: 'annotations'
}
```

#### 2. Excluded from JAR
```groovy
tasks.named('jar', Jar) {
    from {
        configurations.shade.collect { it.isDirectory() ? it : zipTree(it) }
    }
    // Added these excludes:
    exclude 'org/jetbrains/annotations/**'
    exclude 'META-INF/versions/9/org/jetbrains/annotations/**'
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

## Why This Works
- **JetBrains annotations are compile-time only**: They're used for IDE hints and static analysis but aren't needed at runtime
- **OkHttp doesn't require them**: OkHttp will work fine without these annotations at runtime
- **Prevents conflicts**: By excluding them, we avoid exporting a package that other mods might also include

## Verification
After rebuilding with `gradlew clean build`:
- ✅ No `org/jetbrains/annotations` found in the JAR
- ✅ OkHttp classes are still present and functional
- ✅ Build succeeds without errors

## Testing
Replace the old JAR in your mods folder with the newly built one:
```
build/libs/llmconversations-1.0.0.jar
```

The module conflict should now be resolved and Minecraft should launch successfully.

