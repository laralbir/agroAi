# AgroAI

**Gestión agrícola inteligente con IA local para Android**

AgroAI es una aplicación Android nativa que utiliza el modelo Gemma (3/4) ejecutándose completamente en el dispositivo para asistir en la gestión y planificación de plantaciones agrícolas. No envía datos a servidores externos — toda la inteligencia artificial funciona en local.

---

## Características

### Gestión de Plantaciones
- Registro de múltiples plantaciones con wizard detallado
- Tipos de plantación: huerta, secano, regadío, viñedo, olivar, frutal, cítricos, invernadero, aromáticas, medicinales, cereal, leguminosas, tubérculos, floricultura, vivero, bosque, pradera, montaña, aguacate, arrozal, platanera y más
- Gestión de plantas por variedad, distancia de siembra y número de ejemplares
- Registro de ubicación (GPS o manual) con dirección, municipio y provincia
- Medición de superficie en m²
- Historial de acciones y tratamientos por plantación

### IA Local con Gemma
- Análisis de fotos de plantas, árboles, frutos y plantaciones
- Identificación de enfermedades, plagas y necesidades de mantenimiento
- Sugerencias de tratamientos con opción de agendarlos en Google Calendar
- Modelos disponibles:
  - **Gemma 3 1B** — Gama media (~2 GB)
  - **Gemma 3 4B** — Gama alta (~5 GB)
  - **Gemma 3 12B** — Gama premium (~14 GB)
  - **Gemma 4** — Próximamente
- Prompts editables con indicación de nivel de riesgo

### Integración Google Calendar
- Soporte multi-cuenta Google
- Planificación de eventos: cosecha, riego, poda, fertilización, fumigación, trasplante
- Notificaciones y recordatorios
- Verificación de conflictos meteorológicos en eventos agendados

### Meteorología (AEMET)
- Integración con la API de AEMET (OpenData)
- Consulta de predicción por municipio
- Alertas meteorológicas que pueden afectar a tratamientos agendados
- Actualización automática cada 6 horas

### Personalización
- Temas: claro, oscuro o seguir el sistema
- Idiomas: español e inglés (configurable o automático según el sistema)
- Material You (Dynamic Color) en Android 12+
- APIs externas configurables (AEMET pre-incluida)

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
│  (Room, Gemma, Calendar, AEMET)     │
└─────────────────────────────────────┘
```

---

## Requisitos

- Android 8.0 (API 26) o superior
- **Recomendado**: Android 12+ para Material You
- Espacio libre según modelo:
  - Gemma 3 1B: ~2 GB
  - Gemma 3 4B: ~5 GB
  - Gemma 3 12B: ~14 GB
- RAM según modelo:
  - Gemma 3 1B: 3 GB disponibles
  - Gemma 3 4B: 6 GB disponibles
- Cuenta Google para integración con Calendar (opcional)
- API key de AEMET para datos meteorológicos (opcional)

### Dispositivos de Desarrollo y Prueba
- Google Pixel 9 Pro XL (Android 15)
- Google Pixel 10 Pro XL (Android 16)

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
La primera vez que se inicia la app, aparece un wizard para descargar el modelo Gemma seleccionado. El modelo se descarga desde los servidores oficiales de Google/Kaggle y se almacena localmente.

### Configurar API de AEMET (opcional)
1. Registrarse en [OpenData AEMET](https://opendata.aemet.es/)
2. Obtener API key
3. En la app: Settings → APIs externas → AEMET → Introducir API key

---

## Stack Tecnológico

| Componente | Tecnología |
|-----------|-----------|
| Lenguaje | Kotlin 2.x |
| UI | Jetpack Compose + Material Design 3 |
| IA en local | MediaPipe Tasks GenAI |
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
│   └── src/main/kotlin/com/laralnet/agroai/
│       ├── core/              # Infraestructura transversal
│       ├── plantation/        # Bounded Context: Plantaciones
│       ├── aimodel/           # Bounded Context: Modelos IA
│       ├── treatment/         # Bounded Context: Tratamientos
│       ├── weather/           # Bounded Context: Meteorología
│       ├── calendar/          # Bounded Context: Calendario Google
│       ├── account/           # Bounded Context: Cuentas
│       ├── database/          # AppDatabase (Room)
│       └── ui/                # Pantallas y navegación
├── CLAUDE.md                  # Guía de desarrollo para Claude
├── CHANGELOG.md               # Historial de cambios
└── README.md                  # Este archivo
```

---

## Contribuir

1. Fork del repositorio
2. Crear rama feature: `git checkout -b feature/nueva-funcionalidad`
3. Commits descriptivos siguiendo Conventional Commits
4. Pull Request con descripción detallada

---

## Licencia

Copyright © 2026 LaralNet. Todos los derechos reservados.

---

## Contacto

- **Web**: [laralnet.com](https://laralnet.com)
- **App domain**: laralnet.com.agroai
- **Email**: laralbir@gmail.com
