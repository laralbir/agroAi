Compila AgroAI en modo debug e instálala en el dispositivo Android conectado por USB.

Sigue estos pasos en orden:

1. Verifica que `adb` está disponible ejecutando `adb version`. Si no se encuentra, informa al usuario que debe instalar Android SDK Platform Tools (`brew install --cask android-commandlinetools` en macOS) y detente.

2. Lista los dispositivos conectados con `adb devices`. Si no hay ningún dispositivo en estado `device` (solo `offline` o lista vacía), informa al usuario que conecte el Pixel 9 por USB con depuración USB activada y detente.

3. Muestra el número de serie y modelo del dispositivo detectado ejecutando `adb shell getprop ro.product.model`.

4. Compila e instala con `./gradlew installDebug`. Muestra la salida en tiempo real. Si falla, muestra el error completo y detente.

5. Si la instalación es correcta, lanza la app directamente con:
   `adb shell am start -n com.laralnet.agroai/.MainActivity`

6. Informa al resultado final: versión instalada (de `app/build.gradle.kts`), dispositivo, y si la app se ha lanzado correctamente.
