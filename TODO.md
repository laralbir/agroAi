# AgroAI — Plan de Implementación

Estado actual: `v0.11.0`

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

## ✅ FASE 7 — Bugs críticos y mejoras en análisis de fotos _(completada en v0.12.0)_

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
- [ ] Unit test `PhotoAnalysisViewModelTest` — campo `userQuestion` se incluye en el prompt
- [ ] Unit test del builder de prompt — verifica que incluye meteorología, variedad y idioma
- [ ] Unit test `DeletePlantTypeHandlerTest`

---

## ⬜ FASE 8 — Home mejorado y meteorología avanzada

**Bloqueante para:** utilidad diaria de la app.

### `HomeScreen` — tareas del día
- [ ] Mostrar las tareas (treatments/acciones) agendadas para hoy en el calendario
- [ ] Si no hay tareas hoy, mostrar las próximas (máx. 5) con fecha relativa ("mañana", "en 3 días")
- [ ] Tap en tarea → `TreatmentDetailScreen` o detalle de acción

### `HomeScreen` — resumen meteorológico de ubicación actual
- [ ] Widget de clima en Home basado en la ubicación GPS actual (o última conocida)
- [ ] Mostrar: temperatura actual, condición, humedad, viento, precipitación próximas 24h
- [ ] Actualizar al abrir la pantalla; usar caché si tiene < 1h

### `PlantationDetailScreen` — previsión meteorológica 15 días
- [ ] Sección expandible "Previsión 15 días" con fila por día: icono WMO + temp max/min + probabilidad precipitación
- [ ] Datos de `WeatherData.dailyForecast` ya disponibles en Open-Meteo (`temperature_2m_max/min`, `precipitation_probability_max`, `weather_code` daily)

### Tests
- [ ] `HomeViewModelTest` — tareas del día, fallback a próximas, sin tareas
- [ ] `PlantationDetailScreenTest` — sección previsión 15 días visible

---

## ⬜ FASE 9 — Acciones: gestión manual, IA en background y notificaciones

**Bloqueante para:** automatización agrícola real; es el núcleo de valor de la app.

### Cuenta Google en ajustes
- [ ] En `SettingsScreen`, añadir sección "Calendario" con selector de cuenta Google (`AccountManager`)
- [ ] Guardar la cuenta elegida en DataStore; usarla como default en `ScheduleTreatmentHandler` y en el worker de background

### Gestión manual de acciones
- [ ] Pantalla `ActionListScreen` con catálogo de acciones predefinidas: **Regar, Podar, Cavar, Fertilizar, Fumigar, Cosechar, Injertar, Trasplantar, Abonar, Airear, Aclarar, Otro**
- [ ] Crear `PlantationAction` (aggregate): id, plantationId, plantTypeId, actionType, title, notes, scheduledAt, status (`PENDING / DONE / SKIPPED`), calendarEventId, source (`MANUAL / AI`)
- [ ] `ScheduleActionHandler`, `CompleteActionHandler`, `DeleteActionHandler`, `UpdateActionHandler`
- [ ] UI: listar acciones de una plantación, filtrar por estado, crear/editar/eliminar acción
- [ ] Agendar en Google Calendar al crear (si cuenta configurada)

### Marcar acciones como realizadas
- [ ] Botón "Marcar como hecha" en el detalle de acción → `CompleteActionHandler` → actualiza estado + calendarEvent
- [ ] Las acciones `DONE` o `SKIPPED` se excluyen de los prompts de análisis y del worker de background

### Worker de IA en background (cada 6 horas)
- [ ] `PlantationReviewWorker` — WorkManager con `PeriodicWorkRequest` de 6h, requiere red
- [ ] Para cada plantación activa:
  1. Carga todas las plantas (`PlantType`) y acciones pendientes
  2. Obtiene previsión meteorológica 15 días (Open-Meteo)
  3. Construye prompt contextualizado con toda la info y lo envía a Gemma
  4. Parsea respuesta → lista de acciones sugeridas
  5. Crea acciones nuevas (`source = AI`) y las agenda en Calendar
  6. Elimina/cancela acciones `AI` pendientes que ya no sean relevantes (meteorología cambió o ya completadas)
- [ ] El worker respeta el idioma configurado en DataStore

### Notificaciones push
- [ ] Cuando el worker agenda una acción nueva, lanzar notificación local con `NotificationManager`
- [ ] Notificación incluye: nombre de plantación, tipo de acción, fecha propuesta, acción rápida "Ver"
- [ ] Canal de notificación `agroai_actions` creado en `AgroAIApplication.onCreate()`
- [ ] Permiso `POST_NOTIFICATIONS` solicitado en onboarding (Android 13+)

### Tests
- [ ] `ScheduleActionHandlerTest` — 5 tests (manual, con calendar, sin cuenta, duplicado, evento)
- [ ] `CompleteActionHandlerTest` — 4 tests
- [ ] `PlantationReviewWorkerTest` con `TestListenableWorkerBuilder` — mock de `GemmaInferenceEngine` y `WeatherRepository`
- [ ] `ActionListScreenTest` — listar, completar, eliminar

---

## ⬜ FASE 10 — Informes de plantación

**Bloqueante para:** visibilidad histórica del trabajo agrícola.

### Informe por plantación
- [ ] Pantalla `PlantationReportScreen` (accesible desde `PlantationDetailScreen`)
- [ ] Sección **Historial**: lista de acciones/tratamientos completados con fecha, notas y fuente (manual/IA)
- [ ] Sección **Pendiente**: lista de acciones agendadas en Calendar con fecha y tipo
- [ ] Botón **Exportar** → genera texto en markdown o PDF compartible (`shareIntent`)
- [ ] Filtros: por rango de fechas, por tipo de acción, por planta

### Tests
- [ ] `PlantationReportViewModelTest` — historial completo, filtro por fecha, export content

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
| 8 — Home mejorado + meteorología | Utilidad diaria | S | ⬜ Pendiente |
| 9 — Acciones manuales + IA background | Automatización agrícola | L | ⬜ Pendiente |
| 10 — Informes de plantación | Visibilidad histórica | S | ⬜ Pendiente |

`S` = 1-2 días · `M` = 3-5 días · `L` = 1-2 semanas
