# Proposal

## Why

El aviso automático de huecos (C-36) y las sugerencias de compatibilidad (C-31) ya permiten que la lista de espera se entere de un hueco libre, y C-19/C-26 dan todas las validaciones y la garantía de no solapamiento. Falta el camino manual que ya describe US-018 y SU-15: que recepción, por ejemplo tras un llamado, asigne ese hueco a una entrada compatible. Sin él, el paciente que confirma por teléfono queda atado a la oferta por email y el hueco puede perderse aunque haya un interesado a mano. Es un change de backend de la Ola 6 cuyas dependencias (C-19, C-26, C-31, C-36) ya están en `master` y que desbloquea el front de la lista de espera (C-47).

## What Changes

- **Endpoint `POST /api/lista-espera/{id}/asignar`** con `profesionalId`, `inicio` (ISO-8601 con zona) y `sillonId` opcional, más `notas` opcional. Crea un turno normal con **todas** las validaciones del alta (RN-01 a RN-04, RN-09, RN-11, RN-13) reutilizando `CrearTurnoService`, cierra la entrada (`ASIGNADA` con `turnoAsignadoId`) y responde `201` con la representación del turno.
- **Duración del turno**: se toma de la duración por defecto de la prestación de la entrada (RN-24, SU-14 d); el cuerpo no la admite. El hueco evaluado es `[inicio, inicio + duraciónDefaultMin)`.
- **Compatibilidad previa**: antes de crear el turno se valida que la entrada sea compatible con ese hueco (RN-22) usando `CompatibilidadListaEspera.esCompatible`. Una entrada incompatible responde `422` con código `ENTRADA_INCOMPATIBLE`.
- **Sillón opcional determinístico**: si llega `sillonId` se usa (y la creación lo revalida); si no, se resuelve el sillón libre con `DisponibilidadService` (C-26), prefiriendo el sillón sugerido de una oferta del mismo hueco cuando sigue libre y, si no, el primero activo libre (SU-14 d).
- **Cierre de la entrada y de las ofertas**: la entrada pasa a `ASIGNADA` guardando `turnoAsignadoId` y todas las ofertas `ENVIADA` del hueco (mismo `profesionalId` y `huecoInicio`) pasan a `TOMADA`. Una segunda asignación del mismo hueco compite bajo RN-15 y responde `409` (`RN-01`); una entrada que ya no está activa responde `409` (`ENTRADA_NO_ACTIVA`); una entrada inexistente, `404` (`ENTRADA_NO_ENCONTRADA`).
- **Transacción única**: leer/bloquear la entrada, crear el turno, guardar la entrada `ASIGNADA` y marcar las ofertas ocurren en una sola transacción; un fallo deja todo sin cambios.
- **Acceso**: `ADMIN` y `RECEPCION` pueden asignar; `ODONTOLOGO` recibe `403`; sin sesión, `401`; las escrituras exigen CSRF válido.
- **Quedan fuera:**
  - tomar el hueco desde el enlace público de la oferta (C-42);
  - el frontend de la lista de espera (C-47);
  - cambios a las reglas de compatibilidad (C-31) o al aviso automático (C-36), que se consumen sin modificar.

## Capabilities

### New Capabilities

- `asignacion-hueco`: asignación manual de un hueco a una entrada de la lista de espera por recepción: validación de la entrada y la compatibilidad, creación del turno con todas las reglas del alta, resolución del sillón, cierre de la entrada, toma de las ofertas del hueco y competición bajo RN-15.

### Modified Capabilities

Ninguna. `lista-espera` (estados y compatibilidad), `oferta-hueco` (estados de la oferta) y `agenda-turnos` (alta de turno) se consumen sin cambiar sus requisitos.

## Impact

- **Nuevo subpaquete `listaespera.asignacion`** (`domain`, `application`, `infrastructure`, `web`), siguiendo el patrón de `listaespera.oferta`: servicio de aplicación de la asignación, DTOs de entrada/salida y, si hace falta, un valor de dominio para el hueco a asignar.
- **Consume sin cambios:** `CrearTurnoService` (C-19), `DisponibilidadService` y `HorarioDisponible` (C-26), `BuscadorCompatibles` y `CompatibilidadListaEspera` (C-31), `EntradaListaEspera.asignar`, y `OfertaHueco`/`EstadoOferta` (C-36).
- **Pequeñas extensiones de persistencia:** bloqueo pesimista de la entrada en `ListaEsperaPersistencia`/`ListaEsperaJpaRepository` y una actualización `ENVIADA → TOMADA` por `(profesional_id, hueco_inicio)` en `OfertasHuecoPersistencia`/`OfertaHuecoJpaRepository`; se agrega `tomar()` al dominio `OfertaHueco`.
- **Seguridad:** una regla explícita para `POST /api/lista-espera/{id}/asignar` en `SecurityConfig` (ADMIN/RECEPCION).
- **Sin cambios de esquema** (no hay tablas ni columnas nuevas) ni de configuración.
- **Tests backend** con JUnit 5 + Spring Boot Test + AssertJ + Testcontainers (PostgreSQL real) + MockMvc. No aplica Vitest: el frontend de la lista de espera es C-47.
