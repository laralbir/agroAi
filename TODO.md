# AgroAI — Plan de Implementación

Estado actual: `v0.3.0-canary01`

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

## FASE 1 — Pantalla de descarga y gestión de modelos IA

**Por qué primero:** Sin modelo descargado el análisis de fotos no funciona. Los handlers ya existen (v0.3.0-canary01); falta la UI que los invoca.

### UI
- [ ] `ModelManagementScreen` — lista de variantes (1B / 4B / 12B), estado, botones Descargar / Activar / Eliminar
- [ ] `ModelManagementViewModel` — suscribe `ObserveModelsQuery`, invoca `DownloadModelHandler`, `SetActiveModelHandler`, `DeleteModelHandler`
- [ ] Barra de progreso de descarga — observa `WorkManager` progress via `WorkInfo.State`
- [ ] Alerta de espacio en disco y RAM antes de descargar modelos ≥ 12B
- [ ] Onboarding: si no hay modelo activo al abrir la app, redirigir a esta pantalla
- [ ] Añadir entrada en Settings ("Modelos IA") y/o tab propio en el bottom nav

### Tests
- [ ] `ModelManagementViewModelTest` — cubre estados DOWNLOADING / DOWNLOADED / FAILED

---

## FASE 2 — Treatment: handlers, queries y UI de programación

**Por qué segundo:** `PlantationDetailScreen` ya muestra treatments pero no puede crearlos ni completarlos; los handlers son el núcleo funcional de la app.

### Application layer
- [ ] `TreatmentHandlers.kt` — `ScheduleTreatmentHandler`, `CompleteTreatmentHandler`, `DeleteTreatmentHandler`
- [ ] `TreatmentEvents.kt` — `TreatmentScheduled`, `TreatmentCompleted`, `TreatmentDeleted`
- [ ] `TreatmentQueries.kt` — `ObserveTreatmentsByPlantationQuery`, `ObserveUpcomingTreatmentsQuery`, `GetTreatmentQuery`

### UI
- [ ] `ScheduleTreatmentSheet` — bottom sheet con tipo, fecha/hora, notas, checkbox "añadir a Calendar"
- [ ] `TreatmentDetailScreen` — ver detalle, marcar como completado, adjuntar foto
- [ ] `HomeViewModel` — conectar `ObserveUpcomingTreatmentsQuery` para mostrar próximos tratamientos en Home

### Tests
- [ ] Unit tests de los 3 handlers (mismo patrón que AIModel)

---

## FASE 3 — Calendar: handlers e integración con Treatment

**Por qué tercero:** `ScheduleTreatmentHandler` con `addToCalendar = true` necesita `CreateCalendarEventHandler`. `GoogleCalendarRepository` ya está implementado.

### Application layer
- [ ] `CalendarHandlers.kt` — `CreateCalendarEventHandler`, `UpdateCalendarEventHandler`, `DeleteCalendarEventHandler`
- [ ] `CalendarEvents.kt` (domain events) — `CalendarEventCreated`, `CalendarEventDeleted`
- [ ] `CalendarQueries.kt` — `GetCalendarsQuery` (lista cuentas Google disponibles)
- [ ] Enlazar `ScheduleTreatmentHandler`: si `addToCalendar == true`, invocar `CreateCalendarEventHandler` y guardar `calendarEventId` en el `Treatment`

### UI
- [ ] `CalendarScreen` — vista de calendario mensual con tratamientos programados
- [ ] Añadir tab **Calendar** al bottom nav (`Screen.Calendar` ya definido en `NavGraph`)
- [ ] Selector de cuenta Google en `ScheduleTreatmentSheet`
- [ ] Solicitar permisos `READ_CALENDAR` / `WRITE_CALENDAR` en runtime antes de mostrar la pantalla

### Tests
- [ ] Unit tests de handlers con `CalendarRepository` mockeado

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

## FASE 5 — Weather (AEMET): parseo real, caché y WorkManager

**Por qué quinta:** `AemetWeatherRepository.fetchWeather()` obtiene la URL de AEMET pero devuelve `forecast = emptyList()` — la segunda llamada HTTP (a la URL de datos) ni siquiera se hace.

### Infrastructure
- [ ] `AemetWeatherRepository.fetchWeather()` — completar parseo:
  1. Primera llamada → obtener `datos` URL
  2. Segunda llamada → descargar JSON de predicción
  3. Mapear `elaborado`, `prediccion.dia[]` → `DailyForecast` usando `mapSkyState()` (ya implementado)
- [ ] `observeCachedWeather()` — actualmente `flowOf(null)`; implementar con Room (`WeatherEntity`, `WeatherDao`)
- [ ] `WeatherEntity` + `WeatherDao` + migración de base de datos (version 2)
- [ ] `AemetWeatherRepository` como binding en DI (`RepositoryModule`)
- [ ] `WeatherRefreshWorker` — WorkManager que llama `refreshWeather()` cada 6h
- [ ] Scheduling del Worker en `AgroAIApplication.onCreate()`

### Application layer
- [ ] `WeatherHandlers.kt` — `RefreshWeatherHandler`
- [ ] `WeatherQueries.kt` — `ObserveWeatherQuery`, `GetWeatherAlertsQuery`

### UI
- [ ] Widget de clima en `PlantationDetailScreen` (temperatura, condición, próximas alertas)
- [ ] Alerta visible en `TreatmentDetailScreen` si hay lluvia/helada en la fecha programada
- [ ] Pantalla de configuración AEMET en Settings (campo de API key ya existe, falta conectarlo)

### Tests
- [ ] Unit test de parseo AEMET (JSON de muestra real)
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
- [ ] Completar `strings.xml` (en) y `strings-es.xml` — hay literales hardcodeados en inglés en algunas pantallas

### Pruebas instrumentadas pendientes
- [ ] `ModelManagementScreenTest`
- [ ] `TreatmentDetailScreenTest`
- [ ] `CalendarScreenTest`

---

## Resumen de orden

| Fase | Bloqueante para | Esfuerzo estimado |
|------|----------------|-------------------|
| 1 — AIModel screen | Usar cualquier función de IA | M |
| 2 — Treatment handlers + UI | Flujo principal de la app | L |
| 3 — Calendar handlers + tab | Integración calendario | M |
| 4 — PhotoAnalysis fix | Valor real del análisis de fotos | S |
| 5 — Weather AEMET | Alertas y recomendaciones | L |
| 6 — Deuda técnica | Release pública | M |

`S` = 1-2 días · `M` = 3-5 días · `L` = 1-2 semanas
