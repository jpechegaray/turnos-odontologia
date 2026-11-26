# Proposal

## Why

Desde C-38 un paciente puede ver los horarios libres, pero todavía no puede reservar: falta el `POST /api/public/reservas` de US-015 (Flujo 4). PA-08 ya está resuelta en `docs/propuestas/pa-08-consentimiento-datos.md`, así que este change puede pedir y registrar el consentimiento de la Ley 25.326 que exige RN-26. Es el último paso de backend antes de la pantalla de reserva de C-48.

## What Changes

- **Reserva online** `POST /api/public/reservas` (anónimo, sin CSRF, con el límite de tasa de C-17):
  - Recibe la prestación, el profesional, el inicio, nombre, apellido, email, teléfono, obra social (texto), `aceptaTratamientoDatos` y `versionConsentimiento`. No pide DNI (RN-26).
  - Valida los mismos recursos que C-38, con el mismo 404 uniforme. Valida el horizonte y la anticipación mínima (RN-27) y el tope de turnos futuros activos por email (RN-27).
  - Resuelve el paciente por email y teléfono (RN-28): reutiliza la ficha activa sin modificarla o crea una ficha nueva.
  - Asigna el primer sillón activo libre (SU-03) y crea el turno con el servicio de crear turno de `agenda`, con canal `ONLINE` y la duración por defecto de la prestación. No duplica reglas.
  - Si el hueco se ocupó, responde `409` con hasta 5 horarios alternativos (RN-08, RN-15).
  - Emite el `TokenTurno` (C-32) y responde `201` con el token una única vez.
  - Después de confirmar la reserva envía el email `CONFIRMACION_RESERVA` con el enlace. Si el envío falla, la reserva queda hecha y la notificación queda `FALLIDA`, con reintentos (RN-30, RN-31).
- **Consentimiento (PA-08):**
  - Responde `422` si falta la aceptación o si la versión no es la vigente (`2026-11-v1`).
  - Con un paciente nuevo, guarda `consentimientoDatosEn` y la versión en la ficha.
  - Con una ficha existente, guarda el consentimiento ligado al turno y no toca la ficha.
- **Configuración (C-08):** tres parámetros nuevos, `consultorio.domicilio`, `consultorio.email_contacto` y `consultorio.retencion_anios` (2 por defecto). El reemplazo completo pasa de siete a diez claves. **BREAKING** para el contrato de `PUT /api/configuracion`, así que en el mismo change se agregan los tres campos a la pantalla `/configuracion`.
- **Datos públicos del consultorio** `GET /api/public/consultorio`: nombre, domicilio, email de contacto, años de retención y versión vigente del consentimiento, para que C-48 arme el texto.
- **Documentación:**
  - PA-08 pasa a resuelta en `knowledge-base/10_preguntas_abiertas.md` y en el estado de `docs/propuestas/pa-08-consentimiento-datos.md`.
  - Se agrega el supuesto de la AAIP en `knowledge-base/09_decisiones_y_supuestos.md`.
  - PA-09 sigue abierta y se mantiene el comportamiento actual (SU-03).
- **Fuera de alcance:** el texto del consentimiento (`textoConsentimiento.ts`) y la pantalla de reserva, que son de C-48. La supresión real de datos y el borrado automático por retención quedan fuera del MVP (PA-08 §4).

## Capabilities

### New Capabilities

Ninguna. La reserva extiende la capacidad `reserva-publica` que creó C-38.

### Modified Capabilities

- `reserva-publica`: agrega la creación de la reserva online (datos, consentimiento, deduplicación, límites, asignación de sillón, 409 con alternativas, token y email) y la consulta pública de los datos del consultorio.
- `configuracion-parametros`: la tabla de valores iniciales, la consulta, el reemplazo y las lecturas tipadas pasan de siete a diez claves.
- `configuracion-frontend`: la pantalla de configuración carga, valida, muestra y guarda las diez claves.

## Impact

- **Backend:**
  - `reservapublica`: nuevo subpaquete `reserva` (servicio, DTO, controlador y reconstructor del email), endpoint de consultorio y constante de la versión del consentimiento.
  - `agenda`: un método de creación para el canal `ONLINE`, sin `@PreAuthorize` de personal, sobre el mismo núcleo de `CrearTurnoService`. También una consulta que cuenta los turnos futuros activos por paciente.
  - `pacientes`: un puerto para buscar las fichas activas por email, crear una ficha con consentimiento y una columna nueva `consentimiento_version`.
  - `configuracion`: tres valores nuevos en `ParametroConfiguracion` y tres lecturas tipadas.
  - Esquema: Hibernate `ddl-auto` crea la columna nueva de paciente y la tabla nueva del consentimiento por turno. No hace falta tocar el script SQL.
- **Frontend:** `features/configuracion` (`claves.ts`, `tipos.ts`, validación y tests) con tres campos más.
- **API:** `POST /api/public/reservas` y `GET /api/public/consultorio` son nuevos. `GET` y `PUT /api/configuracion` pasan a diez claves.
- **Dependencias:** C-07, C-08, C-15, C-17, C-19, C-32 y C-38, todas mergeadas en `master`. Desbloquea C-48.
