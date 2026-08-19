#!/bin/sh
set -eu

APP_HOME=$(cd "${0%/*}" >/dev/null 2>&1 && pwd -P)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -f "$WRAPPER_JAR" ]; then
    if [ -n "${JAVA_HOME:-}" ]; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        JAVACMD=java
    fi
    exec "$JAVACMD" -Xmx64m -Xms64m -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi

if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

echo "TempoTrack requires Gradle 9.5.0. Install it or generate gradle/wrapper/gradle-wrapper.jar with 'gradle wrapper --gradle-version 9.5.0'." >&2
exit 1
