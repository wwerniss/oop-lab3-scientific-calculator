#!/bin/sh
set -eu
BASE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER="$BASE/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER" ]; then
  echo 'Downloading official Gradle 9.6.0 wrapper launcher...'
  if command -v curl >/dev/null 2>&1; then
    curl -fLsS https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar -o "$WRAPPER"
  elif command -v wget >/dev/null 2>&1; then
    wget -q https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar -O "$WRAPPER"
  else
    echo 'Install curl/wget to download the Gradle wrapper launcher' >&2; exit 1
  fi
fi
if command -v sha256sum >/dev/null 2>&1; then
  echo "497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7  $WRAPPER" | sha256sum --check --status || { echo "Gradle wrapper checksum mismatch" >&2; exit 1; }
fi
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" -classpath "$WRAPPER" org.gradle.wrapper.GradleWrapperMain "$@"
