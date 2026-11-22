# Proposal

## Why

Las rutas públicas `/api/public/**` deben permanecer abiertas y sin autenticación para permitir la reserva online, la gestión de turno por token y la aceptación de ofertas de la lista de espera, lo que las expone a ráfagas de solicitudes, scraping y abuso. C-17 implementa la protección transversal de límite de tasa en memoria (RN-27, US-015, SU-11) antes de habilitar los endpoints públicos que dependen de él (C-38, C-39, C-41 y C-42).

## What Changes

- Incorporar en `shared.ratelimit` un limitador de tasa en memoria basado en ventanas de tiempo con `Clock` inyectable y limpieza automática de contadores vencidos, sin agregar dependencias externas al `pom.xml`.
- Proveer un componente reutilizable de tope por clave arbitraria (por ejemplo, por email normalizado u otra clave de negocio) para que los módulos públicos puedan aplicar límites específicos además del filtro por IP.
- Registrar mediante `FilterRegistrationBean` un filtro HTTP por IP aplicable exclusivamente a `/api/public/**`, sin modificar `SecurityConfig`.
- Responder con estado `429 Too Many Requests`, cabecera `Retry-After` (en segundos enteros restantes hasta el reinicio de la ventana) y cuerpo en formato Problem Details (`application/problem+json`) con `codigo: "RN-27"` cuando se supere el umbral configurado, sin exponer datos personales ni claves en logs ni respuestas.
- Parametrizar los umbrales y la duración de ventana mediante `@ConfigurationProperties` bajo el bloque `app.ratelimit` reservado en `application.yml` (RM-02).
- Fuera de alcance: endpoints de disponibilidad pública (C-38), gestión de turno por token (C-39), creación de reserva pública y validación de máximo de turnos activos por email en BD (C-41), y toma de hueco desde oferta (C-42).

## Capabilities

### New Capabilities
- `limite-tasa-publico`: control de tasa de solicitudes en memoria por IP sobre `/api/public/**` y limitador reutilizable por clave con ventana temporal controlada por `Clock`, respuesta `429` Problem Details con `Retry-After` y limpieza de contadores vencidos.

### Modified Capabilities
- Ninguna.

## Impact

- **Código afectado**: paquete nuevo `ar.edu.utn.turnos.shared.ratelimit` (dominio/lógica de ventana y contadores, propiedades de configuración, filtro servlet y registro con `FilterRegistrationBean`) y sus pruebas unitarias y de integración en `backend/src/test/java/ar/edu/utn/turnos/shared/ratelimit/`.
- **Archivos compartidos**: `backend/src/main/resources/application.yml` (RM-02: solo se descomenta y completa el bloque `app.ratelimit`, en commit separado y avisando al equipo). No se edita `SecurityConfig`, `ManejadorErrores` ni `pom.xml`.
- **Dependencias del roadmap**: depende únicamente de `C-01` (ya mergeado en `master`) y desbloquea `C-38`, `C-39`, `C-41` y `C-42`.
- **Gobernanza**: **HIGH** — en esta etapa solo se generan los artefactos de planificación (`/opsx:propose`); no se escribe código hasta contar con la revisión y aprobación de Juan Pablo.
