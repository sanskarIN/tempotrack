# ADR 0003: Separate Android app from the KMP shared module

- Status: Accepted
- Date: 2026-08-19

## Context

Android Gradle Plugin 9 no longer supports combining `com.android.application` and Kotlin Multiplatform in one module. The supported KMP Android target uses `com.android.kotlin.multiplatform.library`.

## Decision

Use:

- `shared` with Kotlin Multiplatform + Android-KMP library + Desktop JVM target;
- `androidApp` as a separate Android application;
- `desktopApp` as a separate JVM/Compose Desktop application.

## Consequences

The structure follows the modern KMP direction, keeps platform entry points small, and is ready for another platform application module later.
