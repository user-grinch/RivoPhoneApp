#!/usr/bin/env bash
set -e

# Setup environment for Java and Android SDK tools
export JAVA_HOME="${JAVA_HOME:-/home/inan/.jdks/jbr-21.0.11}"
export ANDROID_HOME="${ANDROID_HOME:-/home/inan/Android/Sdk}"
export PATH="${ANDROID_HOME}/platform-tools:${JAVA_HOME}/bin:${PATH}"

PROJECT_DIR="/mnt/deb/Projects/RivoPhoneApp"
APK_DIR="/home/inan/.gradle-builds/Rivo4/app/outputs/apk/play/release"

echo "==> Building Play Release APK..."
cd "$PROJECT_DIR"
./gradlew assemblePlayRelease

echo "==> Finding Release APK in $APK_DIR..."
APK_PATH=$(find "$APK_DIR" -maxdepth 1 -type f -name "*.apk" | head -n 1)

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    echo "ERROR: Release APK not found in $APK_DIR" >&2
    exit 1
fi

echo "==> Release APK found: $APK_PATH"

echo "==> Checking for connected ADB devices..."
DEVICES=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')

if [ -z "$DEVICES" ]; then
    echo "ERROR: No connected Android devices detected via adb. Please plug in or connect your device." >&2
    exit 1
fi

echo "==> Installing to device(s):"
for device in $DEVICES; do
    echo "    Installing on $device..."
    adb -s "$device" install -r -d -g "$APK_PATH"
    echo "    Installed successfully on $device!"
done

echo "==> Done!"
