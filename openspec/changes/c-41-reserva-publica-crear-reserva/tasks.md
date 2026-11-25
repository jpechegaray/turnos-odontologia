# Tasks

> TDD estricto: en cada tarea, primero el test en rojo con `@DisplayName` igual al escenario de la spec y después el código. Persistencia con Testcontainers (PostgreSQL 16). Solo datos ficticios (`@example.com`).

## 1. Documentación de PA-08 y PA-09

- [x] 1.1 Marcar PA-08 como resuelta (2026-11-25) en `knowledge-base/10_preguntas_abiertas.md`, con el resumen de la respuesta y un enlace a `docs/propuestas/pa-08-consentimiento-datos.md`. En la propuesta, pasar **Estado** a `aprobada`. Se verifica con `git diff`.
- [x] 1.2 Agregar en `knowledge-base/09_decisiones_y_supuestos.md` el supuesto de la AAIP: "un consultorio que adopte el sistema inscribe su base ante la AAIP antes de cargar pacientes reales". Verificar que el ID nuevo no choque con los existentes.
- [x] 1.3 Dejar PA-09 abierta y agregarle una nota: C-41 mantiene SU-03 (el paciente elige profesional y el sistema asigna el sillón). Se verifica con `git diff`.

## 2. Configuración: tres parámetros nuevos (C-08)

- [x] 2.1 Tests primero (`InicializadorConfiguracionIT`, `ParametroConfiguracionTest`, `ConfiguracionControllerIT`): "El primer arranque inicializa los siete valores por defecto" (el nombre se conserva por el validador; el cuerpo exige diez), "Los parámetros nuevos del consultorio tienen su valor por defecto", los escenarios de diez claves en GET y PUT, y RN-36 para la retención en 0 o 21 y los textos en blanco.
- [x] 2.2 Agregar `DOMICILIO_CONSULTORIO`, `EMAIL_CONTACTO_CONSULTORIO` y `RETENCION_ANIOS` (rango 1 a 20) a `ParametroConfiguracion`, con tres lecturas tipadas en `ConfiguracionServicio`. Ajustar los tests existentes que mandan siete claves y verificar que la suite de `configuracion` pase en verde.
- [x] 2.3 Front `features/configuracion`: agregar las tres claves a `tipos.ts` y `claves.ts`, ajustar `validacion.ts` y los tests de Vitest a los escenarios modificados de `configuracion-frontend` (diez claves, retención fuera de rango, domicilio o email en blanco). Se verifica con `npm test` en verde.

## 3. Pacientes: deduplicación y consentimiento

- [x] 3.1 Agregar el componente `consentimientoVersion` a `Paciente` y la columna en `PacienteJpa`, ajustar las 8 construcciones y verificar `PacienteTest` y `PacienteRepositorioIT`.
- [x] 3.2 IT de `activosPorEmail`: sin distinguir mayúsculas ni espacios, solo fichas activas y ordenadas por id. Implementarlo con JPQL parametrizado.
- [x] 3.3 Puerto `AltaPacienteReservaOnline` que crea la ficha con `consentimientoDatosEn` y la versión. Unit test con `Clock` fijo.

## 4. Agenda: alta por canal ONLINE y conteo para el tope

- [x] 4.1 Test de `Turno.nuevoOnline`: canal `ONLINE`, `PROGRAMADO` y sin creador.
- [x] 4.2 Extraer el núcleo privado de `CrearTurnoService` y agregar `crearOnline` (sin `@PreAuthorize`, `MANDATORY`). La suite de C-19 tiene que seguir en verde. Agregar un IT: sin transacción, `crearOnline` falla.
- [x] 4.3 Agregar `TurnosActivosConsulta.contarFuturosActivos(pacienteIds, ahora)`. IT: "Los turnos cancelados o pasados no cuentan para el tope".

## 5. Reserva online (`reservapublica.reserva`)

- [x] 5.1 Constante `ConsentimientoDatos.VERSION_VIGENTE` y validación del consentimiento. Tests: "Reservar sin aceptar el tratamiento de datos responde 422" y "Reservar con una versión de consentimiento que no es la vigente responde 422".
- [x] 5.2 Entidad y repositorio `consentimiento_turno` (`turno_id` único). IT de persistencia.
- [x] 5.3 `ReservaOnline` (D2 a D8): consentimiento, recursos (hacer público `validarRecursos` de `DisponibilidadPublica`), límites de RN-27, lock por email, tope, deduplicación, sillón, `crearOnline`, consentimiento, token y, después del commit, el email. Cubrir con ITs los escenarios de "Registro del consentimiento", "Deduplicación…", "Límites…", "Recursos no disponibles…" y "Turno creado con las reglas de la agenda", incluido "Una reserva con una ficha existente no modifica la ficha" y "Se supera el tope de turnos activos por email".
- [x] 5.4 409 con alternativas (D8). ITs: "Un hueco ocupado responde 409 con alternativas" y "Un 409 sin horarios libres devuelve alternativas vacías".
- [x] 5.5 Carreras con Testcontainers (dos hilos, `CountDownLatch`): "Dos reservas simultáneas del mismo hueco: una responde 201 y la otra 409" y "Dos reservas simultáneas del mismo email no superan el tope".
- [x] 5.6 `SolicitudReservaPublica`, `ReservaCreadaDto` y `POST /api/public/reservas` en un controlador nuevo, con `toString` que oculta datos. ITs MockMvc de la spec: 201, 400, sin DNI, respuesta sin datos de la ficha, el token da acceso al turno, el token no aparece en los logs (`OutputCaptureExtension`) y 429 del límite de tasa.

## 6. Email de confirmación

- [x] 6.1 IT con el adaptador de notificaciones en memoria: "La reserva envía el email de confirmación con el enlace".
- [x] 6.2 IT con un adaptador que falla: "Un fallo del email deja la reserva hecha y la notificación FALLIDA".
- [x] 6.3 `ReconstructorConfirmacionReserva` (D10). ITs: "El reintento de la confirmación lleva un enlace nuevo válido" y "El reintento de un turno cancelado se descarta".

## 7. Datos públicos del consultorio

- [x] 7.1 `ConsultorioPublicoDto` y `GET /api/public/consultorio`. ITs: "El consultorio expone los datos del aviso de consentimiento" y "Los datos del consultorio reflejan la configuración vigente".

## 8. Integración

- [x] 8.1 Correr `mvn verify` en `backend/` y `npm test` en `frontend/` con todo en verde, y anotar la cantidad de tests en el informe de verify.
- [x] 8.2 Actualizar la ficha de C-41 en `CHANGES.md` (estado y PR) y verificar que `openspec validate c-41-reserva-publica-crear-reserva --strict` pase.

## Workflow follow-up

- Correr `openspec-verify` y dejar `verify.md` con el veredicto.
- Preguntar antes de commitear. Después, pushear `lionel` con `--force-with-lease`, preguntando antes, y abrir un PR a `master` que incluya los commits de C-38 (archivo) y PA-08.
- Después del merge, `/opsx:archive`.
