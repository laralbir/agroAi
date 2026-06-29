# AgroAI — Plan de Implementación

Estado actual: `v0.17.0`

---

## Flujo de usuario objetivo (referencia)

```
Descarga modelo Gemma
  → Abre plantación
    → Toma foto / selecciona de galería
      → Gemma analiza la imagen
        → Sugiere tratamientos
          → Usuario agenda en Google Calendar
            → App avisa si el tiempo lo desaconseja
              → Usuario marca tratamiento como completado
```

---

## ✅ FASE 1 — Pantalla de descarga y gestión de modelos IA _(completada en v0.7.0)_

### UI y funcionalidad (v0.4.0-canary01)
- [x] `ModelManagementScreen` — lista de variantes (1B / 4B / 12B), estado, botones Descargar / Activar / Eliminar
- [x] `ModelManagementViewModel` — suscribe `ObserveModelsQuery`, invoca `DownloadModelHandler`, `SetActiveModelHandler`, `DeleteModelHandler`
- [x] Barra de progreso de descarga — observa `WorkManager` progress via `WorkInfo.State`
- [x] Alerta de espacio en disco y RAM antes de descargar modelos ≥ 12B
- [x] Banner en Home si no hay modelo activo → navega a `ModelManagementScreen`
- [x] Entrada en Settings → `ModelManagementScreen`; ruta `Screen.ModelManagement("models")`

### Ampliaciones (v0.5.0)
- [x] HuggingFace OAuth 2.0 con PKCE (Chrome Custom Tab) — sin copiar tokens manualmente
- [x] Botón **Probar** en tarjetas de modelo: sheet con prompt editable, respuesta completa, tiempos de carga e inferencia
- [x] Auto-activación del primer modelo descargado
- [x] Backend.CPU para MediaPipe (elimina GPU que causaba SIGSEGV por OpenCL inaccesible)
- [x] Migraciones Room 3→4 (purga filas con variantes de enum desconocidas)

### Ampliaciones (v0.6.0)
- [x] Modelo GEMMA4_E2B (`gemma-4-E2B-it.litertlm`, ~2.5 GB)
- [x] SDK LiteRT-LM (`litertlm-android:0.13.1`) para el formato `.litertlm` de Gemma 4
- [x] `GemmaInferenceEngine` dual-engine: `.task` → MediaPipe Tasks GenAI, `.litertlm` → LiteRT-LM con `Backend.CPU`
- [x] Prompt de prueba editable antes de ejecutar; botón unificado Ejecutar/Reintentar

### Tests
- [x] `ModelManagementViewModelTest` — 12 tests: NOT_DOWNLOADED, DOWNLOADED, DOWNLOADING, FAILED, warning dialog, reactive updates, GEMMA4 unavailable

---

## ✅ FASE 1.5 — Edición de plantaciones _(completada en v0.7.1)_

**Por qué aquí:** El wizard ya muestra pantalla de creación; editar una plantación existente es la siguiente necesidad UX.

### Application layer
- [x] `UpdatePlantationCommand` + `UpdatePlantationHandler` — implementados en `PlantationHandlers.kt`

### UI
- [x] `PlantationWizardViewModel` — modo edición: carga plantación existente, `isEditMode`, invoca `UpdatePlantationHandler`
- [x] `PlantationWizardScreen` — título dinámico Crear/Editar, callback `onPlantationSaved`
- [x] `PlantationDetailScreen` — botón Editar → `Screen.PlantationEdit`
- [x] `Screen.PlantationEdit` + ruta en `NavGraph`
- [x] Fix `Plantation.create()/update()` — `plants` reciben `plantationId` correcto al construir
- [x] Mejora de `PlantationDetailScreen` — tarjetas header/location/plants con diseño claro
- [x] Geocodificación automática vía Nominatim (700ms debounce) cuando se introduce municipio/provincia

### Tests
- [x] `UpdatePlantationHandlerTest` — 6 tests (save, evento, plantationId en plants, not found, preserva id, área)
- [x] `PlantationWizardViewModelTest` modo edición — 6 tests (isEditMode, carga datos, carga plantas, llama update, savedId, error)

---

## ✅ FASE 2 — Treatment: handlers, queries y UI de programación _(completada)_

**Por qué segundo:** `PlantationDetailScreen` ya muestra treatments pero no puede crearlos ni completarlos; los handlers son el núcleo funcional de la app.

### Application layer
- [x] `TreatmentHandlers.kt` — `ScheduleTreatmentHandler`, `CompleteTreatmentHandler`, `DeleteTreatmentHandler`
- [x] `TreatmentEvents.kt` — `TreatmentScheduled`, `TreatmentCompleted`, `TreatmentDeleted`
- [x] `TreatmentQueries.kt` — `ObserveTreatmentsByPlantationQuery`, `ObserveUpcomingTreatmentsQuery`, `GetTreatmentQuery`

### UI
- [x] `ScheduleTreatmentScreen` — pantalla con tipo, título, fecha/hora, checkbox "añadir a Calendar" + email
- [x] `TreatmentDetailScreen` — ver detalle (status badge, fecha), marcar como completado con notas, eliminar con confirmación
- [x] `HomeViewModel` + `HomeScreen` — próximos 3 tratamientos pendientes con navegación a detalle
- [x] `PlantationDetailScreen` — FAB "+" para programar, tarjetas de tratamiento tappables con estado/fecha
- [x] Navegación: `Screen.ScheduleTreatment` + `Screen.TreatmentDetail` en NavGraph

### Tests
- [x] `ScheduleTreatmentHandlerTest` — 5 tests (sin calendario, con calendario, fallo calendario no bloquea)
- [x] `CompleteTreatmentHandlerTest` — 4 tests (DONE, record, evento, not found)
- [x] `DeleteTreatmentHandlerTest` — 5 tests (delete, evento, sin calendario, sin event, not found)

---

## ✅ FASE 3 — Calendar: handlers e integración con Treatment _(completada)_

**Por qué tercero:** `ScheduleTreatmentHandler` ya llamaba directamente a `CalendarRepository` (Fase 2). Fase 3 introduce handlers dedicados y la UI de calendario.

### Infraestructura _(ya completada)_
- [x] `GoogleCalendarRepository` — ContentResolver implementation completa
- [x] `CalendarRepository` port (interfaz de dominio)
- [x] Binding en `RepositoryModule`

### Application layer
- [x] `CalendarHandlers.kt` — `CreateCalendarEventHandler`, `UpdateCalendarEventHandler`, `DeleteCalendarEventHandler`
- [x] `CalendarEvents.kt` (domain events) — `CalendarEventCreated`, `CalendarEventUpdated`, `CalendarEventDeleted`
- [x] `CalendarQueries.kt` — `GetCalendarsQuery`
- [x] `ScheduleTreatmentHandler` refactorizado para delegar en `CreateCalendarEventHandler`
- [x] `DeleteTreatmentHandler` refactorizado para delegar en `DeleteCalendarEventHandler`
- [x] `ObserveTreatmentsByMonthQuery` — para CalendarScreen; `observeByDateRange` en repositorio y DAO
- [x] `ScheduleTreatmentCommand` — selector de calendario (email + "Cargar" + dropdown `ExposedDropdownMenuBox`)

### UI
- [x] `CalendarScreen` — cuadrícula mensual con navegación prev/next, dots en días con tratamientos, lista del día seleccionado
- [x] Tab **Calendar** en el bottom nav (5 tabs: Home / Plantations / Análisis / Calendar / Settings)
- [x] `ScheduleTreatmentScreen` — botón "Cargar" carga calendarios del email; dropdown para elegir calendario
- [x] Permiso `READ_CALENDAR` solicitado en runtime al abrir `CalendarScreen`

### Tests
- [x] `CreateCalendarEventHandlerTest` — 6 tests (primary, fallback, evento, sin calendarios, fallo, valor retornado)
- [x] `DeleteCalendarEventHandlerTest` — 3 tests (delete, evento, fallo)
- [x] `UpdateCalendarEventHandlerTest` — 4 tests (update, evento, éxito, fallo)

---

## ✅ FASE 4 — Análisis de fotos: parseo real y flujo completo _(completada en v0.9.0)_

**Por qué cuarta:** La pantalla existe y el streaming funciona, pero el parseo de la respuesta de Gemma es un stub que ignora el JSON.

### Fixes en `PhotoAnalysisViewModel`
- [x] `parseSuggestions()` — parseo real del JSON que devuelve Gemma (kotlinx.serialization, lenient, markdown fences, fallback a raw)
- [x] `scheduleSuggestion()` — navega a `ScheduleTreatmentScreen` con datos pre-rellenados vía URL-encoded query params
- [x] Mostrar `species` y `generalCondition` en la UI
- [x] Guardar resultado del análisis en `Treatment.aiAnalysisResult` (columna DB + migración v4→v5); se pasa como `prefillAnalysis` en la URL de navegación

### Tests
- [x] `PhotoAnalysisViewModelTest` — 13 tests: parser (6) + ViewModel (7)

---

## ✅ FASE 5 — Weather (Open-Meteo): caché Room, WorkManager y UI _(completada en v0.9.0)_

**Por qué quinta:** `OpenMeteoWeatherRepository.fetchWeather()` ya funciona (v0.7.0), pero `observeCachedWeather()` devuelve `flowOf(null)` — no hay persistencia en Room ni refresco periódico.

### Infrastructure (parcialmente completado en v0.7.0)
- [x] `OpenMeteoApiService` — endpoint `/forecast` con variables current y daily, WMO codes
- [x] `OpenMeteoWeatherRepository.fetchWeather()` — parseo completo de respuesta Open-Meteo
- [x] `OpenMeteoWeatherRepository` como binding en DI (`RepositoryModule`)
- [x] `observeCachedWeather()` — implementado con Room (`WeatherEntity`, `WeatherDao`)
- [x] `WeatherEntity` + `WeatherDao` + migración de base de datos (version 6)
- [x] `WeatherRefreshWorker` — WorkManager que llama `refreshWeather()` cada 6h, requiere red
- [x] Scheduling del Worker en `AgroAIApplication.onCreate()`

### Application layer
- [x] `WeatherHandlers.kt` — `RefreshWeatherHandler`
- [x] `WeatherQueries.kt` — `ObserveWeatherQuery`, `GetWeatherAlertsQuery`

### UI
- [x] Widget de clima en `PlantationDetailScreen` (temperatura, condición, humedad, viento)
- [x] Alerta visible en `TreatmentDetailScreen` si hay lluvia intensa/helada/tormenta/granizo/nieve en la fecha programada

### Tests
- [x] `OpenMeteoParserTest` — 9 tests (WMO codes, compass, forecast, failure, locationKey)
- [x] `RefreshWeatherHandlerTest` — 3 tests

---

## ✅ FASE 6 — Deuda técnica y pulido _(completada en v0.11.0)_

### Location (arquitectura)
- [x] `PlaceResult` value object en `location/domain/model/`
- [x] `LocationRepository` puerto (interfaz) en `location/domain/repository/`
- [x] `SearchPlacesQuery`, `ReverseGeocodeQuery`, `GetCurrentLocationQuery` en `location/application/query/`
- [x] `NominatimLocationRepository` adaptador en `location/infrastructure/repository/`
- [x] `LocationModule` binding Hilt en `location/infrastructure/di/`
- [x] `LocationPickerViewModel` refactorizado: ya no importa de infraestructura directamente
- [x] `LocationPickerScreen` actualizado para usar `PlaceResult.municipality`

### Iconos de la app
- [x] `ic_launcher_foreground.xml` rediseñado: hoja rellena + venas de datos + nodo ápice blanco
- [x] `ic_launcher_background.xml` actualizado a `#F0FDF4` (verde muy claro)
- [x] `agroai_launcher.xml` sincronizado

### Onboarding
- [x] `OnboardingViewModel` — lee/escribe `onboarding_done` en DataStore
- [x] `OnboardingScreen` — 3 páginas: Bienvenida → Permisos (Cámara/Ubicación/Calendario) → Listo
- [x] `Screen.Router` + `Screen.Onboarding` añadidos al NavGraph
- [x] Primer arranque navega a Onboarding; vuelve a Home al completar; saltar también disponible
- [x] Strings i18n completos en EN y ES

### Accesibilidad
- [x] `contentDescription = stringResource(R.string.cd_navigate_back)` en todos los ArrowBack de TopAppBar
- [x] `contentDescription = stringResource(R.string.cd_previous_month/cd_next_month)` en CalendarScreen
- [x] Strings de accesibilidad añadidos: `cd_*` en values/ y values-es/

### Pruebas instrumentadas
- [x] `ModelManagementScreenTest` — 6 tests: banner HF, variant cards (NOT_DOWNLOADED/DOWNLOADING/DOWNLOADED/FAILED/múltiples)
- [x] `TreatmentDetailScreenTest` — 10 tests: loading, título, info card, complete button, weather alert, calendar badge, diálogo
- [x] `CalendarScreenTest` — 7 tests: permiso, contenido, mes anterior/siguiente, nombre de mes, lista vacía

---

---

## ✅ FASE 7 — Bugs críticos y mejoras en análisis de fotos _(completada en v0.12.0-v0.13.0)_

**Bloqueante para:** flujo real de IA; sin esto el análisis no aporta valor.

### Bugs
- [x] **Cámara no funciona** — implementado `ActivityResultContracts.TakePicture()` con FileProvider URI temporal
- [x] **Gemma siempre devuelve el mismo análisis** — verificado que el bitmap se carga correctamente; la limitación real es que `tasks-genai:0.10.22` no expone Session API para pasar imagen real (`BitmapImageBuilder`/`addImage` no disponibles). Documentado en `GemmaInferenceEngine` con TODO para upgrade

### Requisito previo para analizar
- [x] Botón de análisis bloqueado si hay plantaciones pero no se ha seleccionado plantación + tipo de planta; mensaje de guía visible

### Prompt enriquecido con contexto
- [x] Tipo de plantación, ubicación (municipio/provincia), variedad de planta incluidos en prompt
- [x] Meteorología actual y previsión 15 días incluida (forecast ampliado de 7 → 15 días en `OpenMeteoApiService`)
- [x] Idioma activo leído desde SharedPreferences e incluido en el prompt
- [x] Respuesta de Gemma renderizada con `SimpleMarkdownText` (negrita, listas, encabezados)

### Campo de pregunta opcional
- [x] `OutlinedTextField` opcional en `PhotoAnalysisScreen` — pregunta inyectada como sección en el prompt

### Gestión de plantas en `PlantationDetailScreen`
- [x] Botón **Analizar** en cada `PlantCard` → navega a `PhotoAnalysisScreen` con `plantationId` + `plantTypeId` pre-seleccionados
- [x] Botón **Editar** → `AlertDialog` con campos de planta; `updatePlantType()` en ViewModel via `UpdatePlantationHandler`
- [x] Botón **Eliminar** → diálogo de confirmación; `deletePlantType()` en ViewModel

### Tests
- [x] Unit test `PhotoAnalysisViewModelTest` — campo `userQuestion` se incluye en el prompt
- [x] Unit test `PlantationDetailViewModelTest` — `deletePlantType` y `updatePlantType`

---

## ✅ FASE 7.5 — Correcciones críticas de análisis de imagen y calendario _(completada en v0.13.0)_

**Bloqueante para:** que el flujo de IA sea realmente usable de principio a fin.

### Análisis de imagen — bugs críticos

- [x] **El análisis no debe lanzarse automáticamente** — separados `setImageUri()` y `analyzePhoto()`; el análisis solo se lanza al pulsar el botón *Analizar*
- [x] **El análisis de imagen ha de funcionar con los dos modelos de imagen** — `supportsVision` expuesto en `PhotoAnalysisUiState`; banner de advertencia si el modelo activo no soporta visión (sólo Gemma 3n E2B/E4B son multimodales)

### Análisis de imagen — mejoras de prompt

- [x] **Validación de imagen en el prompt** — instrucción de validación al principio del `PromptTemplate.photoAnalysisDefault()`; bloque JSON `{actions:[...]}` al final de la respuesta
- [x] **Sugerencias con icono y fecha real** — cada acción tiene `type`, `title`, `description`, `urgency`, `suggestedDate` (año actual); `SuggestionCard` muestra emoji por tipo

### Sugerencias de calendario — bugs corregidos

- [x] **Sugerencias mal parseadas** — `PhotoAnalysisParser` reescrito para buscar `{actions:[...]}` en bloque markdown o texto libre; test suite completo
- [x] **Prompt segmentado por acción** — acciones ordenadas cronológicamente, fecha realista en año en curso, máx. 5 acciones, agendables individualmente

### Google Calendar — integración previa

- [x] **Selector de cuenta en Ajustes** — sección "Google Calendar" en `SettingsScreen` con email input + Guardar + Desvincular
- [x] **Cuenta pre-rellenada en ScheduleTreatment** — `ScheduleTreatmentViewModel` lee la cuenta guardada en `init{}` y activa `addToCalendar=true` automáticamente
- [x] **Cuenta de Google Calendar en onboarding** — `CalendarSetupPage` (página 3/4) en `OnboardingScreen`; guarda en DataStore vía `OnboardingViewModel.setCalendarAccount()`
- [x] **Aviso al cambiar de cuenta** — diálogo de tres opciones: Cancelar / Cambiar y perder / Migrar tratamientos via `MigrateCalendarAccountHandler`
- [x] **`MigrateCalendarAccountHandler`** — crea eventos en nueva cuenta, elimina los de la cuenta anterior (best-effort), actualiza `calendarAccountEmail` + `calendarEventId` en cada tratamiento

### Tests

- [x] `PhotoAnalysisParserTest` — 5 tests del nuevo formato `{actions:[...]}`
- [x] `PhotoAnalysisViewModelTest` — `setImageUri` no lanza análisis, `analyzePhoto()` sin URI no hace nada, `supportsVision`, `userQuestion` en prompt
- [x] `PlantationDetailViewModelTest` — `deletePlantType` y `updatePlantType` con verificación de comando enviado

---

## ✅ FASE 7.6 — Correcciones de UI y tema (v0.15.0)

### Status bar en tema claro
- [x] **Iconos del sistema invisibles en tema claro** — `SideEffect` en `AgroAITheme` que llama a `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme`

### Cabecera de pantalla
- [x] **Cabecera demasiado alta** — `AppTopBar` cambia `height(44.dp)` por `padding(vertical = 8.dp)`, la fila se adapta al contenido real del título

### Listado de plantaciones — revisión manual
- [x] **Botón "Revisar ahora" en `PlantationListScreen`** — icono Refresh en la barra superior que llama a `PlantationHealthWorker.scheduleOneTime(context)` (`OneTimeWorkRequest`, `ExistingWorkPolicy.KEEP`)

### Tests
- [ ] Comprobación visual en ambos temas (claro y oscuro) en dispositivo real; no automatizable con tests unitarios

---

## ✅ FASE 8 — Home mejorado y meteorología avanzada (v0.15.0)

### `HomeScreen` — tareas del día
- [x] Sección "Hoy" con las tareas agendadas para la fecha actual; si vacía, muestra "No hay tareas para hoy"
- [x] Sección "Próximos tratamientos" con los 5 siguientes (excl. hoy) con fecha relativa: hora, "Mañana · HH:mm", "En N días · HH:mm", o fecha absoluta para > 13 días
- [x] Tap en tarea → `TreatmentDetailScreen`

### `HomeScreen` — resumen meteorológico de ubicación actual
- [x] Widget de clima con emoji de condición, temperatura, humedad, viento y precipitación (si > 0)
- [x] `HomeViewModel` obtiene ubicación GPS via `GetCurrentLocationQuery` en `init` y refresca con `RefreshWeatherHandler`
- [x] Expone `homeWeather: StateFlow<WeatherData?>` via `ObserveWeatherQuery`

### `PlantationDetailScreen` — previsión meteorológica 15 días
- [x] `ForecastSection` expandible: fila por día con emoji WMO, día abreviado, max/min °C y precipitación (si > 0%)
- [x] Insertada tras la `WeatherCard` existente, usando `weather.forecast` ya disponible en el VM

### Tests
- [x] `HomeViewModelTest` — 10 tests: tareas del día, fallback a próximas, sin tareas, homeWeather, hasActiveModel (fix: `backgroundScope.launch` para `SharingStarted.WhileSubscribed`)
- [x] `PlantationDetailScreenTest` — 5 tests: título, forecast visible, forecast sin datos, forecast nulo, expandir filas

---

## ✅ FASE 9 — Acciones: gestión manual, IA en background y notificaciones _(completada en v0.16.0)_

**Bloqueante para:** automatización agrícola real; es el núcleo de valor de la app.

### Cuenta Google en ajustes
- [x] En `SettingsScreen`, sección "Calendario" con selector de cuenta Google (`AccountManager`) — ya completado en Fase 7.5
- [x] Cuenta guardada en DataStore; usada como default en `ScheduleActionHandler` y en el worker de background

### Gestión manual de acciones
- [x] Pantalla `ActionListScreen` con catálogo de acciones: Regar, Podar, Cavar, Fertilizar, Fumigar, Cosechar, Injertar, Trasplantar, Abonar, Airear, Aclarar, Otro
- [x] `PlantationAction` (aggregate): id, plantationId, plantTypeId, actionType, title, notes, scheduledAt, status (`PENDING / DONE / SKIPPED`), calendarEventId, source (`MANUAL / AI`)
- [x] `ScheduleActionHandler`, `CompleteActionHandler`, `DeleteActionHandler`, `UpdateActionHandler`
- [x] UI: listar acciones de una plantación, filtrar por estado (All/Pending/Done/Skipped), crear/eliminar acción
- [x] Agendar en Google Calendar al crear (si cuenta configurada)
- [x] Sección "Acciones" en `PlantationDetailScreen` con las 3 primeras pendientes y link "Ver todas"

### Marcar acciones como realizadas
- [x] Botón "Marcar como hecha" en `ActionDetailScreen` → `CompleteActionHandler` → actualiza estado
- [x] Las acciones completadas se excluyen del prompt del worker (sólo pasan `PENDING` AI actions)

### Worker de IA en background (cada 6 horas)
- [x] Botón "Revisar ahora" en `PlantationListScreen` — lanza `PlantationHealthWorker` de forma puntual
- [x] `PlantationHealthWorker` — WorkManager con `PeriodicWorkRequest` de 6h, requiere batería no baja
- [x] Para cada plantación activa: carga plantas y acciones pendientes, obtiene forecast, construye prompt, parsea respuesta → crea acciones `source=AI`, agenda en Calendar, elimina acciones AI obsoletas
- [x] El worker lee la cuenta de calendario desde DataStore

### Notificaciones push
- [x] Canal de notificación `agroai_actions` creado en `AgroAIApplication.onCreate()`
- [x] Notificación con nombre de plantación, título de acción y acción rápida "Ver"
- [x] Permiso `POST_NOTIFICATIONS` solicitado en onboarding (Android 13+)

### Tests
- [x] `ScheduleActionHandlerTest` — 6 tests (manual, con calendar, sin cuenta, fallo calendar, retorno, source AI)
- [x] `CompleteActionHandlerTest` — 4 tests (DONE, evento, not found, plantationId)
- [x] `ActionListViewModelTest` — 9 tests: filter, load, scheduleAction, deleteAction
- [x] `PlantationHealthWorkerTest` — 14 tests JVM: mapToActionType (10 casos) + parseSuggestedDate (4 casos)
- [x] `ActionListScreenTest` — 7 tests instrumentados: empty state, cards, filter chips, navegación a detalle

---

## ✅ FASE 10 — Informes de plantación y log del worker _(completada en v0.17.0)_

**Bloqueante para:** visibilidad histórica del trabajo agrícola.

### Informe por plantación
- [x] Pantalla `PlantationReportScreen` (accesible desde `PlantationDetailScreen` — icono Article en topBar)
- [x] Sección **Historial**: lista de acciones completadas con fecha, notas y fuente (manual/IA/foto)
- [x] Sección **Pendiente**: lista de acciones agendadas con fecha y tipo
- [x] Botón **Exportar** → genera texto en markdown compartible (`shareIntent`)
- [x] Filtros: por tipo de acción (bottom sheet); por rango de fechas

### Fuente de los informes/acciones generadas
- [x] `ActionSource.PHOTO_AI` añadido al enum (además de `MANUAL` y `AI`)
- [x] En `ActionListScreen` y `PlantationReportScreen`, emoji diferenciador: 📷 foto, 🤖 worker, ✋ manual

### Log de ejecuciones del worker
- [x] `WorkerRunEntity` (Room, DB v9): id, timestamp, plantationId, plantationName, actionsCreated, summary, durationMs
- [x] `WorkerRunDao` + migración 8→9 en `DatabaseModule`
- [x] `PlantationHealthWorker` persiste una entrada por plantación por ejecución
- [x] `WorkerLogScreen` accesible desde Settings → "Registro de ejecuciones"
- [x] `WorkerRunDetailScreen`: muestra markdown completo del informe con `SimpleMarkdownText`

### Tests
- [x] `PlantationReportViewModelTest` — 6 tests: historial/pendiente, filtro por tipo, filtro por fecha, clearFilters, exportContent, estado vacío
- [x] `WorkerRunDaoTest` — 7 tests: insert/findById, null, observeAll, orden desc, observeByPlantation, deleteOlderThan, replace conflict
- [x] `WorkerLogViewModelTest` — 5 tests: lista vacía, runs cargados, loadDetail, unknown id, isLoading

---

## ✅ FASE 11 — Editor de prompts funcional _(completada en v0.18.0)_

**Bloqueante para:** personalización real del comportamiento de la IA.

### Pantalla de edición
- [x] `PromptEditorScreen` accesible desde Settings → "Gestionar prompts"
- [x] `SavePromptTemplateHandler` — persiste el `PromptTemplate` editado en Room; marca `isCustomized = true`
- [x] Botón **Restablecer** — diálogo de confirmación → restaura el contenido por defecto de fábrica (`defaultContent`)
- [x] Diálogo de advertencia al guardar prompts con `warningLevel >= MEDIUM`
- [x] Vista previa del prompt con datos de ejemplo sustituidos (plantación de muestra)
- [x] Seed automático de los 3 templates por defecto en BD si está vacía
- [x] Badge "Personalizado" en tarjeta del template cuando `isCustomized = true`

### Tests
- [x] `SavePromptHandlerTest` — guarda, marca customized, no modifica otros campos
- [x] `PromptEditorViewModelTest` — carga inicial (12 tests): load, seed, selectTemplate, onContentChanged, savePrompt (MEDIUM/HIGH/LOW), confirmSave, dismissDialog, resetPrompt, confirmReset, clearSavedOk

---

## ⬜ FASE 12 — Reestructuración de navegación y meteorología

**Bloqueante para:** UX limpia y datos de clima siempre accesibles.

### Reestructuración de navegación
- [ ] **Eliminar tab Plantaciones** del bottom nav — el listado ya aparece en Home; sustituir por otro tab útil o reducir a 4 tabs (Home / Análisis / Calendar / Settings)
- [ ] **Mover botón "Revisar ahora"** (lanzar worker) a `HomeScreen`, con acceso también desde la ficha de cada plantación (`PlantationDetailScreen`) para lanzarlo solo para esa plantación (pasar `plantationId` como input data al `OneTimeWorkRequest`)
- [ ] **Indicador de próxima ejecución del worker** en `HomeScreen`: "Próxima revisión automática en Xh Ym" usando el `WorkInfo` del `PeriodicWorkRequest`

### Meteorología en Home (fix)
- [ ] Revisar por qué `homeWeather` no aparece en `HomeScreen` — comprobar que `GetCurrentLocationQuery` devuelve coordenadas válidas y que `ObserveWeatherQuery` emite datos en cold start
- [ ] **Resumen compacto** visible siempre: emoji condición + temperatura + humedad (una línea)
- [ ] **Tap en el widget** → expande un panel detallado in-place (o abre `WeatherDetailSheet`): temperatura aparente, viento, precipitación, previsión 3 días

### Meteorología por plantación
- [ ] **Icono de clima en el listado de plantaciones** (`PlantationListScreen`): para cada plantación, mostrar emoji WMO + temperatura máx del día junto al nombre (usar datos en caché de Room, sin nuevas llamadas de red)
- [ ] **Resumen meteorológico en `PlantationDetailScreen`**: ya existe `WeatherCard`; añadir tap para expandir un `WeatherDetailSheet` con previsión 15 días, alertas activas y recomendaciones de tratamiento

### Tests
- [ ] `HomeViewModelTest` — workerCountdown emite tiempo restante correcto, weather visible
- [ ] `PlantationListViewModelTest` — expone weatherByPlantation map con datos de caché
- [ ] `PlantationDetailScreenTest` — tap en WeatherCard abre sheet con previsión

---

## Resumen de orden

| Fase | Bloqueante para | Esfuerzo estimado | Estado |
|------|----------------|-------------------|--------|
| 1 — AIModel screen + modelos | Usar cualquier función de IA | M | ✅ Completada (v0.7.0) |
| 1.5 — Edición de plantaciones | UX básica de gestión | S | ✅ Completada (v0.7.1) |
| 2 — Treatment handlers + UI | Flujo principal de la app | L | ✅ Completada (v0.8.0) |
| 3 — Calendar handlers + tab | Integración calendario | M | ✅ Completada |
| 4 — PhotoAnalysis fix | Valor real del análisis de fotos | S | ✅ Completada (v0.9.0) |
| 5 — Weather Open-Meteo | Alertas y recomendaciones | M | ✅ Completada (v0.9.0) |
| 6 — Deuda técnica | Release pública | M | ✅ Completada (v0.11.0) |
| 7 — Bugs críticos + análisis de fotos | IA funcional y útil | M | ✅ Completada (v0.12.0) |
| 7.5 — Correcciones análisis + calendario | Flujo de IA usable de principio a fin | M | ✅ Completada (v0.13.0) |
| 7.6 — Correcciones de UI y tema | Status bar claro + cabecera compacta + revisión manual | S | ✅ Completada (v0.15.0) |
| 8 — Home mejorado + meteorología | Utilidad diaria | S | ✅ Completada (v0.15.0) |
| 9 — Acciones manuales + IA background | Automatización agrícola | L | ✅ Completada (v0.16.0) |
| 10 — Informes + log del worker | Visibilidad histórica | M | ✅ Completada (v0.17.0) |
| 11 — Editor de prompts funcional | Personalización de la IA | S | ✅ Completada (v0.18.0) |
| 12 — Reestructuración nav + meteorología | UX limpia + clima por plantación | M | ⬜ Pendiente |

`S` = 1-2 días · `M` = 3-5 días · `L` = 1-2 semanas
