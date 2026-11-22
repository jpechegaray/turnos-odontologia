# Tasks

## 1. Revisión previa (Gobernanza HIGH)

- [x] 1.1 Registrar la revisión y aprobación explícita de Juan Pablo sobre los artefactos de planificación (`proposal.md`, `specs/limite-tasa-publico/spec.md`, `design.md` y `tasks.md`) antes de escribir código de producción; verificar que la aprobación incluya los umbrales por defecto propuestos en `app.ratelimit` y el commit separado para `application.yml` (RM-02).

Lionel confirmó en la sesión la aprobación para avanzar con la implementación de la propuesta de C-17 (`/openspec-apply-change`), incluyendo los umbrales por defecto en `app.ratelimit` y la separación del commit para `application.yml` (RM-02).

## 2. Limitador en memoria y ayudante reutilizable por clave

- [x] 2.1 Implementar por TDD en `ar.edu.utn.turnos.shared.ratelimit` el limitador en memoria por ventana fija con `Clock` inyectable y limpieza de entradas vencidas; verificar con tests unitarios JUnit 5 + AssertJ (sin Spring) los escenarios `@DisplayName("Al reiniciar la ventana la misma IP vuelve a recibir respuestas normales")` y `@DisplayName("Los contadores de ventanas vencidas se limpian de la memoria")`, incluyendo el cálculo exacto de segundos `Retry-After` y concurrencia básica.
- [x] 2.2 Implementar por TDD el ayudante reutilizable de tope por clave normalizada (`trim` + minúsculas) y las propiedades `@ConfigurationProperties(prefix = "app.ratelimit")`; verificar con tests unitarios los escenarios `@DisplayName("El limitador por clave bloquea al superar el tope para una misma clave normalizada")` y `@DisplayName("Dos claves distintas tienen cupos independientes y se reinician al vencer su ventana")`, comprobando que ningún mensaje de error ni registro de log contenga el email o la clave (RN-35).

## 3. Filtro HTTP por IP para rutas públicas y configuración

- [x] 3.1 Implementar por TDD el filtro HTTP por IP para `/api/public/**` con respuesta `429 Too Many Requests` en formato Problem Details (`application/problem+json`, `codigo: "RN-27"`) y cabecera `Retry-After`, registrado mediante `FilterRegistrationBean` sin editar `SecurityConfig` ni `ManejadorErrores`; verificar con tests de integración web (MockMvc y `Clock` controlado) los escenarios `@DisplayName("Las solicitudes públicas dentro del umbral se permiten")`, `@DisplayName("Superar el umbral por IP en una ruta pública devuelve 429 con Retry-After")`, `@DisplayName("El bloqueo de una IP no afecta a otra IP distinta")` y `@DisplayName("Las rutas fuera de la zona pública no son limitadas por el filtro público")`.
- [x] 3.2 Descomentar y completar únicamente el bloque `app.ratelimit` en `backend/src/main/resources/application.yml` (respetando RM-02 para comitearlo por separado y avisar al equipo); verificar que el contexto de Spring Boot levanta con las propiedades por defecto enlazadas correctamente.

## 4. Verificación integral del change

- [x] 4.1 Ejecutar `mvnw.cmd verify` desde `backend/` y `openspec.cmd validate "c-17-limite-de-tasa-endpoints-publicos" --strict`; verificar que todos los tests unitarios, de arquitectura (`ArquitecturaTest`) y de integración con Testcontainers pasen en verde, que `SecurityConfig` y `pom.xml` no hayan sido modificados (`git diff`) y que todos los escenarios de la spec tengan su test con `@DisplayName` exacto.

## Workflow follow-up

- Avisar al equipo antes de comitear el bloque `app.ratelimit` de `application.yml` en un commit separado (RM-02) y pedir confirmación antes de cada commit (regla dura #16).
- Ejecutar `openspec-verify` para generar `verify.md` antes de abrir el PR hacia `master` para revisión de Juan Pablo.
- Archivar el change con `openspec-archive-change` una vez mergeado el PR.
