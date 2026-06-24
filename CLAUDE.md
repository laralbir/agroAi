# AgroAI — Guía de Desarrollo para Claude

## Visión del Proyecto

**AgroAI** es una app Android nativa para gestión agrícola inteligente que ejecuta modelos Gemma (3/4) completamente en local. No hay backend propio; toda la IA corre en el dispositivo.

- **Package**: `com.laralnet.agroai`
- **Dominio APK**: `laralnet.com.agroai`
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
- **Exportar schema**: `room.schemaLocation` en build.gradle.kts

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
# Build debug
./gradlew assembleDebug

# Tests unitarios
./gradlew test

# Tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedAndroidTest

# Análisis estático
./gradlew lint

# Generar APK release
./gradlew assembleRelease

# Instalar en dispositivo conectado
./gradlew installDebug
```

---

## Versioning

- **Semver**: `MAJOR.MINOR.PATCH`
- **Git tags**: `v1.0.0`, `v1.1.0`, etc.
- **versionCode**: incrementar manualmente en cada release
- **CHANGELOG.md**: mantener actualizado con cada tag

---

## Notas Importantes

1. **El modelo Gemma DEBE descargarse antes de usar cualquier función de IA**. La app muestra un onboarding de descarga si no hay modelo disponible.
2. **Las fotos analizadas NO se envían a ningún servidor** — todo el análisis es local.
3. **AEMET requiere API key** — la app funciona sin ella pero muestra aviso en Settings.
4. **Permisos de calendario**: solicitar en runtime antes de mostrar la integración con Google Calendar.
5. **Modelos grandes (12B, 27B)**: mostrar advertencia de espacio en disco y RAM requerida antes de descargar.
