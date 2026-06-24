# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# AgroAI — Guía de Desarrollo para Claude

## Visión del Proyecto

**AgroAI** es una app Android nativa para gestión agrícola inteligente que ejecuta modelos Gemma (3/4) completamente en local. No hay backend propio; toda la IA corre en el dispositivo.

- **Package**: `com.laralnet.agroai` (debug: `com.laralnet.agroai.debug`)
- **Dispositivos de prueba**: Google Pixel 9 Pro XL (Android 15), Google Pixel 10 Pro XL (Android 16)
- **minSdk**: 26 (Android 8.0) — cubre ~95% de dispositivos Android
- **targetSdk**: 36 (Android 16)

---

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin 2.x |
| UI | Jetpack Compose + Material Design 3 |
| IA en local | MediaPipe Tasks GenAI (`tasks-genai`) |
| Base de datos | Room (SQLite) |
| DI | Hilt |
| Async | Coroutines + Flow |
| Prefs | DataStore (Proto) |
| Red | Retrofit + OkHttp |
| Imágenes | CameraX + Coil |
| Trabajo background | WorkManager |
| Calendario | Google Calendar ContentProvider (Android) |
| Mapas | osmdroid + OpenStreetMap (tiles online, sin descarga) |
| Geocodificación | Nominatim (OSM, sin API key) |
| GPS | Android LocationManager (sin Google Play Services) |
| Navegación | Navigation Compose |

---

## Arquitectura

### Hexagonal (Ports & Adapters) + DDD + CQRS + Event-Driven

```
app/src/main/kotlin/com/laralnet/agroai/
├── core/                          # Infraestructura transversal
│   ├── domain/
│   │   ├── event/                 # DomainEvent base
│   │   └── repository/            # Repository<T, ID> base
│   └── infrastructure/
│       ├── di/                    # Módulos Hilt globales
│       └── event/                 # EventBus (SharedFlow)
│
├── plantation/                    # Bounded Context: Plantaciones
│   ├── domain/
│   │   ├── model/                 # Aggregates, Entities, Value Objects
│   │   ├── event/                 # Domain Events
│   │   └── repository/            # Puerto (interfaz)
│   ├── application/
│   │   ├── command/               # Comandos CQRS
│   │   ├── query/                 # Queries CQRS
│   │   └── handler/               # Handlers (use cases)
│   └── infrastructure/
│       ├── persistence/           # Room: entities, DAOs, mappers
│       └── repository/            # Adaptador Room → Repositorio
│
├── aimodel/                       # Bounded Context: Modelos IA
├── treatment/                     # Bounded Context: Tratamientos
├── weather/                       # Bounded Context: Meteorología
├── calendar/                      # Bounded Context: Calendario
├── account/                       # Bounded Context: Cuentas Google
│
├── database/                      # AppDatabase (Room)
│   └── AppDatabase.kt
│
└── ui/
    ├── theme/                     # Color, Theme, Type
    ├── navigation/                # NavGraph
    └── screens/                   # Pantallas Compose
```

### Reglas de arquitectura OBLIGATORIAS

1. **El dominio NO depende de nada externo** — sin imports de Android, Room, Retrofit en `domain/`
2. **La aplicación depende sólo del dominio** — handlers invocan repositorios por interfaz
3. **La infraestructura implementa los puertos** — Room, API, Calendar son adaptadores
4. **CQRS estricto**: commands mutan estado y emiten eventos; queries sólo leen
5. **Event-Driven**: los domain events se publican en `EventBus` (SharedFlow) y los handlers los suscriben en `Application` layer
6. **Hilt para DI** — nunca instanciar dependencias manualmente en producción

---

## Bounded Contexts y Modelos de Dominio

### Plantation (Plantación)
- `Plantation` (aggregate root): id, name, type, location, area, plants, notes, createdAt
- `PlantType` (entity): id, name, variety, count, rowSpacing, plantSpacing
- `PlantationType` (enum): HUERTA, SECANO, REGADIO, VIÑEDO, OLIVAR, FRUTAL, CITRICOS, INVERNADERO, AROMATICAS, MEDICINALES, CEREAL, LEGUMINOSAS, TUBERCULOS, FLORICULTURA, VIVERO, BOSQUE, PRADERA, MONTAÑA, AGUACATE, ARROZ, OTRO
- `Location` (value object): latitude, longitude, address, municipality, province, country

### AIModel (Modelo IA)
- `AIModel` (aggregate root): id, name, gemmaVersion, variantSize, filePath, downloadState, checksum
- `GemmaVersion` (enum): GEMMA_3, GEMMA_4
- `ModelVariant` (enum): 1B, 4B, 12B, 27B
- `PromptTemplate` (entity): id, name, content, context, isEditable, warningLevel

### Treatment (Tratamiento)
- `Treatment` (aggregate root): id, plantationId, type, description, scheduledAt, status
- `TreatmentRecord` (entity): id, treatmentId, executedAt, notes, photoUri, calendarEventId
- `TreatmentType` (enum): RIEGO, PODA, COSECHA, FERTILIZACION, FUMIGACION, INJERTO, TRANSPLANTE, OTRO

### Weather (Meteorología)
- `WeatherData` (value object): temperature, humidity, windSpeed, precipitation, forecast
- `WeatherAlert` (entity): type, severity, startAt, endAt, affectedTreatments

### CalendarEvent (Calendario)
- `CalendarEvent` (value object): id, calendarId, title, description, startAt, endAt, accountEmail

### Account (Cuenta Google)
- `GoogleAccount` (entity): email, displayName, photoUri, calendarId

---

## Tipos de Plantación (Referencia Completa)

```kotlin
enum class PlantationType(val labelEn: String, val labelEs: String) {
    HUERTA("Vegetable Garden", "Huerta"),
    SECANO("Dry Farming", "Secano"),
    REGADIO("Irrigated", "Regadío"),
    VIÑEDO("Vineyard", "Viñedo"),
    OLIVAR("Olive Grove", "Olivar"),
    FRUTAL("Fruit Orchard", "Frutal"),
    CITRICOS("Citrus Orchard", "Cítricos"),
    INVERNADERO("Greenhouse", "Invernadero"),
    AROMATICAS("Herb Garden", "Aromáticas"),
    MEDICINALES("Medicinal Plants", "Medicinales"),
    CEREAL("Grain Crops", "Cereal"),
    LEGUMINOSAS("Legumes", "Leguminosas"),
    TUBERCULOS("Root Vegetables", "Tubérculos"),
    FLORICULTURA("Floriculture", "Floricultura"),
    VIVERO("Plant Nursery", "Vivero"),
    BOSQUE("Commercial Forestry", "Bosque/Silvicultura"),
    PRADERA("Pasture/Meadow", "Pradera/Prado"),
    MONTANA("Mountain/Terraced", "Montaña/Terraza"),
    AGUACATE("Avocado", "Aguacate"),
    ARROZ("Rice Paddy", "Arrozal"),
    PLATANO("Banana Plantation", "Platanera"),
    OTRO("Other", "Otro")
}
```

---

## Navegación actual (NavGraph)

Bottom nav con 4 tabs: **Home → Plantations → Analysis → Settings**. El tab Calendar está definido en el dominio pero aún no está en el bottom nav. Pantallas adicionales sin tab propio: `PlantationDetail`, `PlantationWizard`, `LocationPicker`.

El flujo de datos entre `LocationPickerScreen` y `PlantationWizardScreen` usa `savedStateHandle` (5 claves: `picked_lat`, `picked_lon`, `picked_address`, `picked_municipality`, `picked_province`).

---

## Integración de Mapas

### Stack: osmdroid + Nominatim

| Componente | Librería / Servicio | Licencia | API key |
|-----------|-------------------|---------|---------|
| Tiles de mapa | osmdroid + OpenStreetMap MAPNIK | Apache 2.0 / ODbL | No |
| Búsqueda de lugares | Nominatim (nominatim.openstreetmap.org) | ODbL | No |
| Geocodificación inversa | Nominatim `/reverse` | ODbL | No |
| GPS del dispositivo | Android `LocationManager` | Apache 2.0 | No |

### Notas de implementación
- **Tiles**: se cachean automáticamente en `cacheDir/osmdroid/tiles` (almacenamiento interno, sin permiso extra)
- **Nominatim**: se requiere `User-Agent: AgroAI/X.Y.Z (laralbir@gmail.com)` en cada request (política de uso)
- **Rate limit Nominatim**: máx. 1 req/s — el debounce de 500 ms en la búsqueda lo cumple sobradamente
- **GPS**: `LocationManager.requestSingleUpdate` con timeout 15 s; usa última posición conocida si tiene < 2 min
- **Atribución OSM obligatoria**: mostrar `"© OpenStreetMap contributors"` visible en el mapa en todo momento
- **`OsmdroidMapView`** composable: usa `AndroidView` + `DisposableEffect` para lifecycle (onResume/onPause/onDetach)
- **Flujo de ubicación en el wizard**: Step 2 → "Pick on map" → `LocationPickerScreen` → `savedStateHandle` → `PlantationWizardViewModel.setLocationFromMap()`

### Configuración en AgroAIApplication
```kotlin
OsmConfiguration.getInstance().apply {
    userAgentValue = "AgroAI/X.Y.Z (laralbir@gmail.com)"
    osmdroidBasePath = File(cacheDir, "osmdroid")
    osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
}
```

---

## Integración Gemma (MediaPipe Tasks GenAI)

- **SDK**: `com.google.mediapipe:tasks-genai`
- **Modelos disponibles**:
  - Gemma 3 1B (recomendado para gama media, ~2GB)
  - Gemma 3 4B (gama alta, ~5GB)
  - Gemma 3 12B (gama premium, ~14GB)
  - Gemma 4 (cuando esté disponible)
- **Descarga**: WorkManager con notificación de progreso
- **Almacenamiento**: `getExternalFilesDir("models")` — NO en caché
- **Inferencia**: `LlmInference` de MediaPipe, en `Dispatchers.Default`
- **Análisis de foto**: Imagen → Bitmap → BitmapBuffer → prompt multimodal

### Flujo de análisis de foto
1. Usuario toma foto con CameraX o selecciona de galería
2. `PhotoAnalysisCommand` → `PhotoAnalysisHandler`
3. Handler carga imagen como `Bitmap`, construye prompt multimodal
4. `GemmaInferenceEngine.analyzeWithImage(bitmap, prompt)` → streaming response
5. Respuesta parseada → `TreatmentSuggestion` (lista de sugerencias)
6. UI muestra sugerencias → usuario elige cuáles agendar
7. `ScheduleTreatmentCommand` → Google Calendar + `TreatmentRecord`

---

## Integración AEMET

- **API Base URL**: `https://opendata.aemet.es/openapi/api/`
- **Auth**: Header `api_key` (configurada por el usuario en Settings)
- **Endpoints usados**:
  - `/prediccion/especifica/municipio/diaria/{codMunicipio}` — predicción diaria
  - `/observacion/convencional/datos/estacion/{idema}` — datos observación
  - `/prediccion/especifica/municipio/horaria/{codMunicipio}` — predicción horaria
- **Frecuencia**: Refresh automático cada 6h via WorkManager
- **Uso**: Al sugerir tratamientos, consultar forecast. Al revisar calendario, alertar si lluvia/helada afecta lo agendado.

---

## Integración Google Calendar

- **Acceso**: `ContentResolver` con permiso `READ_CALENDAR` / `WRITE_CALENDAR`
- **Multi-cuenta**: Enumerar cuentas con `AccountManager`, filtrar tipo `com.google`
- **Operaciones**:
  - Listar calendarios por cuenta
  - Crear evento con alarma
  - Actualizar evento existente
  - Verificar conflictos meteorológicos

---

## Internacionalización

- **Idiomas soportados**: `en` (English), `es` (Spanish — todas las regiones)
- **Default**: Si el idioma del sistema no es `en` ni `es`, usar `en`
- **Configuración**: Manual en Settings o automático (sistema)
- **Medidas**: Sistema Internacional (m², km, litros, kg, °C)
- **Recursos**: `res/values/strings.xml` (en), `res/values-es/strings.xml` (es)

---

## Temas

- **Opciones**: Light, Dark, System (follow system)
- **Implementación**: `DynamicColorScheme` de Material You cuando sea posible (API 31+), fallback a paleta fija
- **Configuración**: DataStore proto preference `theme_mode`

---

## Base de Datos (Room)

```
AppDatabase (SQLite vía Room)
├── plantations
├── plant_types
├── treatment_records
├── ai_models
├── prompt_templates
├── api_configs
└── weather_cache
```

- **Migraciones**: `Migration` con fallback `fallbackToDestructiveMigration(false)` en debug
- **Exportar schema**: schemas generados en `app/schemas/` (KSP arg `room.schemaLocation`)
- **Procesador de anotaciones**: KSP (no kapt) — Hilt y Room usan `ksp()` en `app/build.gradle.kts`

---

## Prompts para Gemma (Editables)

- Cada `PromptTemplate` tiene un `warningLevel`:
  - `LOW`: cambios cosméticos al prompt (sin riesgo)
  - `MEDIUM`: modificaciones de instrucciones (riesgo moderado)
  - `HIGH`: cambios estructurales que pueden afectar la salida (riesgo alto)
- Se muestra diálogo de confirmación al editar con `warningLevel >= MEDIUM`
- Los prompts editados se marcan como `isCustomized = true`

---

## Convenciones de Código

- **Naming**: `PascalCase` clases, `camelCase` funciones/vars, `SCREAMING_SNAKE` constantes
- **Coroutines**: `viewModelScope` en VM, `applicationScope` para tareas de larga duración
- **Error handling**: `Result<T>` en use cases, never `throw` desde domain hacia UI
- **Testing**: Unit tests para domain + handlers; integration tests con Room in-memory
- **No comentarios obvios**: sólo documentar WHY cuando no es evidente del código

---

## Comandos Frecuentes

```bash
# Build debug (genera APKs por ABI + universal en app/build/outputs/apk/debug/)
./gradlew assembleDebug

# Tests unitarios (JVM, sin dispositivo)
./gradlew test

# Unit tests de módulo concreto
./gradlew :app:testDebugUnitTest

# Tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedAndroidTest

# Tests instrumentados de una clase concreta
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.laralnet.agroai.database.PlantationDaoTest

# Análisis estático
./gradlew lint

# Generar APK release (minificado + shrinkResources)
./gradlew assembleRelease

# Instalar en dispositivo conectado (instala como com.laralnet.agroai.debug)
./gradlew installDebug
```

---

## Testing

### Regla fundamental — tests obligatorios por funcionalidad

**Toda funcionalidad nueva DEBE incluir sus tests en el mismo commit o PR.** No se aprueba código sin tests.

### Qué test escribir según el tipo de código

| Tipo de código nuevo | Test requerido | Ubicación |
|---------------------|---------------|-----------|
| Modelo de dominio (aggregate, value object, event) | Unit test del modelo | `src/test/.../domain/` |
| Handler de comando/query (application layer) | Unit test con mocks de repositorio y EventBus | `src/test/.../application/` |
| Mapper entidad ↔ dominio | Unit test de round-trip | `src/test/.../infrastructure/persistence/mapper/` |
| ViewModel | Unit test con `MainDispatcherRule` + MockK | `src/test/.../ui/` |
| Room DAO / entidad nueva | Instrumented test con BD en memoria | `src/androidTest/.../database/` |
| Pantalla Compose nueva | Instrumented Compose test con `createComposeRule()` | `src/androidTest/.../ui/` |

### Stack de testing

```
# Unit tests (JVM — sin emulador)
JUnit 4                  → runner + assertions
MockK                    → mocking de interfaces y clases
Turbine                  → test de Flow (app.cash.turbine)
kotlinx-coroutines-test  → runTest, advanceTimeBy, MainDispatcherRule

# Instrumented tests (requieren emulador o dispositivo real)
AndroidJUnit4            → runner
Room.inMemoryDatabaseBuilder → tests de DAO sin BD real
ComposeTestRule          → tests de UI con createComposeRule()
MockK-Android            → mocking en contexto Android
```

### Convenciones de nombrado

- **Archivos**: `<ClaseQueTestea>Test.kt` — mismo paquete que la clase testeada
- **Métodos**: backtick notation descriptiva: `` `método() hace X cuando Y`() ``
- **Tests de ViewModel**: usar `MainDispatcherRule` siempre
- **Tests de Flow**: usar Turbine (`.test { }`) o `collect` con `runTest`
- **Datos de prueba**: crear funciones privadas `fun entity(...)` con parámetros con defaults razonables

### Checklist de tests antes de cada PR / push

1. [ ] Unit tests de dominio para cada nuevo aggregate o value object
2. [ ] Unit tests del handler para cada nuevo Command o Query
3. [ ] Unit test del ViewModel para cada pantalla nueva
4. [ ] Instrumented DAO test para cada nueva entidad Room
5. [ ] Instrumented Compose test para cada nueva pantalla
6. [ ] `./gradlew test` pasa sin errores
7. [ ] `./gradlew connectedAndroidTest` pasa sin errores (cuando hay emulador disponible)

### TestContainers y casos especiales

- **GemmaInferenceEngine**: no testear con el modelo real; usar una interfaz `AIEngine` y mockearla en tests
- **GPS / LocationManager**: inyectar `GpsLocationProvider` como interfaz y mockearlo en unit tests
- **AEMET / Nominatim**: mockear `NominatimApiService` y `AemetApiService` (son interfaces Retrofit)
- **WorkManager**: usar `TestListenableWorkerBuilder` para tests de Workers

---

## Versioning

- **Semver**: `MAJOR.MINOR.PATCH`
- **Git tags**: `v1.0.0`, `v1.1.0`, etc.
- **versionCode**: incrementar manualmente en cada release en `app/build.gradle.kts`
- **CHANGELOG.md**: mantener actualizado con cada tag

### Checklist OBLIGATORIO antes de cada push con cambio de versión

Antes de ejecutar `git push`, verificar y actualizar en este orden:

1. **`CHANGELOG.md`** — añadir sección con la nueva versión, fecha y lista de cambios:
   ```markdown
   ## [X.Y.Z] - YYYY-MM-DD
   ### Añadido / Cambiado / Corregido
   - ...
   ```

2. **`README.md`** — revisar que la documentación refleja las nuevas funcionalidades, requisitos o cambios de configuración introducidos en esta versión.

3. **Tag de git** — crear el tag anotado **siempre** que cambie `versionName` en `app/build.gradle.kts`:
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z — descripción breve"
   git push origin main
   git push origin vX.Y.Z
   ```

> **Regla**: si `versionName` cambia → tag obligatorio. Sin tag no hay release.

---

## Notas Importantes

1. **El modelo Gemma DEBE descargarse antes de usar cualquier función de IA**. La app muestra un onboarding de descarga si no hay modelo disponible.
2. **Las fotos analizadas NO se envían a ningún servidor** — todo el análisis es local.
3. **AEMET requiere API key** — la app funciona sin ella pero muestra aviso en Settings.
4. **Permisos de calendario**: solicitar en runtime antes de mostrar la integración con Google Calendar.
5. **Modelos grandes (12B, 27B)**: mostrar advertencia de espacio en disco y RAM requerida antes de descargar.
