#!/usr/bin/env bash
# ==============================================================================
# Nell Downloader - Automated Build Script for Termux PRoot Distro
# ==============================================================================

set -e

# ANSI Color Codes
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

BUILD_TYPE="${1:-debug}"

echo -e "${CYAN}======================================================${NC}"
echo -e "${CYAN}  Nell Downloader - Termux PRoot Build System         ${NC}"
echo -e "${CYAN}======================================================${NC}"
echo ""

# 1. Environment Verification
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/android-sdk" ]; then
        export ANDROID_HOME="$HOME/android-sdk"
        export ANDROID_SDK_ROOT="$HOME/android-sdk"
    else
        echo -e "${RED}[!] ANDROID_HOME is not set and $HOME/android-sdk was not found.${NC}"
        echo -e "${YELLOW}Please run: bash setup-termux-proot.sh first!${NC}"
        exit 1
    fi
fi

if [ -z "$JAVA_HOME" ]; then
    if command -v javac >/dev/null 2>&1; then
        export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
    elif command -v java >/dev/null 2>&1; then
        export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    fi
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$JAVA_HOME/bin:$PATH"

echo -e "${GREEN}[*] Environment:${NC}"
echo -e "  • Java Home:    ${JAVA_HOME:-'System Default'}"
echo -e "  • Android Home: $ANDROID_HOME"
echo -e "  • Target Build: $BUILD_TYPE"
echo ""

# 2. Ensure Gradlew and Gradle Wrapper JAR are present
mkdir -p gradle/wrapper
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo -e "${YELLOW}[*] Downloading missing gradle-wrapper.jar...${NC}"
    curl -sLo gradle/wrapper/gradle-wrapper.jar https://repo.maven.apache.org/maven2/org/gradle/gradle-wrapper/8.13/gradle-wrapper-8.13.jar || \
    wget -q -O gradle/wrapper/gradle-wrapper.jar https://repo.maven.apache.org/maven2/org/gradle/gradle-wrapper/8.13/gradle-wrapper-8.13.jar || true
fi
chmod +x ./gradlew 2>/dev/null || true

# 3. Create output directory
OUTPUT_DIR="./build-output"
mkdir -p "$OUTPUT_DIR"

# 4. Handle Build Actions
case "$BUILD_TYPE" in
    clean)
        echo -e "${YELLOW}[*] Cleaning project build artifacts...${NC}"
        ./gradlew clean --no-daemon
        echo -e "${GREEN}[✓] Clean completed.${NC}"
        exit 0
        ;;
    release)
        echo -e "${CYAN}[*] Starting Release APK Build...${NC}"
        # Set JVM args optimized for PRoot Termux memory limits
        export GRADLE_OPTS="-Dorg.gradle.jvmargs=\"-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8\" -Dorg.gradle.workers.max=2"
        ./gradlew assembleRelease --no-daemon --stacktrace
        
        APK_SRC="app/build/outputs/apk/release/app-release.apk"
        if [ -f "$APK_SRC" ]; then
            cp "$APK_SRC" "$OUTPUT_DIR/NellDownloader-release.apk"
            echo -e "${GREEN}[✓] Release APK Build Successful!${NC}"
            echo -e "${GREEN}    Output: $OUTPUT_DIR/NellDownloader-release.apk${NC}"
        fi
        ;;
    debug|*)
        echo -e "${CYAN}[*] Starting Debug APK Build...${NC}"
        # Set JVM args optimized for PRoot Termux memory limits
        export GRADLE_OPTS="-Dorg.gradle.jvmargs=\"-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8\" -Dorg.gradle.workers.max=2"
        
        # Run assembleDebug
        ./gradlew assembleDebug --no-daemon --stacktrace
        
        APK_SRC="app/build/outputs/apk/debug/app-debug.apk"
        if [ -f "$APK_SRC" ]; then
            cp "$APK_SRC" "$OUTPUT_DIR/NellDownloader-debug.apk"
            
            # Check if Android external storage /sdcard/Download is accessible
            if [ -d "/sdcard/Download" ]; then
                cp "$APK_SRC" "/sdcard/Download/NellDownloader-debug.apk" 2>/dev/null && \
                echo -e "${GREEN}    Copied to Phone Storage: /sdcard/Download/NellDownloader-debug.apk${NC}" || true
            elif [ -d "/storage/emulated/0/Download" ]; then
                cp "$APK_SRC" "/storage/emulated/0/Download/NellDownloader-debug.apk" 2>/dev/null && \
                echo -e "${GREEN}    Copied to Phone Storage: /storage/emulated/0/Download/NellDownloader-debug.apk${NC}" || true
            fi
            
            echo ""
            echo -e "${GREEN}======================================================${NC}"
            echo -e "${GREEN}  ✓ Build Succeeded! APK is ready to install.        ${NC}"
            echo -e "${GREEN}======================================================${NC}"
            echo -e "APK Location in PRoot: ${CYAN}$OUTPUT_DIR/NellDownloader-debug.apk${NC}"
        else
            echo -e "${RED}[!] Build finished but APK was not found in expected location ($APK_SRC).${NC}"
        fi
        ;;
esac
