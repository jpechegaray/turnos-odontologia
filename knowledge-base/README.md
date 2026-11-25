# Turnos Odontología — Base de Conocimiento

Base de conocimiento generada a partir del Discovery del proyecto (`docs/discovery/discovery.md`, fuente principal, y `docs/discovery/informe-discovery.md`, análisis de 23 sistemas) y de las decisiones técnicas del equipo.

## Índice de Archivos

| Archivo | Contenido |
|---------|-----------|
| [01_vision_y_objetivos.md](01_vision_y_objetivos.md) | Propósito, posicionamiento frente a competidores, vacíos del mercado argentino, diferenciador, objetivos por actor, alcance del MVP y fuera de alcance |
| [02_descripcion_general.md](02_descripcion_general.md) | Stack (Java 21 + Spring Boot 3, PostgreSQL 16, React + TypeScript), arquitectura general, integraciones y resumen de la API REST |
| [03_actores_y_roles.md](03_actores_y_roles.md) | Odontólogo (solo lectura), recepción, administrador y paciente con token; matriz RBAC; rutas públicas |
| [04_modelo_de_datos.md](04_modelo_de_datos.md) | ERD (mermaid), entidades, restricciones `EXCLUDE USING gist`, configuración por defecto y seed ficticio |
| [05_reglas_de_negocio.md](05_reglas_de_negocio.md) | RN-01 a RN-38: solapamientos, duración, horarios y bloqueos, cancelación, lista de espera, token, notificaciones, roles |
| [06_funcionalidades.md](06_funcionalidades.md) | Épicas e historias de usuario del MVP (E1 a E9) y épicas futuras fuera de alcance (E10 a E17) |
| [07_flujos_principales.md](07_flujos_principales.md) | Crear, reprogramar y cancelar turnos, reserva online con token, recordatorio por email, login y toma de un hueco desde la lista de espera |
| [08_arquitectura_propuesta.md](08_arquitectura_propuesta.md) | Patrones, estructura `backend/` y `frontend/` para trabajo en paralelo, esquema con Hibernate, seguridad, testing y variables de entorno |
| [09_decisiones_y_supuestos.md](09_decisiones_y_supuestos.md) | Decisiones DD-01 a DD-14 (con alternativas descartadas) y supuestos SU-01 a SU-15 |
| [10_preguntas_abiertas.md](10_preguntas_abiertas.md) | Inconsistencias, preguntas abiertas priorizadas y datos "No evidenciado" del informe |

## Quick Start para Desarrolladores

1. Entender el dominio → [01](01_vision_y_objetivos.md), [03](03_actores_y_roles.md)
2. Entender los datos → [04](04_modelo_de_datos.md)
3. Entender las reglas → [05](05_reglas_de_negocio.md)
4. Entender la arquitectura → [02](02_descripcion_general.md), [08](08_arquitectura_propuesta.md)
5. Implementar → [07](07_flujos_principales.md), [06](06_funcionalidades.md)
6. Antes de codificar → [10](10_preguntas_abiertas.md) y los supuestos de [09](09_decisiones_y_supuestos.md)

## Resumen Ejecutivo

Aplicación web (API REST + SPA) para consultorios odontológicos argentinos de 2 a 5 profesionales, un consultorio por instalación. Garantiza cero solapamientos por profesional y por sillón (intervalos `[inicio, fin)`, validación en dominio más restricción `EXCLUDE` en PostgreSQL), permite reserva online sin cuenta mediante un token, y recupera los huecos con una lista de espera que avisa sola por email a los pacientes compatibles (gana el primero que confirma), que es el diferenciador.

El MVP se limita a agenda, turnos, reserva con token, ficha básica de paciente, recordatorios por email y tres roles. Historia clínica, WhatsApp, cobros, ARCA, obras sociales, multisede e IA quedan como épicas futuras. Solo se usan datos ficticios y nunca se versionan secretos.

Con las decisiones del líder del 2026-11-20 quedaron resueltas PA-01 (token y datos de reserva), PA-02 (aviso automático de la lista de espera), PA-03 (odontólogo solo lectura), PA-04 (sin tipos de sillón), IN-06 y, en parte, PA-06 (el paciente no reprograma). El 2026-11-25 se completó PA-06: la reprogramación tardía del personal se permite y se registra. Las preguntas pendientes están en `10_preguntas_abiertas.md`; los supuestos que la KB asume en el ínterin se marcan como `**Suposición:**`.
