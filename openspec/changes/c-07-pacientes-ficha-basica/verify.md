# Verify — c-07-pacientes-ficha-basica (2026-11-22)

**Veredicto:** LISTO PARA ARCHIVAR

---

## Validación estructural

```text
$ openspec validate c-07-pacientes-ficha-basica --strict
Change 'c-07-pacientes-ficha-basica' is valid
```

- Estructura OpenSpec completa y válida: `proposal.md`, `design.md`, `tasks.md` y `specs/gestion-pacientes/spec.md`.
- Veredicto de validación: **OK** (0 bloqueantes).

---

## Tareas

- **Total:** 22/22 completadas (100%).
- **Pendientes:** Ninguna.
- Detalle por sección:
  - Sección 1 (Dominio `Paciente` y puerto de repositorio): 4/4 completadas (`[x]`).
  - Sección 2 (Persistencia JPA): 4/4 completadas (`[x]`).
  - Sección 3 (Servicio de aplicación y puerto `PacienteConsulta`): 3/3 completadas (`[x]`).
  - Sección 4 (API web y permisos): 7/7 completadas (`[x]`).
  - Sección 5 (Verificación integral): 4/4 completadas (`[x]`).

---

## Matriz escenario → test

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| `gestion-pacientes` | Alta con email y sin teléfono | **Cubierto** | `PacienteGestionIT.java:44` | Valida 201, `id` generado, `activo=true`, `email` y ausencia de teléfono. |
| `gestion-pacientes` | Alta con teléfono y sin email | **Cubierto** | `PacienteGestionIT.java:64` | Valida 201, `telefono` informado y ausencia de email. |
| `gestion-pacientes` | La obra social se guarda como texto libre | **Cubierto** | `PacienteGestionIT.java:84` | Valida 201 y texto exacto de la obra social. |
| `gestion-pacientes` | Alta sin nombre o apellido se rechaza | **Cubierto** | `PacienteGestionIT.java:102` | Valida 400 Problem Details y lista de `errores` con campo `apellido`. |
| `gestion-pacientes` | Alta sin email ni teléfono se rechaza | **Cubierto** | `PacienteGestionIT.java:125` | Valida 422 Problem Details y `codigo="RN-32"`. También cubierto en unitario `PacienteTest.java:15`. |
| `gestion-pacientes` | Edición que quita el único medio de contacto se rechaza | **Cubierto** | `PacienteGestionIT.java:143` | Valida 422 Problem Details, `codigo="RN-32"` y conservación de datos originales. |
| `gestion-pacientes` | Edición que reemplaza email por teléfono se acepta | **Cubierto** | `PacienteGestionIT.java:168` | Valida 200, actualización a teléfono y baja de email. |
| `gestion-pacientes` | Un DNI repetido devuelve 409 | **Cubierto** | `PacienteGestionIT.java:194` | Valida 409 Problem Details y `codigo="RN-32"`. |
| `gestion-pacientes` | Varios pacientes pueden no tener DNI | **Cubierto** | `PacienteGestionIT.java:216` | Valida que múltiples registros sin DNI se creen con 201 sin conflicto. |
| `gestion-pacientes` | El DNI de una ficha inactiva sigue ocupado | **Cubierto** | `PacienteGestionIT.java:235` | Ficha dada de baja con ADMIN rechaza alta con mismo DNI (409 RN-32). |
| `gestion-pacientes` | Un DNI en blanco se trata como no informado | **Cubierto** | `PacienteGestionIT.java:257` | DNI `"   "` se normaliza a `null` y crea con 201 sin violar constraint. También cubierto en `PacienteTest.java:25`. |
| `gestion-pacientes` | Búsqueda por apellido sin distinguir mayúsculas | **Cubierto** | `PacienteGestionIT.java:281` | Valida 200 y coincidencia case-insensitive. |
| `gestion-pacientes` | Búsqueda por nombre con coincidencia parcial | **Cubierto** | `PacienteGestionIT.java:293` | Valida 200 y coincidencia parcial LIKE. |
| `gestion-pacientes` | Búsqueda por DNI exacto | **Cubierto** | `PacienteGestionIT.java:305` | Valida 200 y coincidencia exacta por DNI. |
| `gestion-pacientes` | Búsqueda por email sin distinguir mayúsculas | **Cubierto** | `PacienteGestionIT.java:318` | Valida 200 y coincidencia case-insensitive por email. |
| `gestion-pacientes` | Los filtros se combinan | **Cubierto** | `PacienteGestionIT.java:330` | Valida 200 y filtrado AND de múltiples campos. |
| `gestion-pacientes` | Una búsqueda sin filtros devuelve las fichas activas | **Cubierto** | `PacienteGestionIT.java:346` | Valida 200 y exclusión de fichas inactivas. |
| `gestion-pacientes` | Una búsqueda sin coincidencias devuelve una lista vacía | **Cubierto** | `PacienteGestionIT.java:361` | Valida 200 y lista vacía `[]`. |
| `gestion-pacientes` | Un paciente inactivo no aparece en la búsqueda | **Cubierto** | `PacienteGestionIT.java:370` | Valida que ficha dada de baja no aparezca en búsquedas. |
| `gestion-pacientes` | La baja deja la ficha inactiva y consultable | **Cubierto** | `PacienteGestionIT.java:386` | Valida 204 en DELETE y posterior GET /{id} con `activo=false`. |
| `gestion-pacientes` | Una ficha dada de baja no se puede consultar como activa | **Cubierto** | `PacienteGestionIT.java:400` | Valida que GET /{id} responda `activo=false`. |
| `gestion-pacientes` | Dar de baja una ficha inexistente devuelve 404 | **Cubierto** | `PacienteGestionIT.java:412` | Valida 404 Problem Details al dar de baja ID inexistente. |
| `gestion-pacientes` | Recepción crea, edita y busca fichas | **Cubierto** | `PacienteGestionIT.java:425` | Rol RECEPCION opera exitosamente POST, PUT y GET. |
| `gestion-pacientes` | Recepción no puede dar de baja una ficha | **Cubierto** | `PacienteGestionIT.java:459` | DELETE con rol RECEPCION devuelve 403 Problem Details y ficha sigue activa. |
| `gestion-pacientes` | Administrador da de baja una ficha | **Cubierto** | `PacienteGestionIT.java:473` | DELETE con rol ADMIN devuelve 204 y marca `activo=false`. |
| `gestion-pacientes` | El odontólogo no accede a las fichas | **Cubierto** | `PacienteGestionIT.java:486` | Rol ODONTOLOGO recibe 403 en GET, GET /{id} y POST. |
| `gestion-pacientes` | Una petición anónima recibe 401 | **Cubierto** | `PacienteGestionIT.java:513` | Petición anónima recibe 401 Problem Details. |
| `gestion-pacientes` | La consulta por identificador devuelve la ficha | **Cubierto** | `PacienteConsultaIT.java:27` | Puerto de lectura devuelve paciente activo por ID. |
| `gestion-pacientes` | La consulta por email ignora mayúsculas | **Cubierto** | `PacienteConsultaIT.java:44` | Puerto de lectura busca por email en mayúsculas/minúsculas. |
| `gestion-pacientes` | La consulta por identificador devuelve también una ficha inactiva | **Cubierto** | `PacienteConsultaIT.java:60` | Puerto de lectura devuelve paciente con `activo=false`. |
| `gestion-pacientes` | La consulta de una ficha inexistente devuelve vacío | **Cubierto** | `PacienteConsultaIT.java:76` | Puerto de lectura devuelve `Optional.empty()` por ID o email inexistente. |
| `gestion-pacientes` | Un DNI duplicado no expone los datos del otro paciente | **Cubierto** | `PacienteGestionIT.java:525` | 409 RN-32 no filtra nombre, apellido, email ni teléfono del paciente existente. |

---

## Ejecución de tests

### Backend (Maven + Testcontainers PostgreSQL 16)
- **Comando:** `cd backend; .\mvnw.cmd test-compile surefire:test failsafe:integration-test failsafe:verify`
- **Surefire (Unitarios + Slice Tests):** 32 ejecutados, 32 pasados, 0 fallidos, 0 omitidos.
  - `PerfilDevTest`: 3 pasados.
  - `PacienteTest`: 5 pasados.
  - `SecurityConfigTest`: 8 pasados.
  - `ReglaNegocioExceptionTest`: 3 pasados.
  - `ConfiguracionRelojTest`: 3 pasados.
  - `ManejadorErroresTest`: 9 pasados.
  - `ArquitecturaTest`: 1 pasado.
- **Failsafe (Tests de Integración con PostgreSQL real):** 43 ejecutados, 43 pasados, 0 fallidos, 0 omitidos.
  - `ContextoAplicacionIT`: 1 pasado.
  - `ExtensionBtreeGistIT`: 1 pasado.
  - `ScriptRestriccionesIT`: 1 pasado.
  - `ConfiguracionRelojIT`: 1 pasado.
  - `PacienteRepositorioIT`: 1 pasado (garantía de constraint única en base).
  - `PacienteServicioIT`: 4 pasados.
  - `PacienteConsultaIT`: 4 pasados.
  - `PacienteGestionIT`: 28 pasados (todos los escenarios de la spec).
  - `RelojFijoIT`: 1 pasado.
- **Resultado:** **BUILD SUCCESS** (75 tests totales en verde).

### Frontend (Vitest)
- **Comando:** `cd frontend; npm test` (`vitest run`)
- **Resultado:** 6 archivos de test pasados, 8 tests pasados, 0 fallidos.

---

## Reglas de negocio y reglas duras

- **RN-32 (Ficha básica y medio de contacto):** Validada en dominio (`Paciente.java`), DTO (`PacienteRequest.java`) y base de datos (`uk_paciente_dni`). Cobertura completa en unitarios e IT.
- **RN-35 (Privacidad en colisión de DNI):** Probada específicamente en `PacienteGestionIT:525`. El mensaje de error no expone datos personales de la ficha preexistente.
- **RN-20 / Permisos RBAC:** El rol `ODONTOLOGO` tiene acceso denegado (403) a lectura y escritura de fichas. `RECEPCION` puede crear, editar y consultar pero no dar de baja (403). `ADMIN` puede dar de baja (204). Peticiones anónimas reciben 401.
- **Baja lógica:** No hay borrado físico; `DELETE` establece `activo = false` y la ficha queda fuera de las búsquedas pero consultable por ID. El DNI permanece ocupado por la constraint única.
- **Reglas duras de datos y seguridad:**
  - Sin datos reales: todos los datos en tests son ficticios (`0000000x`, `@example.test`, etc.).
  - Sin secretos en repo: verificado.
  - Sin JWT / Sesión de servidor: verificado.
  - Sin H2: PostgreSQL 16 real en Testcontainers.
  - Sin cambios fuera del módulo: `git status --porcelain` confirma que no se tocaron `pom.xml`, `SecurityConfig.java`, `schema-constraints.sql`, `application.yml` ni `application-dev.yml`.

---

## Diferencias con design.md

- **Segregación de puertos en aplicación (Menor):** `design.md` D4 planteaba que `PacienteServicio` implementaría directamente `PacienteConsulta`. En la implementación, `PacienteConsultaImpl` implementa el puerto de lectura `PacienteConsulta` y `PacienteServicioImpl` implementa `PacienteServicio`. Esto mantiene una segregación de interfaces limpia y evita ambigüedades de inyección en Spring. No afecta contratos ni especificaciones.
- **Búsqueda parametrizada con cast explícito (Menor):** En `PacienteRepositorioJpa.java`, se utilizó `CAST(:param AS String)` y normalización previa en `PacienteRepositorioAdapter.java` para prevenir que PostgreSQL infiriera tipo `bytea` ante parámetros nulos, mitigando de forma limpia el riesgo técnico advertido en `design.md` §Risks.

---

## Qué NO se pudo verificar

- **Uso HTTP manual desde navegador:** No aplica en este change; el flujo end-to-end de autenticación por cookies y CSRF se completará en C-03.

---

## Próximo paso

El change está completamente verificado y listo para proceder:
1. Consultar confirmación al usuario antes del commit / PR (Regla 16).
2. Abrir PR contra `master` para revisión por parte de Juan Pablo.
3. Tras el merge: `/opsx:archive c-07-pacientes-ficha-basica`.
