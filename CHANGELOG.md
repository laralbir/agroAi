# Changelog

Todos los cambios notables de AgroAI se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es/1.0.0/) y el proyecto usa [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

## [0.11.1] - 2026-06-26

### Corregido
- **Pantalla blanca al arrancar**: el `Router` composable (destino inicial del NavGraph) no tenía UI mientras DataStore cargaba; se añade `CircularProgressIndicator` durante esa espera
- **Spinner infinito en onboarding**: `OnboardingViewModel.onboardingDone` mapeaba la clave ausente en DataStore a `null` en lugar de `false`, haciendo que el router interpretara "primer arranque" como "todavía cargando" indefinidamente; corregido con `?: false`

## [0.11.0] - 2026-06-26

### Añadido
- **Arquitectura Location (Fase 6)**: `PlaceResult` domain value object, `LocationRepository` puerto, `SearchPlacesQuery/ReverseGeocodeQuery/GetCurrentLocationQuery`, `NominatimLocationRepository` adaptador, `LocationModule` Hilt — `LocationPickerViewModel` ya no importa infraestructura directamente
- **Onboarding**: flujo de primer arranque con 3 páginas (Bienvenida, Permisos, Listo); solicita permisos de Cámara, Ubicación y Calendario en runtime; `Screen.Router` como destino inicial del NavGraph para decidir si mostrar onboarding o ir directamente a Home
- **Tests instrumentados**: `ModelManagementScreenTest` (6 tests), `TreatmentDetailScreenTest` (10 tests), `CalendarScreenTest` (7 tests) — todos los screens testeables mediante composables `*Content` extraídos con `@VisibleForTesting`

### Cambiado
- **Icono**: `ic_launcher_foreground.xml` rediseñado con hoja rellena (#10B981), venas de datos en blanco y nodo ápice blanco; fondo cambiado a `#F0FDF4` (verde muy claro)
- **Accesibilidad**: `contentDescription` añadido a todos los `ArrowBack` en TopAppBars (→ `R.string.cd_navigate_back`) y a las flechas de mes anterior/siguiente en `CalendarScreen`
- `ModelManagementScreen`, `TreatmentDetailScreen` y `CalendarScreen` refactorizados para exponer composables `*Content` internos testeables sin Hilt

---

## [0.10.0] - 2026-06-26

### Añadido
- **Renovación automática de token en descarga**: si el servidor HuggingFace devuelve HTTP 401, el Worker intenta renovar el token de acceso con el refresh token antes de fallar. Si la renovación tiene éxito, reintenta la descarga de forma transparente sin que el usuario tenga que hacer nada
- **Botón "Reconectar cuenta HuggingFace"**: si el refresh del token también falla (token revocado, PAT sin refresh token), aparece el botón *Reconectar cuenta HuggingFace* directamente en el panel de error del modelo afectado. Al pulsarlo, abre el flujo OAuth y relanza la descarga automáticamente al completarse la autenticación

### Técnico
- `ModelDownloadWorker` inyecta `HuggingFaceAuthRepository`; en 401 llama a `getValidAccessToken()` (que ya maneja refresh interno) antes de marcar el fallo. Si sigue en 401, escribe el marcador `[RECONNECT]` en el log persistido en `lastError`
- `ModelManagementViewModel.onReconnectHuggingFace(variant)`: guarda la variante como pendiente y delega en el flujo OAuth existente; el observer del `init` block relanza la descarga al recibir la nueva credencial
- `DownloadLogPanel` detecta `[RECONNECT]` en el texto del log y renderiza el botón de reconexión junto al panel de error (mismo patrón que el botón "Aceptar términos" para 403)

---

## [0.9.0] - 2026-06-26

### Añadido
- **Widget meteorológico en detalle de plantación**: tarjeta con temperatura actual, condición (emoji), humedad y viento. Se refresca automáticamente al abrir la pantalla y cada 6 horas en background vía `WeatherRefreshWorker`
- **Alertas de tiempo en detalle de tratamiento**: si el pronóstico para el día programado incluye helada, lluvia intensa, tormenta, granizo o nieve, aparece una tarjeta de alerta (fondo `errorContainer`) junto a la fecha
- **Análisis de foto — parseo real de Gemma**: la respuesta JSON se parsea correctamente con `kotlinx.serialization` (modo lenient, extrae JSON de markdown code fences y texto libre, fallback si no hay JSON)
- **Análisis de foto — especie y estado**: `species` y `generalCondition` devueltos por Gemma se muestran como etiquetas en la UI sobre la lista de sugerencias
- **Análisis de foto — flujo de agendado completo**: el botón *Agendar* en cada sugerencia navega a `ScheduleTreatmentScreen` con tipo, título y descripción pre-rellenados (vía query params URL-encoded). El raw JSON del análisis se guarda en `Treatment.aiAnalysisResult` (nuevo campo en BD)
- `WeatherRefreshWorker` (`@HiltWorker`) — WorkManager periódico cada 6 horas (requiere conexión); itera todas las plantaciones con coordenadas y refresca su clima en Room
- `WeatherEntity` + `WeatherDao` — caché Room con serialización Gson; clave = lat/lon redondeada a 2 decimales con `Locale.ROOT`
- `RefreshWeatherHandler` — handler CQRS para refrescar meteorología de una ubicación
- `ObserveWeatherQuery` — Flow reactivo sobre la caché Room de meteorología
- `GetWeatherAlertsQuery` — deriva `WeatherAlert`s a partir de un `WeatherData` y una fecha; mapea condiciones peligrosas a tipo y severidad
- Campo `aiAnalysisResult: String?` en `Treatment` para almacenar el contexto de la IA que motivó el tratamiento

### Corregido
- `locationKey` usa `Locale.ROOT` en el format de lat/lon para evitar separador decimal de coma en locales españolas

### Técnico
- Migración DB v5→v6: tabla `weather_cache`
- Migración DB v4→v5 (v0.8.0): columna `aiAnalysisResult` en `treatments`
- `PlantationDetailViewModel` inyecta `ObserveWeatherQuery` y `RefreshWeatherHandler`; expone `weather: StateFlow<WeatherData?>`
- `TreatmentDetailViewModel` inyecta `PlantationRepository`, `ObserveWeatherQuery` y `GetWeatherAlertsQuery`; expone `weatherAlerts: List<WeatherAlert>`
- Worker programado en `AgroAIApplication.onCreate()` con `ExistingPeriodicWorkPolicy.UPDATE`

### Tests
- `OpenMeteoParserTest` — 9 tests (WMO codes: CLEAR, STORM, FROST, HEAVY_RAIN; compass N/E; forecast list; API failure; locationKey Locale)
- `RefreshWeatherHandlerTest` — 3 tests (delega a repositorio, fallo, coordenadas correctas)
- `PhotoAnalysisParserTest` — 6 tests (JSON válido, markdown code fence, JSON en prosa, fallback raw, suggestedDate ausente/presente)
- `PhotoAnalysisViewModelTest` — 7 tests (scheduleSuggestion con/sin plantationId, modelLoaded, analyzePhoto, reset, error)

---

## [0.8.0] - 2026-06-26

### Añadido
- **Programación de tratamientos**: pantalla `ScheduleTreatmentScreen` con selector de tipo (riego, poda, cosecha, fertilización, fumigación, injerto, trasplante, otro), título, descripción, fecha/hora y opción de crear evento en Google Calendar con la cuenta indicada
- **Detalle de tratamiento**: `TreatmentDetailScreen` — badge de estado (pendiente/completado/omitido/reprogramado), marcar como completado con notas, eliminar con confirmación
- **Próximos tratamientos en Home**: los 3 tratamientos pendientes más próximos con tipo, fecha y estado; tappables para ir al detalle
- **FAB de programar en detalle de plantación**: el botón "+" abre la pantalla de programación para esa plantación; las tarjetas de tratamiento son tappables
- `ScheduleTreatmentHandler` — programa tratamiento y opcionalmente crea evento en Google Calendar (fallo de calendario no bloquea el guardado)
- `CompleteTreatmentHandler` — marca tratamiento como DONE y guarda registro con notas
- `DeleteTreatmentHandler` — elimina tratamiento y opcionalmente su evento de calendario
- Queries `ObserveTreatmentsByPlantationQuery`, `ObserveUpcomingTreatmentsQuery`, `GetTreatmentQuery`
- Eventos de dominio: `TreatmentScheduled`, `TreatmentCompleted`, `TreatmentDeleted`
- Nuevas strings i18n (en/es): `treatment_add`, `treatment_schedule_title`, `treatment_detail_title`, tipos, estados, botones, confirmaciones

### Tests
- `ScheduleTreatmentHandlerTest` — 5 tests (sin calendario, con calendario, fallo de calendario no bloquea)
- `CompleteTreatmentHandlerTest` — 4 tests (DONE, registro con notas, evento, not found)
- `DeleteTreatmentHandlerTest` — 5 tests (delete, evento, sin calendarEventId, sin calendario, not found)
- `UpdatePlantationHandlerTest` — 6 tests (save, `PlantationUpdated`, plantationId en plants, not found, preserva id, área)
- `PlantationWizardViewModelTest` modo edición — 6 tests (isEditMode, carga datos, carga plantas, llama updateHandler, savedId, error)

---

## [0.7.1] - 2026-06-25

### Añadido
- **Editar plantación**: botón "Editar" (✏️) en la pantalla de detalle abre el wizard precargado con los datos existentes; guardar actualiza la plantación y vuelve al detalle con los datos nuevos
- Soporte de modo edición en `PlantationWizardScreen` y `PlantationWizardViewModel` (reutilización del wizard para crear y editar)

### Corregido
- Las plantas creadas en el wizard ahora se guardan correctamente en Room: `Plantation.create()` y `Plantation.update()` asignan el `plantationId` correcto a cada `PlantType` antes de persistirlo (antes se guardaban con `plantationId=""` y nunca se mostraban en el detalle)

---

## [0.7.0] - 2026-06-25

### Cambiado
- **Meteorología**: sustituida la integración con AEMET por **Open-Meteo** (API pública y gratuita, sin clave API, basada en coordenadas GPS, válida en cualquier país del mundo)
- `WeatherRepository` y `WeatherData` ahora usan latitud/longitud en lugar de código de municipio INE
- Eliminados `AemetApiService`, `AemetWeatherRepository` y toda la configuración de clave API de AEMET en Settings y en el wizard de plantaciones

### Añadido
- Nueva sección **Ayuda y soporte** en Ajustes → enlace a la guía de usuario ([laralbir.github.io/agroAi](https://laralbir.github.io/agroAi/)) que se abre en Chrome Custom Tab

### Documentación
- `docs/index.html` (guía de usuario): sección Meteorología completamente reescrita para reflejar Open-Meteo; eliminadas todas las referencias a AEMET
- `README.md`: actualizado para reflejar Open-Meteo; eliminada la sección de configuración de AEMET

---

## [0.6.0] - 2026-06-25

### Añadido
- **Gemma 4 E2B** (`GEMMA4_E2B`) — nuevo modelo disponible para descarga desde `litert-community/gemma-4-E2B-it-litert-lm` (~2.5 GB, requiere ~4 GB RAM)
- Integración del SDK **LiteRT-LM** (`com.google.ai.edge.litertlm:litertlm-android:0.13.1`) — motor de inferencia para el nuevo formato `.litertlm` de Gemma 4
- `GemmaInferenceEngine` ahora detecta automáticamente el formato del modelo por extensión (`.task` → MediaPipe, `.litertlm` → LiteRT-LM) y selecciona el motor correcto
- Campo `localFileName` en `ModelVariant` para gestionar extensiones de archivo distintas sin romper descargas existentes
- Prompt de prueba adaptado al formato de cada modelo: texto plano para LiteRT-LM (el engine gestiona el template) y `<start_of_turn>` manual para MediaPipe

### Añadido (continuación v0.5.0)
- Prompt de prueba de modelo **editable** antes de ejecutar — `OutlinedTextField` en el sheet; botón unificado "Ejecutar prueba" / "Reintentar"

### Técnico
- Compiler flag `-Xskip-metadata-version-check` añadido para compatibilidad con el SDK LiteRT-LM 0.13.1 (compilado con Kotlin 2.3.x, proyecto en Kotlin 2.1.0)

---

## [0.5.0] - 2026-06-24

### Añadido
- Autenticación HuggingFace mediante **OAuth 2.0 con PKCE** — Chrome Custom Tab, sin copiar tokens manualmente
- `HuggingFaceOAuthCallbackChannel` — puente singleton `Channel<OAuthCallback>` para entrega exacta del callback entre `MainActivity.onNewIntent` y el repositorio
- `DataStoreHuggingFaceAuthRepository` — gestiona access token, refresh token, expiración, username y avatar; compatibilidad retroactiva con tokens PAT guardados en versiones anteriores
- `HuggingFaceConnectionItem` en Settings — muestra avatar y username cuando está conectado; permite desconectar con confirmación
- Botón **Probar** en tarjetas de modelo descargado/activo — lanza pregunta agrícola de muestra a Gemma y muestra sheet con: tamaño en disco, ruta, tiempo de carga, tiempo de inferencia, prompt enviado y respuesta completa
- En el sheet de prueba, estado de error: botón *Copiar error* (con feedback visual de 1,5 s) y botón *Reintentar*
- Activación automática del primer modelo descargado (si no hay ningún modelo activo al completarse la descarga)

### Corregido
- Backend MediaPipe cambiado de `DEFAULT` (legacy CPU, incompatible con firmas modernas) a `Backend.CPU` con fallback a `DEFAULT` para modelos más antiguos
- `GEMMA4_2B` eliminado del enum `ModelVariant` — el archivo `.web.task` es formato WebAssembly, incompatible con el SDK Android de MediaPipe Tasks GenAI
- Crash `SIGSEGV` al inicializar GPU via OpenCL: `libvndksupport.so` inaccesible desde apps con target SDK moderno — GPU eliminado del path de inicialización
- Crash `IllegalArgumentException` al leer filas de BD con variantes de enum eliminadas (`GEMMA3_4B`, `GEMMA4_2B`)

### Técnico
- `BuildConfig.HF_CLIENT_ID` — Client ID OAuth inyectado en tiempo de compilación
- `androidx.browser:browser:1.8.0` — Chrome Custom Tabs para flujo OAuth
- `android:launchMode="singleTop"` en `MainActivity` + `onNewIntent` para recibir el deep link `agroai://oauth-callback-huggingface`
- Migraciones BD: 2→3 (purga variantes desconocidas), 3→4 (elimina `GEMMA4_2B`)
- `AppDatabase.version` → 4

---

## [0.4.0-alpha01] - 2026-06-24

### Añadido
- `ModelManagementScreen` — pantalla de gestión de modelos Gemma con lista de variantes, estado por variante, y botones Descargar / Activar / Eliminar
- `ModelManagementViewModel` — conecta `ObserveModelsQuery` con `DownloadModelHandler`, `SetActiveModelHandler`, `DeleteModelHandler`; observa progreso de descarga en tiempo real via `WorkManager`
- Banner en `HomeScreen` si no hay modelo de IA activo, con enlace directo a `ModelManagementScreen`
- `HomeViewModel.hasActiveModel` — StateFlow reactivo que observa si hay un modelo activo en BD
- Ruta `Screen.ModelManagement("models")` en el NavGraph; accesible desde Settings y banner de Home
- Diálogo de advertencia para modelos ≥ 12 GB antes de iniciar la descarga
- Chip de estado por variante: Activo / Descargado / Descargando (con barra de progreso y %) / Error / No descargado
- Variante GEMMA4_2B marcada como "Próximamente" (sin URL de descarga)
- 12 tests unitarios en `ModelManagementViewModelTest`
- Nuevas strings (en/es): `model_management_title`, `model_download`, `model_activate`, `model_not_downloaded`, `model_coming_soon`, `model_warning_title`, `model_warning_message`, `home_no_model_*`

---

## [0.3.0-alpha01] - 2026-06-24

### Añadido
- Bounded context AIModel completamente implementado: handlers CQRS, queries, eventos de dominio y port `ModelDownloader`
- `DownloadModelHandler` — encola descarga con WorkManager, idempotente (reutiliza id si ya existe la variante)
- `SetActiveModelHandler` — activa modelo descargado, valida estado previo
- `DeleteModelHandler` — cancela descarga en curso si aplica, borra fichero del disco
- `SavePromptTemplateHandler` — edita contenido de prompt, marca como personalizado
- Queries: `ObserveModelsQuery`, `GetActiveModelQuery`, `ObservePromptTemplatesQuery`, `GetPromptTemplateQuery`
- Eventos de dominio: `ModelDownloadStarted`, `ModelActivated`, `ModelDeleted`, `PromptTemplateUpdated`
- Port `ModelDownloader` — desacopla WorkManager de la capa application
- `WorkManagerModelDownloader` — adaptador con `enqueueUniqueWork` (política KEEP)
- 18 tests unitarios para los 4 handlers cubriendo casos de error y edge cases

### Corregido
- `ModelVariant` lleva `gemmaVersion` propio — elimina hardcode de `GemmaVersion.GEMMA_3` en el Worker
- Doble `companion object` en `ModelDownloadWorker` (error de compilación)
- `setResultListener` eliminado de `LlmInferenceOptions` — no existe en MediaPipe 0.10.22
- Shadowing de `it` en `LocationPickerViewModel.onSearchQueryChange` (error de compilación)
- `Icons.Default.Map` requería import explícito de extended icons (error de compilación)
- Package de lanzamiento corregido en `/deploy` (`com.laralnet.agroai.debug`)

### Eliminado
- `account/domain/model/GoogleAccount.kt` — bounded context prematuro sin capa application ni infrastructure

---

## [0.2.0] - 2026-06-24

### Añadido
- Selector de ubicación de plantación con mapa interactivo (osmdroid + OpenStreetMap)
- Búsqueda de lugares con autocompletado mediante Nominatim (OSM, sin API key)
- Obtención de ubicación por GPS del dispositivo (LocationManager nativo, sin Google Play Services)
- Geocodificación inversa automática al tocar el mapa o usar GPS
- Atribución OSM visible en el mapa (requerida por política de uso de tiles)
- Caché de tiles OSM en directorio interno de la app (sin permiso de almacenamiento externo)

### Técnico
- Nuevo bounded context `location` con `NominatimApiService`, `GpsLocationProvider`
- `LocationPickerScreen` y `LocationPickerViewModel` con arquitectura MVVM + StateFlow
- Composable `OsmdroidMapView` con gestión correcta del ciclo de vida (onResume/onPause/onDetach)
- Resultado del mapa devuelto al wizard mediante `NavController.savedStateHandle`
- Retrofit separado para Nominatim con interceptor User-Agent obligatorio
- Debounce 500 ms en búsqueda — cumple la política de 1 req/s de Nominatim
- osmdroid configurado en `AgroAIApplication` con User-Agent y rutas de caché internas

---

## [0.1.0] - 2026-06-24

### Añadido
- Inicialización del proyecto con arquitectura hexagonal + DDD + CQRS + Event-Driven
- Estructura de directorios y bounded contexts: plantation, aimodel, treatment, weather, calendar, account
- CLAUDE.md con guía de desarrollo, arquitectura y convenciones
- README.md con documentación del proyecto y licencia Apache 2.0
- CHANGELOG.md con historial de versiones
- Configuración Gradle con Version Catalog (AGP 8.7, Kotlin 2.1)
- AndroidManifest.xml con todos los permisos necesarios
- Tema Material You con soporte claro/oscuro/sistema
- Internacionalización: inglés y español
- Modelos de dominio iniciales (Plantation, AIModel, Treatment, WeatherData, CalendarEvent)
- Base de datos Room con DAOs y migraciones
- Integración MediaPipe Tasks GenAI para Gemma 3/4 en local
- Wizard de alta de plantación (4 pasos) con 22 tipos de cultivo
- Análisis de fotos con Gemma (streaming)
- Integración con Google Calendar multi-cuenta
- Integración con API AEMET para meteorología
- Gestión de descarga de modelos Gemma con WorkManager
- Prompts editables con indicación de nivel de riesgo
- Inyección de dependencias con Hilt

---

[Unreleased]: https://github.com/laralbir/agroAi/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/laralbir/agroAi/compare/v0.4.0-alpha01...v0.5.0
[0.4.0-alpha01]: https://github.com/laralbir/agroAi/compare/v0.3.0-alpha01...v0.4.0-alpha01
[0.3.0-alpha01]: https://github.com/laralbir/agroAi/compare/v0.2.0...v0.3.0-alpha01
[0.2.0]: https://github.com/laralbir/agroAi/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/laralbir/agroAi/releases/tag/v0.1.0
