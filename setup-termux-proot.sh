#!/usr/bin/env bash
# ==============================================================================
# Nell Downloader - Automated Setup Script for Termux PRoot Distro (Ubuntu/Debian)
# ==============================================================================

set -e

# ANSI Color Codes
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}======================================================${NC}"
echo -e "${CYAN}  Nell Downloader - Termux PRoot Distro Setup Script  ${NC}"
echo -e "${CYAN}======================================================${NC}"
echo ""

# 1. Check if running inside PRoot / Linux environment
if [ ! -f /etc/os-release ]; then
    echo -e "${YELLOW}[!] Warning: /etc/os-release not found. Make sure you are inside PRoot Distro (e.g. proot-distro login ubuntu).${NC}"
fi

echo -e "${GREEN}[1/5] Updating system packages & installing required tools...${NC}"
if command -v apt-get >/dev/null 2>&1; then
    apt-get update -y
    apt-get install -y openjdk-17-jdk wget curl unzip git zip tar ca-certificates
elif command -v pacman >/dev/null 2>&1; then
    pacman -Syu --noconfirm jdk17-openjdk wget curl unzip git zip tar ca-certificates
elif command -v apk >/dev/null 2>&1; then
    apk update
    apk add openjdk17 wget curl unzip git zip tar ca-certificates bash
else
    echo -e "${YELLOW}[!] Unknown package manager. Please ensure OpenJDK 17, wget, curl, unzip, git, and tar are installed.${NC}"
fi

# 2. Setup Android SDK directory
echo -e "${GREEN}[2/5] Configuring Android SDK environment...${NC}"
SDK_DIR="${HOME}/android-sdk"
mkdir -p "$SDK_DIR/cmdline-tools"

export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac 2>/dev/null || which java))))
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$JAVA_HOME/bin:$PATH"

# 3. Download Android Command-Line Tools if not present
echo -e "${GREEN}[3/5] Setting up Android Command-Line Tools...${NC}"
if [ ! -d "$SDK_DIR/cmdline-tools/latest" ]; then
    CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    TEMP_ZIP="/tmp/cmdline-tools.zip"
    
    echo -e "Downloading Android Command-Line Tools..."
    wget -q --show-progress -O "$TEMP_ZIP" "$CMDLINE_TOOLS_URL"
    
    echo -e "Extracting tools..."
    unzip -q "$TEMP_ZIP" -d "$SDK_DIR/cmdline-tools"
    mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm -f "$TEMP_ZIP"
else
    echo -e "Android Command-Line Tools already installed in $SDK_DIR/cmdline-tools/latest"
fi

# 4. Accept Android SDK Licenses & Install Core Platforms
echo -e "${GREEN}[4/5] Installing SDK platforms and accepting licenses...${NC}"
export PATH="$SDK_DIR/cmdline-tools/latest/bin:$PATH"

if command -v sdkmanager >/dev/null 2>&1; then
    yes | sdkmanager --licenses >/dev/null 2>&1 || true
    echo -e "Installing Android Platform 36 and Build-Tools..."
    sdkmanager "platforms;android-36" "build-tools;34.0.0" "platform-tools" || true
else
    echo -e "${YELLOW}[!] sdkmanager not found in PATH directly. Licenses will be auto-accepted by Gradle during build.${NC}"
fi

# 5. Persist environment variables in ~/.bashrc
echo -e "${GREEN}[5/5] Updating ~/.bashrc for future sessions...${NC}"
BASHRC="$HOME/.bashrc"
if ! grep -q "ANDROID_HOME" "$BASHRC" 2>/dev/null; then
    cat << 'EOF' >> "$BASHRC"

# Android SDK Environment (Nell Downloader)
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$HOME/android-sdk"
if [ -x "$(command -v javac)" ]; then
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
fi
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
EOF
    echo -e "Environment variables appended to $BASHRC."
fi

# Grant execute permission to gradlew and build-termux.sh if in project root
chmod +x ./gradlew 2>/dev/null || true
chmod +x ./build-termux.sh 2>/dev/null || true

echo ""
echo -e "${GREEN}======================================================${NC}"
echo -e "${GREEN}  ✓ Termux PRoot Environment Setup Complete!         ${NC}"
echo -e "${GREEN}======================================================${NC}"
echo -e "You can now run:"
echo -e "  ${CYAN}bash build-termux.sh${NC} (or ${CYAN}./gradlew assembleDebug${NC})"
echo ""
