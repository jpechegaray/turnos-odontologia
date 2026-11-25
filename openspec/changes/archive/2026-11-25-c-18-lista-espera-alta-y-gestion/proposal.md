# Proposal

## Why

La lista de espera es el diferenciador del producto (DD-10). Antes de avisar huecos (C-31, C-36) o de mostrarla en pantalla (C-47), recepción tiene que poder anotar pacientes con sus preferencias. Las entradas vencidas tienen que dejar de contar. C-04, C-06 y C-07 ya proveen prestaciones, profesionales y pacientes.

## What Changes

- Agregar el alta, la consulta y la baja de entradas de lista de espera:
  - `POST /api/lista-espera`
  - `GET /api/lista-espera`, con filtros opcionales por estado, paciente y prestación
  - `GET /api/lista-espera/{id}`
  - `DELETE /api/lista-espera/{id}`: no borra la entrada, la pasa a `CANCELADA`
- Cada entrada tiene:
  - paciente y prestación, obligatorios;
  - profesional preferido, opcional;
  - rango de fechas `[fechaDesde, fechaHasta]`: `fechaHasta` es obligatoria y `fechaDesde` toma la fecha de hoy si no se envía (decisión del usuario, 2026-11-25);
  - franja horaria preferida, opcional, en hora local;
  - estado `ACTIVA`, `ASIGNADA`, `CANCELADA` o `VENCIDA`.
- Reglas de RN-21:
  - prestación obligatoria, existente y activa;
  - el paciente tiene que existir, estar activo y tener email (PA-16);
  - un paciente no puede tener dos entradas `ACTIVA` para la misma prestación, garantizado también en PostgreSQL;
  - el profesional preferido, si se indica, tiene que existir y estar activo.
- Las transiciones del dominio (`asignar`, `cancelar` y `vencer`) quedan definidas completas para que C-24, C-31 y C-42 no reabran la entidad. Solo se pueden aplicar sobre una entrada `ACTIVA`.
- Se agrega un planificador diario que marca `VENCIDA` toda entrada `ACTIVA` con `fechaHasta` anterior a hoy. Hoy se calcula en Buenos Aires con el `Clock` inyectable.
- Permisos: `RECEPCION` y `ADMIN` gestionan las entradas. `ODONTOLOGO` solo las lee (matriz de `03_actores_y_roles.md`) y recibe `403` al escribir (RN-33).
- Quedan fuera:
  - la compatibilidad con huecos (C-31);
  - las ofertas y avisos (C-36, C-42);
  - la asignación manual desde la API (C-31 o C-43);
  - el frontend (C-47);
  - editar una entrada: para cambiar preferencias se cancela y se crea otra.

## Capabilities

### New Capabilities

- `lista-espera`: alta, consulta, baja y vencimiento automático de entradas de lista de espera, y el ciclo de estados que usarán los changes de ofertas y asignación.

### Modified Capabilities

Ninguna.

## Impact

- Se agrega el paquete backend `listaespera`, organizado en dominio, aplicación, infraestructura y web.
- Se crea la tabla `lista_espera`. En `schema-constraints.sql` se suman sus FK y un índice único parcial para las entradas activas.
- Se suman matchers de escritura en `SecurityConfig`, como en C-11 y C-12.
- Es la primera tarea programada del proyecto: se habilita `@EnableScheduling`.
- Se consumen `PacienteConsulta`, `ConsultaPrestaciones` y `ProfesionalConsulta` sin modificarlos.
- C-31, C-36 y C-47 consumirán esta capacidad.
