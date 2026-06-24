# Changelog

Todos los cambios notables de AgroAI se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es/1.0.0/) y el proyecto usa [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

### Añadido
- Estructura inicial del proyecto con arquitectura hexagonal + DDD + CQRS
- Configuración de Gradle con Version Catalog
- Módulos de dominio: plantation, aimodel, treatment, weather, calendar, account
- Integración MediaPipe Tasks GenAI para Gemma 3/4 en local
- Base de datos Room con migraciones
- Inyección de dependencias con Hilt
- UI con Jetpack Compose + Material Design 3
- Soporte de temas: claro, oscuro, sistema
- Internacionalización: inglés y español
- Wizard de alta de plantación con tipos de cultivo
- Análisis de fotos con Gemma
- Integración con Google Calendar (multi-cuenta)
- Integración con API AEMET para meteorología
- Gestión de descarga de modelos Gemma
- Prompts editables con indicación de riesgo
- Historial de acciones por plantación

---

## [0.1.0] - 2026-06-24

### Añadido
- Inicialización del proyecto
- Estructura de directorios y arquitectura base
- CLAUDE.md con guía de desarrollo
- README.md con documentación del proyecto
- CHANGELOG.md con historial de versiones
- Configuración inicial de Gradle (Version Catalog)
- AndroidManifest.xml con permisos necesarios
- Tema Material You con soporte claro/oscuro
- Modelos de dominio iniciales

---

[Unreleased]: https://github.com/laralnet/agroai/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/laralnet/agroai/releases/tag/v0.1.0
