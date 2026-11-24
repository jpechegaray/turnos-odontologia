# Proposal

## Why

Hoy no existe la ficha de paciente, y sin ella la agenda no puede asignar turnos ni la reserva pública puede deduplicar por email y teléfono. C-07 es el eslabón de la Ola 1 que desbloquea C-15, C-18, C-19, C-23, C-33, C-41 y C-45 (CHANGES.md, ficha C-07). Con C-01 ya mergeado, el backend tiene la base de seguridad, errores y tests como para agregar el primer módulo de dominio.

## What Changes

- Nuevo módulo `pacientes` con la entidad `Paciente` (nombre, apellido, DNI opcional y único, email, teléfono, obra social como texto, número de afiliado, notas, activo, consentimiento de datos y fecha de creación), según KB 04 §Paciente.
- Regla RN-32: nombre y apellido obligatorios, al menos un medio de contacto (email o teléfono), DNI único entre fichas activas cuando se informa (una ficha dada de baja libera su DNI), obra social sin validación y baja lógica en lugar de borrado físico.
- API `/api/pacientes`: alta, edición, consulta por id, búsqueda por apellido, nombre, DNI o email, y baja lógica. La escritura queda reservada a `RECEPCION` y `ADMIN`; la baja lógica, solo a `ADMIN`, según la matriz de KB 03. `ODONTOLOGO` no accede a estos endpoints en este change (la lectura limitada a sus turnos es C-28).
- Puerto `PacienteConsulta` (por id y por email) para que agenda, lista de espera y reserva pública lean la ficha sin acoplarse al repositorio.
- Errores con el formato existente: DNI duplicado devuelve 409, falta de medio de contacto devuelve 422, paciente inexistente devuelve 404 y falta de rol devuelve 403 (Problem Details con `codigo`).
- Tests de integración con PostgreSQL real (Testcontainers) y unitarios donde corresponda, con `@DisplayName` igual al escenario de la spec y solo datos ficticios.

## Capabilities

### New Capabilities
- `gestion-pacientes`: la ficha básica de paciente como comportamiento del sistema. Cubre los datos mínimos y sus validaciones, la unicidad del DNI, el medio de contacto obligatorio, la obra social como texto libre, la búsqueda, la baja lógica, los permisos por rol y la consulta de la ficha por id y por email para otros módulos.

### Modified Capabilities
- (ninguna)

## Impact

- **Código nuevo:** `backend/src/main/java/ar/edu/utn/turnos/pacientes/{domain,application,infrastructure,web}` y sus tests espejo en `backend/src/test/java/.../pacientes/`.
- **Archivos existentes:** no se editan. Se reutilizan `shared/error` (`ReglaNegocioException`, `EstadoError`), `shared/web/ManejadorErrores`, `shared/tiempo` y `SecurityConfig` con `@PreAuthorize` y `@WithMockUser`. El `pom.xml` ya trae todas las dependencias (RM-08).
- **Esquema:** la tabla `pacientes` la genera Hibernate (`ddl-auto`). La unicidad del DNI es un índice único parcial (`WHERE activo = true AND dni IS NOT NULL`) que Hibernate no puede generar; va en `schema-constraints.sql` (DD-08), que además descarta la unique completa de versiones previas. El "al menos un contacto" se resuelve en el dominio, sin `EXCLUDE`.
- **API:** se agregan rutas bajo `/api/pacientes`; no se modifica ninguna ruta existente.
- **Dependencias entre changes:** C-07 solo depende de C-01, que ya está mergeado. El login real y los roles persistidos llegan con C-03; mientras tanto los endpoints declaran sus roles y los tests los simulan con `@WithMockUser`, igual que la base que dejó C-01.
- **Gobernanza:** MEDIUM. Se implementa por pasos y se muestran las decisiones no obvias.

### Fuera de alcance

- Lectura del odontólogo limitada a los pacientes de sus turnos: **C-28**.
- Deduplicación por email y teléfono de la reserva online: **C-41**.
- Captura del consentimiento de datos personales y su texto legal: **C-41** (PA-08). C-07 solo deja la columna prevista en el modelo.
- Historial clínico o cualquier dato de salud: excluido del MVP por RN-35.
- Recordatorios por email, lista de espera y pantallas de pacientes: **C-15**, **C-18** y **C-23**.
- Seed de datos ficticios: **C-33**.
- Regla completa de "no borrar físicamente un paciente con turnos": se cierra en **C-19**, cuando exista la entidad `Turno`.
