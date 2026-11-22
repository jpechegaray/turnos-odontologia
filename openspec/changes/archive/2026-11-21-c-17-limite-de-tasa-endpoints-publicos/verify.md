# Verify — c-17-limite-de-tasa-endpoints-publicos (2026-11-21)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes)

## Validación estructural

- `openspec.cmd validate "c-17-limite-de-tasa-endpoints-publicos" --strict`: válido (`Change 'c-17-limite-de-tasa-endpoints-publicos' is valid`).
- Artefactos del change completos (`4/4`: `proposal.md`, `specs/limite-tasa-publico/spec.md`, `design.md`, `tasks.md`).

## Tareas

- `6/6` tareas completas (`[x]`) en `openspec/changes/c-17-limite-de-tasa-endpoints-publicos/tasks.md`.
- Pendientes: `0`.

## Matriz escenario → test

Todos los escenarios pertenecen a `openspec/changes/c-17-limite-de-tasa-endpoints-publicos/specs/limite-tasa-publico/spec.md`. Los tests están en `backend/src/test/java/ar/edu/utn/turnos/shared/ratelimit/`.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| `limite-tasa-publico` | Las solicitudes públicas dentro del umbral se permiten | Cubierto | `FiltroLimiteTasaPublicoTest.java:102` | Verifica 2 solicitudes dentro del umbral desde la misma IP hacia `/api/public/disponibilidad-prueba` con estado `200 OK` y cuerpo esperado. |
| `limite-tasa-publico` | Superar el umbral por IP en una ruta pública devuelve 429 con Retry-After | Cubierto | `FiltroLimiteTasaPublicoTest.java:120` | Verifica estado `429`, `application/problem+json`, `status: 429`, `title: "Too Many Requests"`, `codigo: "RN-27"`, cabecera `Retry-After: 30` y que el controlador no se invoca en la solicitud rechazada. |
| `limite-tasa-publico` | El bloqueo de una IP no afecta a otra IP distinta | Cubierto | `FiltroLimiteTasaPublicoTest.java:157` | Mientras la primera IP recibe `429`, una segunda IP en el mismo instante recibe `200 OK`. |
| `limite-tasa-publico` | Las rutas fuera de la zona pública no son limitadas por el filtro público | Cubierto | `FiltroLimiteTasaPublicoTest.java:183` | Una IP bloqueada con `429` en `/api/public/**` invoca `/api/turnos/privada-prueba` y recibe `200 OK`. |
| `limite-tasa-publico` | Al reiniciar la ventana la misma IP vuelve a recibir respuestas normales | Cubierto | `LimitadorTasaEnMemoriaTest.java:22`, `FiltroLimiteTasaPublicoTest.java:146` | Prueba el agotamiento del cupo, el redondeo de `Retry-After` a `1` segundo cuando restan `500 ms` y el reinicio de ventana al avanzar el `Clock` hasta `T + D`. |
| `limite-tasa-publico` | Los contadores de ventanas vencidas se limpian de la memoria | Cubierto | `LimitadorTasaEnMemoriaTest.java:46` | Verifica con `Clock` mutable que las entradas cuya ventana venció se eliminan del mapa en memoria al evaluar tras el vencimiento. |
| `limite-tasa-publico` | El limitador por clave bloquea al superar el tope para una misma clave normalizada | Cubierto | `LimitadorPorClaveTest.java:16` | Verifica normalización (`trim` + minúsculas), rechazo al superar el tope con `codigo: "RN-27"` y `segundosReintento`, y que ni `getMessage()` ni `toString()` contienen el email o datos personales (RN-35). |
| `limite-tasa-publico` | Dos claves distintas tienen cupos independientes y se reinician al vencer su ventana | Cubierto | `LimitadorPorClaveTest.java:47` | Verifica independencia entre dos emails ficticios distintos y reinicio del cupo al avanzar el `Clock`. |

Pruebas adicionales de soporte:
- Concurrencia atómica (20 hilos concurrentes sobre la misma clave): `LimitadorTasaEnMemoriaTest.java:66`.
- Propiedades por defecto e integración con el contexto Spring Boot (`app.ratelimit` en `application.yml`): `LimitadorPorClaveTest.java:63` y `RateLimitConfigIT.java:24`.

Resultado: **8/8 escenarios cubiertos, 0 parciales, 0 sin test**.

## Ejecución de tests

- **Backend (`mvnw.cmd verify` desde `backend/`)**:
  - Surefire (unitarios, web y arquitectura `ArquitecturaTest`): **46 tests ejecutados, 46 pasados, 0 fallidos, 0 saltados**.
  - Failsafe (integración con PostgreSQL 16 real vía Testcontainers, incluyendo `RateLimitConfigIT`): **27 tests ejecutados, 27 pasados, 0 fallidos, 0 saltados**.
  - Total backend: **73/73 en verde** (`BUILD SUCCESS`) + reporte JaCoCo generado (`28` clases analizadas).
- **Frontend**: no aplica (`C-17` es exclusivamente de backend y no toca `frontend/`).

## Reglas de negocio y reglas duras

- **RN-27 (Límites de la reserva online — rate limit por IP y tope por clave) y US-015 / SU-11**: cubiertos por `FiltroLimiteTasaPublico`, `LimitadorPorClave`, `LimitadorTasaEnMemoria` y `RateLimitProperties`, con código de regla `RN-27`, estado `429` Problem Details y cabecera `Retry-After`.
- **RN-35 y Regla dura #1 (Datos ficticios y sin PII en errores ni logs)**: los tests usan exclusivamente emails ficticios (`ana.ficticia@consultorio.test`, `bruno.inventado@consultorio.test`) e IPs de documentación RFC 5737 (`198.51.100.x`, `203.0.113.x`). `LimiteTasaSuperadoException` y `FiltroLimiteTasaPublico` no incluyen la IP ni el email en el cuerpo Problem Details ni en logs.
- **Regla dura #2 (Sin secretos)**: `git diff` sobre `application.yml` solo descomenta `app.ratelimit` con umbrales numéricos (`60`, `1m`, `10`, `1m`).
- **Regla dura #6 (Uso de `Clock` inyectable, DD-05)**: `LimitadorTasaEnMemoria` recibe `Clock` por constructor y no invoca `Instant.now()`.
- **Regla dura #10 (PostgreSQL real con Testcontainers)**: `RateLimitConfigIT` y toda la suite `*IT` corrieron contra `postgres:16-alpine` en Docker.
- **Regla dura #11 (`@DisplayName` exacto)**: los 8 escenarios de `specs/limite-tasa-publico/spec.md` tienen su `@DisplayName` idéntico en los tests JUnit 5.
- **Restricciones del change (RM-02, RM-07, RM-08)**: `git status` y `git diff` confirman que `SecurityConfig.java`, `ManejadorErrores.java` y `pom.xml` no fueron modificados, y que en `application.yml` solo se tocó el bloque `app.ratelimit`.

## Diferencias con design.md

- **Menor:** `LimitadorTasaEnMemoria` inicializa `proximaLimpieza` de forma diferida en la primera llamada a `evaluar(...)` en lugar de llamar a `reloj.instant()` dentro del constructor, para ser compatible con tests de integración existentes (como `AutenticacionPersonalIT` de C-03) que declaran `@MockitoBean Clock` sin stubbing durante el arranque del contexto. El comportamiento de limpieza por ventana guiado por `Clock` es idéntico al especificado.

## Qué NO se pudo verificar

- Integración directa del ayudante `LimitadorPorClave` desde los controladores de reserva pública y token (`C-38`, `C-39`, `C-41`, `C-42`), ya que esos endpoints se implementan en las Olas 5 y 6.

## Próximo paso

1. Avisar al equipo y hacer un **commit separado** para `backend/src/main/resources/application.yml` (mitigación **RM-02**), previa confirmación del usuario (regla dura #16).
2. Hacer el commit con el paquete `ar.edu.utn.turnos.shared.ratelimit`, sus tests y los artefactos de `openspec/changes/c-17-limite-de-tasa-endpoints-publicos/`, abrir el PR hacia `master` para revisión de Juan Pablo y, tras el merge, ejecutar `/opsx:archive c-17-limite-de-tasa-endpoints-publicos`.
