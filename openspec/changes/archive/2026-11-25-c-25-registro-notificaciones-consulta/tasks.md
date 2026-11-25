# Tasks

Aplicar TDD estricto por tarea. Usar solo datos ficticios (`@correo.test`) y, cuando el test cubra la spec, poner el nombre exacto del escenario en `@DisplayName`.

## 1. Tipo de página compartido

- [x] 1.1 Crear el record `Pagina<T>` en `ar.edu.utn.turnos.shared.paginacion`, con `contenido`, `pagina`, `tamanio` y `totalElementos`, más `totalPaginas()` calculado y `map(Function)` (design D1). Se verifica con un `PaginaTest` unitario sin Spring:
  - `totalPaginas()` da 0 con 0 elementos, 1 con 20 elementos y tamaño 20, y 2 con 25 elementos y tamaño 20;
  - `map` transforma el contenido y conserva página, tamaño y total;
  - el contenido es inmutable.
  `ArquitecturaTest` sigue en verde.

## 2. Búsqueda paginada en el puerto y el adaptador

- [x] 2.1 Sumar `buscar(estado, turnoId, pagina, tamanio)` a `NotificacionRepositorio` e implementarlo en `NotificacionRepositorioAdapter` con JPQL, filtros nulos opcionales, `order by n.id desc` y `PageRequest` convertido a `Pagina` (design D3 y D4). Se verifica en `NotificacionRepositorioIT` con PostgreSQL real:
  - sin filtros devuelve todas, de la más reciente a la más antigua;
  - el filtro por estado, el filtro por turno y los dos combinados devuelven solo las que corresponden;
  - los totales reflejan los filtros;
  - una página posterior a la última viene vacía y conserva el total.

## 3. Consulta, endpoint y permisos

- [x] 3.1 Crear `ConsultaNotificaciones.listar(...)` con `@Transactional(readOnly = true)` y `@PreAuthorize` para `ADMIN` y `RECEPCION`. Crear también `RespuestaNotificacion` con sus 13 campos y `NotificacionController` en `GET /api/notificaciones`, que recibe `estado`, `turnoId`, `pagina` (por defecto 0, `@Min(0)`) y `tamanio` (por defecto 20, `@Min(1)` y `@Max(100)`), validados por Spring MVC sin `@Validated` en la clase (design D2, D5 y D6). Se verifica en `ConsultaNotificacionesApiIT` con estos escenarios, cada uno con su `@DisplayName` exacto:
  - «Recepción lista el registro de notificaciones» (`ADMIN` y `RECEPCION`, orden y todos los campos);
  - «Sin notificaciones el registro está vacío»;
  - «Una notificación fallida muestra su último error»;
  - «Una notificación enviada no trae error».
- [x] 3.2 Cubrir filtros y paginación en `ConsultaNotificacionesApiIT`. Se verifica con estos escenarios:
  - «Filtrar por estado devuelve solo las de ese estado»;
  - «Filtrar por turno devuelve solo las de ese turno»;
  - «Los filtros de estado y turno se combinan»;
  - «Un turno sin notificaciones devuelve una página vacía»;
  - «Un estado desconocido responde 400» (Problem Details);
  - «Sin parámetros se devuelve la primera página de 20»;
  - «La segunda página trae el resto en el mismo orden»;
  - «Una página posterior a la última viene vacía»;
  - «Parámetros de paginación fuera de rango responden 400» (parametrizado con `pagina=-1`, `tamanio=0` y `tamanio=101`).
- [x] 3.3 Agregar al final de `SecurityConfig` la regla `GET /api/notificaciones` para `ADMIN` y `RECEPCION`, con un comentario. Se verifica en `ConsultaNotificacionesApiIT` con estos escenarios, comprobando además que el cuerpo no trae `contenido`:
  - «El odontólogo no puede ver el registro» (`403`);
  - «Sin sesión el registro responde 401».

## 4. Verificación integral

- [x] 4.1 Correr `.\mvnw.cmd verify` en `backend/` con Docker encendido. Se verifica que los tests unitarios y de integración terminen en verde, incluidos los de C-15 (`NotificacionRepositorioIT`, `PlanificadorNotificacionesIT`), y que `openspec validate c-25-registro-notificaciones-consulta --strict` pase.

## Workflow follow-up

- Correr `openspec-verify` y dejar `verify.md` con el veredicto.
- Commit, push y PR a `master`, que revisa Juan Pablo. El agente pregunta antes de cada commit; el push y el PR los hace Valentino.
- Una vez mergeado, archivar el change sincronizando la spec `registro-notificaciones` y marcar C-25 como hecho en `CHANGES.md`.
