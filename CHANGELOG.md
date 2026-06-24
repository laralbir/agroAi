# Changelog

Todos los cambios notables de AgroAI se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es/1.0.0/) y el proyecto usa [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

---

## [0.4.0-canary01] - 2026-06-24

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

## [0.3.0-canary01] - 2026-06-24

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

[Unreleased]: https://github.com/laralbir/agroAi/compare/v0.4.0-canary01...HEAD
[0.4.0-canary01]: https://github.com/laralbir/agroAi/compare/v0.3.0-canary01...v0.4.0-canary01
[0.3.0-canary01]: https://github.com/laralbir/agroAi/compare/v0.2.0...v0.3.0-canary01
[0.2.0]: https://github.com/laralbir/agroAi/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/laralbir/agroAi/releases/tag/v0.1.0
