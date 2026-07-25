#!/bin/bash
# Gradle wrapper script for Hermes Android project
set -e

GRADLE_HOME="${HOME}/.gradle"
GRADLE_VERSION="8.7"
GRADLE_ZIP="gradle-${GRADLE_VERSION}-all.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_ZIP}"
GRADLE_INSTALL="${GRADLE_HOME}/wrapper/dists/gradle-${GRADLE_VERSION}-all/gradle-${GRADLE_VERSION}"

# Download and extract Gradle if not present
if [ ! -d "${GRADLE_INSTALL}" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    mkdir -p "$(dirname "${GRADLE_INSTALL}")"
    curl -fsSL "${GRADLE_URL}" -o "${GRADLE_HOME}/${GRADLE_ZIP}"
    unzip -q "${GRADLE_HOME}/${GRADLE_ZIP}" -d "$(dirname "${GRADLE_INSTALL}")"
fi

exec "${GRADLE_INSTALL}/bin/gradle" "$@"
