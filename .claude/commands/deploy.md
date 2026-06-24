Compila AgroAI en modo debug e instálala en el dispositivo Android conectado por USB.

Sigue estos pasos en orden:

1. Localiza `adb` comprobando primero si está en el PATH (`adb version`), y si no, intenta con la ruta `$HOME/Library/Android/sdk/platform-tools/adb`. Usa la que funcione para todos los pasos siguientes. Si ninguna funciona, informa al usuario que debe instalar Android SDK Platform Tools (`brew install --cask android-commandlinetools` en macOS) y detente.

2. Lista los dispositivos conectados con `adb devices`. Si no hay ningún dispositivo en estado `device` (solo `offline` o lista vacía), informa al usuario que conecte el Pixel 9 por USB con depuración USB activada y detente.

3. Muestra el modelo del dispositivo detectado ejecutando `adb shell getprop ro.product.model`.

4. Compila e instala con `./gradlew installDebug`. Si falla, muestra el error completo y detente.

5. Si la instalación es correcta, lanza la app directamente con:
   `adb shell am start -n com.laralnet.agroai.debug/.MainActivity`

6. Informa el resultado final: versión instalada (de `app/build.gradle.kts`), dispositivo, y si la app se ha lanzado correctamente.
