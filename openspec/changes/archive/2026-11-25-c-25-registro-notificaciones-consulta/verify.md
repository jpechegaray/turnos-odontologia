# Verify — c-25-registro-notificaciones-consulta (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR

No hay bloqueantes. Los 15 escenarios del spec están cubiertos por tests que terminaron en verde. Juan Pablo puede revisar en el PR tres diferencias menores con `design.md` y dos notas de cobertura.

## Validación estructural

`openspec validate c-25-registro-notificaciones-consulta --strict` → `Change 'c-25-registro-notificaciones-consulta' is valid`.

## Tareas

6/6 completas (1.1, 2.1, 3.1, 3.2, 3.3 y 4.1). No hay pendientes.

## Matriz escenario → test

Spec: `registro-notificaciones`. Todos los tests están en `backend/src/test/java/ar/edu/utn/turnos/shared/notificacion/web/ConsultaNotificacionesApiIT.java`, con el nombre exacto del escenario en `@DisplayName`.

| Escenario | Estado | Test (línea) | Nota |
|---|---|---|---|
| Recepción lista el registro de notificaciones | Cubierto | :102 | Parametrizado con `ADMIN` y `RECEPCION`. Afirma el orden de los tres ids (más reciente primero), `totalElementos` 3 y los 13 campos de un elemento. Los campos propios de `ENVIADA` y `FALLIDA` se afirman en :139 y :153. |
| Sin notificaciones el registro está vacío | Cubierto | :129 | `contenido` vacío, `totalElementos` 0 y `totalPaginas` 0. |
| Una notificación fallida muestra su último error | Cubierto | :139 | `FALLIDA`, `intentos` 2 y `ultimoError` con el mensaje registrado. |
| Una notificación enviada no trae error | Cubierto | :153 | `ultimoError` nulo y `enviadaEn` con la fecha de envío. |
| Filtrar por estado devuelve solo las de ese estado | Cubierto | :167 | Solo las dos `FALLIDA` y `totalElementos` 2. |
| Filtrar por turno devuelve solo las de ese turno | Cubierto | :182 | Incluye en los datos otro turno y una oferta sin turno, que quedan afuera. |
| Los filtros de estado y turno se combinan | Cubierto | :197 | Solo la `FALLIDA` del turno 10. |
| Un turno sin notificaciones devuelve una página vacía | Cubierto | :210 | `200`, `contenido` vacío y total 0. |
| Un estado desconocido responde 400 | Cubierto | :221 | `400` con `application/problem+json`. |
| Sin parámetros se devuelve la primera página de 20 | Cubierto | :231 | 20 elementos, el primero es el más reciente, `pagina` 0, `tamanio` 20, total 25 y 2 páginas. |
| La segunda página trae el resto en el mismo orden | Cubierto | :246 | Afirma que son exactamente los ids de las 5 más antiguas, en orden. Que no se repiten con la primera página se deduce de eso, sin una aserción cruzada. También lo prueba `NotificacionRepositorioIT` (:193). |
| Una página posterior a la última viene vacía | Cubierto | :259 | `pagina` 5, `contenido` vacío, total 3 y 1 página. |
| Parámetros de paginación fuera de rango responden 400 | Cubierto | :273 | Parametrizado con `pagina=-1`, `tamanio=0` y `tamanio=101`, Problem Details. |
| El odontólogo no puede ver el registro | Cubierto | :283 | `403` en Problem Details, sin `contenido`. |
| Sin sesión el registro responde 401 | Cubierto | :294 | `401` en Problem Details, sin `contenido`. |

Tests de apoyo, que no corresponden a ningún escenario:
- `PaginaTest` (6): `totalPaginas`, `map`, inmutabilidad y validación del constructor.
- `NotificacionRepositorioIT` :164, :176 y :193: búsqueda, filtros y paginación contra PostgreSQL real.

## Ejecución de tests

- **Comando:** `.\mvnw.cmd verify` en `backend/`, con Docker encendido (Testcontainers con PostgreSQL 16).
- **Resultado:** `BUILD SUCCESS`, EXIT=0.
  - Unitarios (surefire): 226 corridos, 0 fallidos, 0 con error, 0 saltados.
  - Integración (failsafe): 448 corridos, 0 fallidos, 0 con error, 0 saltados.
- **Clases del change:** `ConsultaNotificacionesApiIT` 18/18, `NotificacionRepositorioIT` 8/8, `PaginaTest` 6/6 y `ArquitecturaTest` 2/2.
- **Clases de C-15 alcanzadas por el cambio del puerto:** `ServicioNotificacionesTest` 9/9, `PlanificadorNotificacionesIT` 3/3, `NotificacionRestriccionesIT` 2/2 y `ConfiguracionNotificacionesIT` 2/2.
- **Vigencia de la corrida:** después de ella no se modificó ningún `.java`; solo cambiaron `tasks.md`, `design.md` y este informe.
- **Frontend:** no ejecutado, porque el change no toca `frontend/`.

## Reglas de negocio y reglas duras

- **RN-30 (registro):** el registro de cada envío ahora se puede consultar, con estado, intentos y último error. Lo cubren los escenarios de consulta y de último error. La idempotencia y los reintentos de RN-30 no cambian y siguen cubiertos por los tests de C-15.
- **US-020:** se cumplen los dos criterios. «Filtrable por estado y turno» está en :167, :182 y :197, y «último error de los fallidos» en :139.
- **RN-01, RN-02, concurrencia y RN-07:** no aplican, porque el change no toca turnos ni tokens.
- **Reglas duras, revisadas sobre el diff sin commitear de la rama `valentino`:**
  - Datos ficticios: solo `@correo.test` y `@example.test`. No hay DNI ni emails reales.
  - Secretos: no hay contraseñas, tokens ni credenciales en el código del change. `.env.example` y `docker-compose.yml` tienen cambios locales previos de Valentino, ajenos a C-25, y **no deben entrar en el commit**.
  - Regla 4: el endpoint es de solo lectura y, además, `ODONTOLOGO` no tiene acceso.
  - Regla 9: se expone el DTO `RespuestaNotificacion`, no la entidad JPA, y la consulta JPQL está parametrizada.
  - Regla 10: los tests de persistencia usan Testcontainers, sin H2.
  - Regla 11: los 15 escenarios tienen su `@DisplayName` exacto.
  - Regla 7: no hay Flyway ni Liquibase, ni cambios de esquema.

## Diferencias con design.md

No hay diferencias bloqueantes ni mayores. Hay tres menores:

1. **D1:** `Pagina<T>` además valida en el constructor la página negativa, el tamaño menor a 1 y el total negativo. `totalPaginas()` lleva `@JsonProperty`, porque Jackson no serializa por sí solo los métodos de un record que no son componentes, y devuelve `long`.
2. **D7:** no lo menciona el design, pero el repositorio en memoria de `ServicioNotificacionesTest` (C-15) implementa el puerto, así que tuvo que sumar `buscar`. Lanza `UnsupportedOperationException`, porque el servicio de envío no consulta el registro.
3. **D2:** el design se corrigió durante el apply. Decía «con `@Validated`», pero así los valores fuera de rango daban 500. Ahora dice «sin `@Validated`» y el código coincide.

## Qué NO se pudo verificar

- No se probó a mano contra el back levantado con la base de dev. Lo cubren los IT con PostgreSQL real.
- No se probó el rendimiento del `count` con un volumen grande. Para el tamaño de un consultorio no hace falta; está anotado en Risks.

## Próximo paso

- Commit de los archivos de C-25, sin `.env.example`, `docker-compose.yml` ni `.claude/launch.json`. Después, push y PR a `master`, que revisa Juan Pablo.
- En el PR, revisar el formato de página nuevo (D1 y D2), que queda como convención para los próximos listados y para C-34.
- Una vez mergeado: `/opsx:archive c-25-registro-notificaciones-consulta`.
