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

# ── Selector de dispositivo ──────────────────────────────────────────────────
i=1
for serial in $DEVICES; do
    model=$("$ADB" -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    echo "  [$i] $model ($serial)"
    eval "SERIAL_$i=$serial"
    i=$((i + 1))
done
DEVICE_COUNT=$((i - 1))

if [ "$DEVICE_COUNT" -eq 1 ]; then
    # Un solo dispositivo — selección automática
    SELECTED_SERIALS="$DEVICES"
    model=$("$ADB" -s "$SELECTED_SERIALS" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    echo "  → Único dispositivo detectado: $model"
else
    echo "  [*] Todos los dispositivos"
    printf "\nSelecciona un dispositivo [1-%d / *]: " "$DEVICE_COUNT"
    read -r CHOICE
    if [ "$CHOICE" = "*" ] || [ -z "$CHOICE" ]; then
        SELECTED_SERIALS="$DEVICES"
    elif [ "$CHOICE" -ge 1 ] 2>/dev/null && [ "$CHOICE" -le "$DEVICE_COUNT" ]; then
        eval "SELECTED_SERIALS=\$SERIAL_$CHOICE"
    else
        echo "ERROR: Selección inválida."
        exit 1
    fi
fi
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
for serial in $SELECTED_SERIALS; do
    model=$("$ADB" -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    echo "▶ $model — instalando..."
    "$ADB" -s "$serial" install -r "$APK"
    "$ADB" -s "$serial" logcat -c
    "$ADB" -s "$serial" shell am start -n "$APP_PACKAGE/$MAIN_ACTIVITY" >/dev/null
    echo "  ✓ Lanzada v$VERSION"
done

# ── Logcat ────────────────────────────────────────────────────────────────────
echo ""
echo "Mostrando logcat (Ctrl+C para salir)..."
echo ""

LOGCAT_PIDS=""
for serial in $SELECTED_SERIALS; do
    model=$("$ADB" -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    APP_PID=$("$ADB" -s "$serial" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r\n ')
    if [ -n "$APP_PID" ]; then
        "$ADB" -s "$serial" logcat --pid="$APP_PID" | sed "s/^/[$model] /" &
    else
        "$ADB" -s "$serial" logcat | sed "s/^/[$model] /" &
    fi
    LOGCAT_PIDS="$LOGCAT_PIDS $!"
done

# shellcheck disable=SC2086
wait $LOGCAT_PIDS
