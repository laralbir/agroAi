# AgroAI

**Gestión agrícola inteligente con IA local para Android**

📖 **[Guía de Usuario / User Guide](https://laralbir.github.io/agroAi/)** — Documentación completa en español e inglés

AgroAI es una aplicación Android nativa que utiliza el modelo Gemma (3/4) ejecutándose completamente en el dispositivo para asistir en la gestión y planificación de plantaciones agrícolas. No envía datos a servidores externos — toda la inteligencia artificial funciona en local.

---

## Características

### Gestión de Plantaciones
- Registro de múltiples plantaciones con wizard detallado (crear y editar)
- Tipos de plantación: huerta, secano, regadío, viñedo, olivar, frutal, cítricos, invernadero, aromáticas, medicinales, cereal, leguminosas, tubérculos, floricultura, vivero, bosque, pradera, montaña, aguacate, arrozal, platanera y más
- Gestión de plantas por variedad, distancia de siembra y número de ejemplares
- Registro de ubicación (GPS o manual) con dirección, municipio y provincia
- Medición de superficie en m²
- Historial de tratamientos por plantación con estado en tiempo real

### Tratamientos
- Programación de tratamientos: riego, poda, cosecha, fertilización, fumigación, injerto, trasplante y otros
- Título, descripción, fecha y hora configurables por tratamiento
- Marcar tratamiento como completado con notas de ejecución
- Eliminar tratamiento (con borrado opcional del evento de calendario)
- **Pantalla de inicio**: widget de meteorología expandible por GPS (tap → `WeatherDetailSheet` con temperatura aparente, viento, precipitación y previsión 3 días) + tratamientos de hoy (hasta 3) y próximos hasta 5 días
- **Botón "Revisar ahora"** en la barra superior de Home — lanza `PlantationHealthWorker` para todas las plantaciones de forma inmediata
- **Indicador de próxima revisión automática** en Home: "Próxima revisión en Xh Ym" basado en el estado del `PeriodicWorkRequest`
- Integración directa con Google Calendar al programar (opcional)

### Acciones de Plantación
- Acciones generadas automáticamente por el worker de salud en background (ciclo de 6 horas) a partir del análisis de la IA
- **Botón "Revisar esta plantación"** en `PlantationDetailScreen` — lanza el worker solo para esa plantación (vía `INPUT_KEY_PLANTATION_ID`)
- 12 tipos: Regar, Podar, Cosechar, Fertilizar, Fumigar, Injertar, Trasplantar, Supervisar, Tratar, Observar, Reparar, Otro
- Estados: Pendiente, Hecha, Omitida
- Origen distinguible: Manual o IA
- `ActionListScreen`: lista filtrable (Todas / Pendientes / Hechas) por plantación
- `ActionDetailScreen`: detalle completo con notas y opción de marcar como hecha
- Notificaciones de nuevas acciones sugeridas (canal `plantation_health`, Android 13+ requiere permiso)

### IA Local con Gemma
- Análisis de fotos de plantas, árboles, frutos y plantaciones
- Identificación de enfermedades, plagas y necesidades de mantenimiento
- Muestra especie detectada, estado general y problemas encontrados
- Sugerencias de tratamientos con tipo, urgencia y fecha sugerida
- Botón "Agendar" en cada sugerencia: navega a la pantalla de programación con datos pre-rellenados; el JSON del análisis se guarda junto al tratamiento para trazabilidad
- Dos SDKs de inferencia en local según el formato del modelo:
  - **MediaPipe Tasks GenAI** (`.task`) — Gemma 3
  - **LiteRT-LM** (`.litertlm`) — Gemma 4 (nuevo formato nativo de Google AI Edge)
- Modelos disponibles:
  - **Gemma 3 1B** — ~0.6 GB, ~2 GB RAM ([litert-community/Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT))
  - **Gemma 4 E2B** — ~2.5 GB, ~4 GB RAM ([litert-community/gemma-4-E2B-it-litert-lm](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm))
- Conexión con cuenta HuggingFace vía **OAuth 2.0** (sin copiar tokens manualmente)
- **Recuperación de token automática**: si la descarga falla con HTTP 401, el Worker intenta renovar el token con el refresh token y reintenta; si el refresh también falla, aparece el botón "Reconectar cuenta HuggingFace" que relanza el OAuth y continúa la descarga automáticamente
- **Prueba de modelo integrada** — verifica que el modelo descargado funciona con una pregunta agrícola de muestra; muestra tiempos de carga e inferencia
- El primer modelo descargado se activa automáticamente
- **Editor de prompts** (Ajustes → "Gestionar prompts"): selector de 3 plantillas (Análisis de fotos, Salud de plantación, Ajuste meteorológico); diálogo de advertencia al guardar con nivel Medio o Alto; botón "Restablecer" con confirmación; vista previa del prompt completo con datos de plantación de ejemplo; badge "Personalizado" cuando la plantilla ha sido editada

### Integración Google Calendar
- Soporte multi-cuenta Google
- Selector nativo de cuentas Google del sistema (con foto de perfil), sin introducir el email manualmente
- Diálogo de cambio de cuenta con opción de migrar tratamientos agendados a la nueva cuenta
- Planificación de eventos: cosecha, riego, poda, fertilización, fumigación, trasplante
- Notificaciones y recordatorios
- Verificación de conflictos meteorológicos en eventos agendados

### Meteorología (Open-Meteo)
- Integración con [Open-Meteo](https://open-meteo.com/) — API pública y gratuita, sin clave API
- Predicción basada en coordenadas GPS de cada plantación (funciona en cualquier país); fallback automático a la primera plantación con coordenadas si el GPS falla
- **`HomeWeatherWidget` expandible** en la pantalla de inicio: muestra el municipio de la ubicación GPS, condición + temperatura; tap → `WeatherDetailSheet` con temperatura aparente, viento, precipitación y previsión completa
- **Previsión de 15 días** en el detalle de plantación (sección desplegable) con temperatura máx/mín, probabilidad de lluvia e índice UV por día
- **Ubicación visible** en todos los widgets y paneles de detalle para saber de qué punto geográfico es el pronóstico
- **Spinner de carga** mientras se obtiene el tiempo por primera vez — sin saltos de layout
- **Clima por plantación en el listado** (`PlantationListScreen`): emoji WMO + temperatura máx del día junto a cada plantación (datos de caché, sin llamadas extra)
- **`WeatherDetailSheet` en el detalle de plantación**: tap en `WeatherCard` → sheet con previsión, sensación térmica y datos detallados
- **Alertas en el detalle de tratamiento**: aviso destacado si el pronóstico del día programado incluye helada, lluvia intensa, tormenta, granizo o nieve
- Caché inteligente en Room (tabla `weather_cache`) — se refresca solo si el dato tiene más de 1 hora; refresh forzado tras análisis de imagen
- Actualización automática cada 6 horas vía WorkManager (requiere conexión)

### Personalización
- Temas: claro, oscuro o seguir el sistema con paleta de colores naturales (verde salvia, gris cálido, ámbar)
- Barra de título compacta que maximiza el espacio de contenido
- Idiomas: español e inglés (configurable o automático según el sistema)

---

## Arquitectura

La aplicación sigue los principios de **Arquitectura Hexagonal (Ports & Adapters)** combinados con **Domain-Driven Design (DDD)**, **CQRS** y **Event-Driven Architecture**:

```
┌─────────────────────────────────────┐
│              UI Layer               │  Jetpack Compose
│  (Screens, ViewModels, Navigation)  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          Application Layer          │  Use Cases
│  (Commands, Queries, Handlers)      │  CQRS
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│           Domain Layer              │  DDD
│  (Aggregates, Entities, Events)     │  Puro Kotlin
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        Infrastructure Layer         │  Adapters
│  (Room, Gemma, Calendar, Open-Meteo)│
└─────────────────────────────────────┘
```

---

## Requisitos

- Android 8.0 (API 26) o superior
- **Recomendado**: Android 12+ para Material You
- Espacio libre: ~0.6 GB para Gemma 3 1B o ~2.5 GB para Gemma 4 E2B
- RAM: mínimo 2 GB disponibles para inferencia
- Cuenta gratuita de HuggingFace (para descargar modelos Gemma, autenticación vía OAuth)
- Cuenta Google para integración con Calendar (opcional)

### Dispositivos de Desarrollo y Prueba
- Google Pixel 9 Pro XL (7)
- Google Pixel 10 Pro XL (Android 17)

---

## Instalación para Desarrollo

### Prerrequisitos
- Android Studio Meerkat (2025.1) o superior
- JDK 17 o superior
- SDK Android API 26-36
- NDK (para inferencia Gemma)

### Clonar y compilar
```bash
git clone https://github.com/laralnet/agroai.git
cd agroai
./gradlew assembleDebug
./gradlew installDebug
```

### Configurar modelo Gemma
1. Crea una cuenta gratuita en [huggingface.co](https://huggingface.co) (no hace falta generar un token manualmente)
2. En la app: **Ajustes → Cuenta HuggingFace** → pulsa *Conectar* → la app abre un navegador seguro (Chrome Custom Tab) con la pantalla de autorización de HuggingFace
3. Aprueba el acceso en el navegador → la app recibe el token automáticamente vía OAuth
4. Ve a **Ajustes → Modelos de IA** → pulsa *Descargar* en el modelo deseado:
   - **Gemma 3 1B** (`gemma3-1b-it-int4.task`, ~555 MB) — recomendado para gama media
   - **Gemma 4 E2B** (`gemma-4-E2B-it.litertlm`, ~2.5 GB) — Gemma 4 en formato LiteRT-LM, mejor calidad
5. El modelo se almacena en el almacenamiento externo de la app y se activa automáticamente al completarse (si no había ninguno activo)
6. Opcional: usa el botón *Probar* en la tarjeta del modelo para verificar que funciona; el prompt es editable antes de ejecutar

---

## Stack Tecnológico

| Componente | Tecnología |
|-----------|-----------|
| Lenguaje | Kotlin 2.x |
| UI | Jetpack Compose + Material Design 3 |
| IA en local (Gemma 3) | MediaPipe Tasks GenAI (`.task`) |
| IA en local (Gemma 4) | LiteRT-LM (`litertlm-android`) |
| Base de datos | Room (SQLite) |
| Inyección de dependencias | Hilt |
| Programación async | Coroutines + Flow |
| Preferencias | DataStore Proto |
| Red | Retrofit + OkHttp |
| Imágenes | CameraX + Coil |
| Trabajo en background | WorkManager |
| Navegación | Navigation Compose |
| Build system | Gradle con Version Catalog |

---

## Estructura del Proyecto

```
agroAI/
├── app/
│   └── src/
│       ├── main/kotlin/com/laralnet/agroai/
│       │   ├── core/              # Infraestructura transversal
│       │   ├── plantation/        # Bounded Context: Plantaciones
│       │   ├── aimodel/           # Bounded Context: Modelos IA
│       │   ├── treatment/         # Bounded Context: Tratamientos
│       │   ├── weather/           # Bounded Context: Meteorología
│       │   ├── calendar/          # Bounded Context: Calendario Google
│       │   ├── account/           # Bounded Context: Cuentas
│       │   ├── database/          # AppDatabase (Room)
│       │   └── ui/                # Pantallas y navegación
│       ├── test/                  # Unit tests (JVM, sin dispositivo)
│       └── androidTest/           # Instrumented tests (requieren dispositivo o emulador)
├── CLAUDE.md                  # Guía de desarrollo para Claude
├── CHANGELOG.md               # Historial de cambios
└── README.md                  # Este archivo
```

### Directorios de tests

El proyecto mantiene dos directorios de tests separados por una razón técnica, no organizativa:

| Directorio | Ejecución | Qué contiene | Comando |
|-----------|-----------|-------------|---------|
| `src/test/` | JVM local (sin dispositivo) | Tests de dominio, handlers CQRS, ViewModels, mappers, parsers, workers (con mocks) | `./gradlew test` |
| `src/androidTest/` | Dispositivo o emulador Android | Tests de DAOs Room (SQLite nativo), pantallas Compose (`createComposeRule()`) | `./gradlew connectedAndroidTest` |

Los tests de `src/androidTest/` **no pueden** ejecutarse en JVM porque dependen de APIs exclusivas de Android: Room necesita el SQLite nativo del sistema operativo Android, y los tests de Compose necesitan el runtime gráfico de Android. Moverlos a `src/test/` causaría `ClassNotFoundException` en tiempo de ejecución.

---

## Contribuir

1. Fork del repositorio
2. Crear rama feature: `git checkout -b feature/nueva-funcionalidad`
3. Commits descriptivos siguiendo Conventional Commits
4. Pull Request con descripción detallada

---

## Licencia

Distribuido bajo la licencia **Apache 2.0**. Consulta el fichero [LICENSE](LICENSE) para más detalles.

Las dependencias de terceros tienen sus propias licencias:

| Librería | Licencia |
|---------|---------|
| Jetpack Compose, Room, Hilt, WorkManager, CameraX | Apache 2.0 |
| Kotlin, Kotlinx Coroutines | Apache 2.0 |
| MediaPipe Tasks GenAI (Gemma 3) | Apache 2.0 |
| LiteRT-LM (Gemma 4) | Apache 2.0 |
| Retrofit, OkHttp | Apache 2.0 |
| Coil | Apache 2.0 |
| Gemma model weights | [Gemma Terms of Use](https://ai.google.dev/gemma/terms) |

---

## Contacto

- **App domain**: laralnet.com.agroai
- **Email**: laralbir@gmail.com
