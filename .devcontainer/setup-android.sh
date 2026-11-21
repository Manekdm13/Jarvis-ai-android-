#!/usr/bin/env bash
set -Eeuo pipefail

SDK_ROOT="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="11076708"
ZIP="/tmp/android-command-line-tools.zip"

echo "==> Installing Android command-line tools"
mkdir -p "$SDK_ROOT/cmdline-tools"
if [ ! -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
  curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -o "$ZIP"
  mkdir -p /tmp/android-cmdline-tools
  unzip -q -o "$ZIP" -d /tmp/android-cmdline-tools
  rm -rf "$SDK_ROOT/cmdline-tools/latest"
  mv /tmp/android-cmdline-tools/cmdline-tools "$SDK_ROOT/cmdline-tools/latest"
  rm -rf /tmp/android-cmdline-tools "$ZIP"
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:$PATH"

echo "==> Accepting Android SDK licenses"
yes | sdkmanager --sdk_root="$SDK_ROOT" --licenses >/dev/null

echo "==> Installing Android API 36 and build tools"
sdkmanager --sdk_root="$SDK_ROOT" \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0"

{
  echo "export ANDROID_HOME=\"$SDK_ROOT\""
  echo "export ANDROID_SDK_ROOT=\"$SDK_ROOT\""
  echo "export PATH=\"$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:\$PATH\""
} >> "$HOME/.bashrc"

echo "==> JARVIS Codespace ready"
java -version
node --version
npm --version
sdkmanager --version
