#!/usr/bin/env bash
set -e

# Setup environment for Java and Android SDK tools
if [ -d "/home/inan/.jdks/temurin-21" ]; then
    export JAVA_HOME="/home/inan/.jdks/temurin-21"
elif [ -d "/home/inan/.jdks/jbr-21.0.11" ]; then
    export JAVA_HOME="/home/inan/.jdks/jbr-21.0.11"
else
    export JAVA_HOME="${JAVA_HOME:-/home/inan/.jdks/temurin-21}"
fi

export ANDROID_HOME="${ANDROID_HOME:-/home/inan/Android/Sdk}"
export PATH="${ANDROID_HOME}/platform-tools:${JAVA_HOME}/bin:${PATH}"

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "==> Using Java: $JAVA_HOME"
echo "==> Using Android SDK: $ANDROID_HOME"
echo "==> Building Play Release APK..."
./gradlew assemblePlayRelease

echo "==> Locating Play Release APK..."
APK_DIRS=(
    "/home/inan/.gradle-builds/Rivo4/app/outputs/apk/play/release"
    "$PROJECT_DIR/app/build/outputs/apk/play/release"
)

APK_PATH=""
for dir in "${APK_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        FOUND=$(find "$dir" -maxdepth 1 -type f -name "*.apk" | head -n 1)
        if [ -n "$FOUND" ] && [ -f "$FOUND" ]; then
            APK_PATH="$FOUND"
            break
        fi
    fi
done

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    echo "ERROR: Release APK not found in ${APK_DIRS[*]}" >&2
    exit 1
fi

echo "==> Release APK found: $APK_PATH"

echo "==> Checking for connected ADB devices..."
DEVICES=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')

if [ -z "$DEVICES" ]; then
    echo "WARNING: No connected Android devices detected via adb."
    echo "         Please connect your device or enable USB debugging, then run:"
    echo "         adb install -r -d -g \"$APK_PATH\""
    exit 0
fi

echo "==> Installing to device(s):"
for device in $DEVICES; do
    echo "    Installing on $device..."
    adb -s "$device" install -r -d -g "$APK_PATH"
    echo "    Installed successfully on $device!"
done

echo "==> Done!"
