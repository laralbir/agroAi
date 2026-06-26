# AgroAI — Plan de Implementación

Estado actual: `v0.8.0`

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

## FASE 4 — Análisis de fotos: parseo real y flujo completo

**Por qué cuarta:** La pantalla existe y el streaming funciona, pero el parseo de la respuesta de Gemma es un stub que ignora el JSON.

### Fixes en `PhotoAnalysisViewModel`
- [ ] `parseSuggestions()` — parseo real del JSON que devuelve Gemma:
  ```
  { species, issues, treatments: [{type, description, urgency, suggestedDate}], generalCondition }
  ```
  Usar `kotlinx.serialization` (ya en dependencias)
- [ ] `scheduleSuggestion()` — actualmente no-op; debe navegar a `ScheduleTreatmentSheet` con los datos pre-rellenados
- [ ] Mostrar `species` y `generalCondition` en la UI (actualmente solo se muestra texto raw)
- [ ] Guardar resultado del análisis en `TreatmentRecord.aiAnalysisResult`

### Tests
- [ ] `PhotoAnalysisViewModelTest` — cubre parseo correcto e incorrecto de la respuesta

---

## FASE 5 — Weather (Open-Meteo): caché Room, WorkManager y UI

**Por qué quinta:** `OpenMeteoWeatherRepository.fetchWeather()` ya funciona (v0.7.0), pero `observeCachedWeather()` devuelve `flowOf(null)` — no hay persistencia en Room ni refresco periódico.

### Infrastructure (parcialmente completado en v0.7.0)
- [x] `OpenMeteoApiService` — endpoint `/forecast` con variables current y daily, WMO codes
- [x] `OpenMeteoWeatherRepository.fetchWeather()` — parseo completo de respuesta Open-Meteo
- [x] `OpenMeteoWeatherRepository` como binding en DI (`RepositoryModule`)
- [ ] `observeCachedWeather()` — actualmente `flowOf(null)`; implementar con Room (`WeatherEntity`, `WeatherDao`)
- [ ] `WeatherEntity` + `WeatherDao` + migración de base de datos (version 5)
- [ ] `WeatherRefreshWorker` — WorkManager que llama `refreshWeather()` cada 6h
- [ ] Scheduling del Worker en `AgroAIApplication.onCreate()`

### Application layer
- [ ] `WeatherHandlers.kt` — `RefreshWeatherHandler`
- [ ] `WeatherQueries.kt` — `ObserveWeatherQuery`, `GetWeatherAlertsQuery`

### UI
- [ ] Widget de clima en `PlantationDetailScreen` (temperatura, condición, próximas alertas)
- [ ] Alerta visible en `TreatmentDetailScreen` si hay lluvia/helada en la fecha programada

### Tests
- [ ] Unit test de parseo Open-Meteo (JSON de muestra real)
- [ ] Unit test de `RefreshWeatherHandler`

---

## FASE 6 — Deuda técnica y pulido

Tareas transversales sin bloquear las fases anteriores, pero necesarias antes de una release pública.

### Location (arquitectura)
- [ ] Mover `GpsLocationProvider` e `NominatimApiService` a un módulo con interfaz de puerto
  (actualmente se accede directamente desde `LocationPickerViewModel` sin capa domain/application)
- [ ] `LocationQueries.kt` — `ReverseGeocodeQuery`, `SearchPlacesQuery`

### Iconos de la app
- [ ] Sustituir los placeholders de `ic_launcher` (verde sólido generado en Canary 1) por el icono real de AgroAI

### Onboarding
- [ ] Flujo completo: primer arranque → pedir permisos → descargar modelo → tutorial

### Accesibilidad y strings
- [ ] Revisar `contentDescription` en todos los `Icon()` de las pantallas existentes

### Pruebas instrumentadas pendientes
- [ ] `ModelManagementScreenTest`
- [ ] `TreatmentDetailScreenTest`
- [ ] `CalendarScreenTest`

---

## Resumen de orden

| Fase | Bloqueante para | Esfuerzo estimado | Estado |
|------|----------------|-------------------|--------|
| 1 — AIModel screen + modelos | Usar cualquier función de IA | M | ✅ Completada (v0.7.0) |
| 1.5 — Edición de plantaciones | UX básica de gestión | S | ✅ Completada (v0.7.1) |
| 2 — Treatment handlers + UI | Flujo principal de la app | L | ✅ Completada (v0.8.0) |
| 3 — Calendar handlers + tab | Integración calendario | M | ✅ Completada |
| 4 — PhotoAnalysis fix | Valor real del análisis de fotos | S | ⬜ Pendiente |
| 5 — Weather Open-Meteo | Alertas y recomendaciones | M | 🔄 Parcial (API ✅, caché ⬜) |
| 6 — Deuda técnica | Release pública | M | ⬜ Pendiente |

`S` = 1-2 días · `M` = 3-5 días · `L` = 1-2 semanas
