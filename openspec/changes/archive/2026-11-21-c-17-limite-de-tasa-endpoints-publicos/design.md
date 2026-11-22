# Design

## Context

C-01 y C-03 establecieron la configuración de seguridad en `SecurityConfig` (donde `/api/public/**` es de acceso anónimo y está exceptuado de CSRF), el reloj inyectable (`Clock` en zona `America/Argentina/Buenos_Aires`), el formato uniforme de errores Problem Details (`ManejadorErrores` y `RespuestasSeguridadProblemDetail`) y el bloque reservado `# app.ratelimit:` en `application.yml` (RM-02). `pom.xml` está congelado desde C-01 (RM-08), por lo que el límite de tasa debe resolverse con una implementación propia en memoria dentro de `ar.edu.utn.turnos.shared.ratelimit`. Ver `proposal.md` para la motivación y `specs/limite-tasa-publico/spec.md` para el contrato de comportamiento.

Decision: no (no UI)

## Goals / Non-Goals

**Goals:**
- Aislar toda la solución en `ar.edu.utn.turnos.shared.ratelimit` sin tocar `SecurityConfig`, `ManejadorErrores` ni `pom.xml` (RM-07, RM-08).
- Proveer una primitiva pura y thread-safe de limitación de tasa por clave con ventana fija controlada por `Clock` y limpieza automática de entradas vencidas.
- Aplicar el filtro por IP únicamente sobre `/api/public/**` mediante `FilterRegistrationBean` y dejar listo un ayudante reutilizable por clave (p. ej., email normalizado) para los changes públicos posteriores (C-38, C-39, C-41, C-42).

**Non-Goals:**
- Rate limiting distribuido (Redis o base de datos): el sistema es monolito single-instance (DD-02, SU-10, SU-13).
- Validación de máximo de turnos futuros activos por email en base de datos (`reserva_online.max_turnos_activos_por_email` de RN-27), que pertenece a la creación de reserva pública en C-41.
- Modificar `EstadoError` o `ManejadorErrores` salvo que sea indispensable: la respuesta HTTP 429 del filtro y del ayudante se resuelve dentro de `shared.ratelimit`.

## Decisions

1. **Algoritmo de ventana fija por clave con `ConcurrentHashMap` y `Clock` inyectable.**
   - *Elección*: Implementar en `shared.ratelimit` un limitador en memoria (`LimitadorTasaEnMemoria` / ayudante de tope por clave) libre de dependencias de Spring/Servlet para que sea 100% testeable en unit tests puros. Cada clave normalizada (`trim().toLowerCase(Locale.ROOT)`) se asocia en un `ConcurrentHashMap<String, VentanaContador>` a un registro inmutable `{Instant inicioVentana, int consumidos}` actualizado atómicamente mediante `compute(...)`.
   - *Evaluación*: Dado `ahora = clock.instant()`, si no existe entrada o `!ahora.isBefore(inicioVentana.plus(duracionVentana))`, se abre una ventana nueva con `consumidos = 1` y se permite. Si `consumidos < maximoSolicitudes`, se incrementa y se permite. Si `consumidos >= maximoSolicitudes`, se rechaza devolviendo un resultado con los segundos restantes `Retry-After = max(1, ceil((finVentana - ahora)))` (calculado en milisegundos con redondeo hacia arriba a segundos enteros para nunca devolver `0` mientras la ventana sigue vigente).
   - *Alternativas descartadas*: Bucket4j o Resilience4j (requieren editar `pom.xml`, vedado por RM-08 y la ficha de C-17); ventana deslizante con lista de timestamps por clave (mayor huella de memoria e innecesaria para un consultorio single-tenant).

2. **Limpieza determinista y acotada de contadores vencidos en memoria.**
   - *Elección*: Para evitar fugas de memoria ante múltiples IPs o claves transitorias sin introducir hilos `@Scheduled` adicionales, el limitador ejecuta una barrida de limpieza (`entrySet().removeIf(e -> !ahora.isBefore(e.getValue().finVentana()))`) controlada por `Clock` cada vez que transcurre un intervalo de limpieza o cuando el mapa supera un umbral de entradas (p. ej., cada N operaciones o cuando el tamaño alcanza un tope configurable/razonable, además de un método explícito o automático disparado en las evaluaciones).
   - *Justificación*: Mantiene el componente determinista en tests unitarios con `Clock` fijo/mutable sin depender de timers en segundo plano.

3. **Ayudante reutilizable de tope por clave (`LimitadorPorClave` / `VerificadorTopePorClave`).**
   - *Elección*: Exponer un componente Spring en `shared.ratelimit` que envuelva la primitiva de ventana fija para claves de negocio (como email en C-38/C-41 o token en C-39/C-42), normalizando la clave (`trim().toLowerCase(Locale.ROOT)`) y permitiendo verificar/consumir cupo. Incluye una excepción o respuesta dedicada que nunca incorpora la clave ni el email en el mensaje de `detail` ni en logs (cumpliendo RN-35).

4. **Registro del filtro HTTP por IP con `FilterRegistrationBean` sin editar `SecurityConfig`.**
   - *Elección*: Implementar un `OncePerRequestFilter` (`FiltroLimiteTasaPublico`) registrado mediante un `@Bean FilterRegistrationBean<FiltroLimiteTasaPublico>` en una clase `@Configuration` de `shared.ratelimit`, con patrón de URL `/api/public/*` y orden explícito anterior o posterior a la cadena pública. Además, el propio filtro verifica que la ruta relativa a la aplicación comience con `/api/public/` como defensa en profundidad.
   - *Extracción de IP*: Se usa `request.getRemoteAddr()`. No se confía ciegamente en `X-Forwarded-For` enviado por el cliente para evitar spoofing de IP desde internet si no hay proxy inverso configurado.
   - *Alternativas descartadas*: `http.addFilterBefore(...)` dentro de `SecurityConfig` (prohibido explícitamente por el alcance de C-17 para evitar conflictos de merge con C-03).

5. **Formato de respuesta `429 Too Many Requests` en Problem Details con `Retry-After`.**
   - *Elección*: Cuando el filtro por IP (o el manejador del limitador por clave) rechaza una solicitud, escribe directamente la respuesta HTTP (siguiendo el mismo patrón de `RespuestasSeguridadProblemDetail` con `ObjectMapper`):
     - Estado: `429 Too Many Requests` (`HttpStatus.TOO_MANY_REQUESTS`).
     - Cabecera: `Retry-After: <segundos>` (entero positivo en segundos hasta el fin de la ventana).
     - Tipo de contenido: `application/problem+json;charset=UTF-8`.
     - Cuerpo `ProblemDetail`: `status: 429`, `title: "Too Many Requests"`, `detail` genérico en español sin IP ni email (p. ej., `"Se superó el límite de solicitudes permitidas. Intente nuevamente más tarde."`), y propiedad `codigo: "RN-27"`.
   - *Justificación*: Mantiene consistencia total con RFC 7807 (`errores-api`) sin necesidad de tocar `ManejadorErrores` ni `EstadoError` de C-01.

6. **Propiedades de configuración en `application.yml` (`@ConfigurationProperties(prefix = "app.ratelimit")`).**
   - *Elección*: Definir un record o clase `@ConfigurationProperties(prefix = "app.ratelimit")` con valores por defecto razonables alineados con SU-11 (por ejemplo: límite por IP de `60` solicitudes por ventana de `1m` / `60s`, y límite por clave de `10` solicitudes por ventana de `1m` / `60s`), sobreescribibles en tests mediante propiedades de Spring.
   - *Mitigación RM-02*: En `backend/src/main/resources/application.yml` solo se descomenta y completa el bloque `app.ratelimit` reservado en la línea 43, en un commit separado y avisando al equipo.

## Risks / Trade-offs

- **[Conflicto de merge en `application.yml` (RM-02)]** → Limitar el cambio en `application.yml` exclusivamente a las líneas del bloque `app.ratelimit` y realizarlo en un commit dedicado.
- **[Contención o crecimiento de memoria bajo ataque de múltiples IPs]** → Usar operaciones atómicas cortas sobre `ConcurrentHashMap` y purgar entradas vencidas durante la evaluación cuando ha transcurrido una ventana o se supera un umbral de claves.
- **[Diferencias entre `FilterRegistrationBean` en MockMvc y contenedor Servlet real]** → En tests con `@WebMvcTest` o `@SpringBootTest` + `@AutoConfigureMockMvc`, asegurar que el filtro registrado por `FilterRegistrationBean` sea recogido por `MockMvc` y verificar tanto rutas `/api/public/**` como rutas fuera de `/api/public/**`.

## Migration Plan

- No hay cambios de esquema en PostgreSQL.
- Al activar `app.ratelimit` en `application.yml`, cualquier ruta bajo `/api/public/**` queda protegida automáticamente al arrancar la aplicación.
- Rollback: revertir el paquete `shared.ratelimit` y volver a comentar el bloque `app.ratelimit` en `application.yml`.
