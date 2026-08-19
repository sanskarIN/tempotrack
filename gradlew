#!/bin/sh
set -eu

REQUIRED_GRADLE_VERSION="9.5.0"
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
    INSTALLED_GRADLE_VERSION=$(gradle --version | awk '/^Gradle / { print $2; exit }')
    if [ "$INSTALLED_GRADLE_VERSION" != "$REQUIRED_GRADLE_VERSION" ]; then
        echo "TempoTrack requires Gradle $REQUIRED_GRADLE_VERSION when gradle-wrapper.jar is absent; found ${INSTALLED_GRADLE_VERSION:-unknown}." >&2
        echo "Install Gradle $REQUIRED_GRADLE_VERSION or generate gradle/wrapper/gradle-wrapper.jar with 'gradle wrapper --gradle-version $REQUIRED_GRADLE_VERSION'." >&2
        exit 1
    fi
    exec gradle "$@"
fi

echo "TempoTrack requires Gradle $REQUIRED_GRADLE_VERSION. Install it or generate gradle/wrapper/gradle-wrapper.jar with 'gradle wrapper --gradle-version $REQUIRED_GRADLE_VERSION'." >&2
exit 1
