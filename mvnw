#!/bin/sh

set -e

MAVEN_VERSION=3.9.9
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
ARCHIVE="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION-bin.zip"
DIST_DIR="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"
MVN_BIN="$DIST_DIR/bin/mvn"
DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"

if [ ! -x "$MVN_BIN" ]; then
  mkdir -p "$WRAPPER_DIR"

  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DOWNLOAD_URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DOWNLOAD_URL" -O "$ARCHIVE"
  else
    echo "Neither curl nor wget is available to download Maven." >&2
    exit 1
  fi

  rm -rf "$DIST_DIR"
  (
    cd "$WRAPPER_DIR"
    jar xf "$(basename "$ARCHIVE")"
  )
  chmod +x "$MVN_BIN"
fi

exec "$MVN_BIN" "$@"
