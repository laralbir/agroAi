#!/usr/bin/env sh
set -e

# ── Configuración ────────────────────────────────────────────────────────────
APP_PACKAGE="com.laralnet.agroai.debug"
MAIN_ACTIVITY="com.laralnet.agroai.MainActivity"
APK="app/build/outputs/apk/debug/app-arm64-v8a-debug.apk"

# ── Localizar adb ────────────────────────────────────────────────────────────
if command -v adb >/dev/null 2>&1; then
    ADB="adb"
elif [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
else
    echo "ERROR: adb no encontrado."
    echo "Instala Android SDK Platform Tools:"
    echo "  brew install --cask android-commandlinetools"
    exit 1
fi

# ── Dispositivos conectados (excluye emuladores) ─────────────────────────────
# Los emuladores causan un bug con AGP 8.13+ al comprobar Privacy Sandbox.
DEVICES=$("$ADB" devices | awk 'NR>1 && $2=="device" && $1!~/^emulator/ {print $1}')

if [ -z "$DEVICES" ]; then
    echo "ERROR: No hay dispositivos físicos conectados en estado 'device'."
    echo "Conecta el Pixel 9 por USB con depuración USB activada."
    exit 1
fi

echo "Dispositivos detectados:"
for serial in $DEVICES; do
    model=$("$ADB" -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    echo "  • $model ($serial)"
done
echo ""

# ── Build ────────────────────────────────────────────────────────────────────
echo "Compilando..."
./gradlew assembleDebug

if [ ! -f "$APK" ]; then
    echo "ERROR: No se encontró el APK en $APK"
    exit 1
fi

# Versión instalada
VERSION=$(grep 'versionName' app/build.gradle.kts | grep -o '"[^"]*"' | tr -d '"')

# ── Instalar y lanzar ────────────────────────────────────────────────────────
echo ""
for serial in $DEVICES; do
    model=$("$ADB" -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    echo "▶ $model — instalando..."
    "$ADB" -s "$serial" install -r "$APK"
    "$ADB" -s "$serial" shell am start -n "$APP_PACKAGE/$MAIN_ACTIVITY" >/dev/null
    echo "  ✓ Lanzada v$VERSION"
done
