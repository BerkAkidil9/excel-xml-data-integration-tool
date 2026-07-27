#!/bin/sh

set -e

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_PROPERTIES="$WRAPPER_DIR/maven-wrapper.properties"

if [ -z "$JAVA_HOME" ]; then
  JAVA_EXE=java
else
  JAVA_EXE="$JAVA_HOME/bin/java"
fi

if ! command -v "$JAVA_EXE" >/dev/null 2>&1; then
  echo "Error: Java Runtime is required to run Maven Wrapper." >&2
  exit 1
fi

if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_URL=$(sed -n 's/^wrapperUrl=//p' "$WRAPPER_PROPERTIES")
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
  else
    echo "Error: curl or wget is required to download Maven Wrapper." >&2
    exit 1
  fi
fi

exec "$JAVA_EXE" "-Dmaven.multiModuleProjectDirectory=$BASE_DIR" \
  -classpath "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
