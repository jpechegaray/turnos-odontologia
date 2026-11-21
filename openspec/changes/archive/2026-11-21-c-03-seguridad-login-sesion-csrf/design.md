# Design

## Context

C-01 dejó `SecurityConfig` con autorización a nivel método, respuestas `RespuestasSeguridadProblemDetail`, rutas públicas explícitas y un `UserDetailsService` vacío. `SecurityConfigTest` cubre la base 401/403 y la exención CSRF para rutas públicas. El backend ya tiene un `Clock` inyectable, PostgreSQL/Testcontainers y un bloque reservado para seguridad en `application.yml`.

## Goals / Non-Goals

**Goals:** extender la única cadena de seguridad existente, conservar sus garantías y dar a C-16 un contrato que pueda usar sin depender de detalles internos.

**Non-Goals:** cambiar las dependencias del backend, duplicar la lógica de permisos en la SPA, introducir sesión distribuida o JWT, o implementar la administración y vinculación de usuarios de C-13.

## Decisions

1. **Separar política, persistencia y transporte.** En `seguridad`, mantener las decisiones de bloqueo y la lectura de `Clock` en la capa de dominio/aplicación y dejar JPA, BCrypt y Spring Security detrás de sus adaptadores. La tabla de usuarios impone unicidad sobre el email normalizado; la respuesta usa un DTO, no la entidad. Se descarta usar un almacén en memoria para usuarios porque el contador y las cuentas deben sobrevivir a un reinicio.
2. **Registrar fallos como resultado de negocio.** Serializar la lectura y actualización de una cuenta con bloqueo de fila en una transacción corta; el fallo esperado sale de la transacción como resultado, de modo que el incremento no se revierta al convertirlo a 401. Calcular el umbral y vencimiento con el `Clock`; no cambiar el contador de usuarios inactivos ni extender un bloqueo vigente. Se descarta actualizar sin coordinación porque dos peticiones concurrentes podrían perder incrementos. Usar trabajo BCrypt comparable en los rechazos tempranos para reducir diferencias de tiempo, además del cuerpo público uniforme.
3. **Usar el ciclo de sesión de Spring Security.** Autenticar JSON a través de la cadena de seguridad; en el éxito aplicar la estrategia de sesión (rotación del identificador y limpieza de CSRF) y persistir el contexto explícitamente en el repositorio de sesión. Resolver logout mediante el soporte de Spring Security y emitir `204`, sin redirección; invalidar sesión y limpiar contexto, cookie de sesión y CSRF. No habilitar HTTP Basic ni form login. Usar un DTO de identidad idéntico en login y `me`; responder `Cache-Control: no-store` en los endpoints de autenticación.
4. **Obtener CSRF de forma explícita.** `GET /api/auth/csrf` materializa el token diferido y responde `204` con `XSRF-TOKEN` disponible para la SPA. Usar `CookieCsrfTokenRepository` y tratamiento compatible con el valor de cookie enviado en `X-XSRF-TOKEN`. La SPA solicitará un token antes del login y después de login/logout. Solo la cookie CSRF es legible por JavaScript. Se descarta exceptuar el login de CSRF y se mantiene la exención existente de `/api/public/**`. Diferenciar autorización y CSRF al responder: una escritura protegida anónima recibe 401, mientras que el login público y las escrituras de una sesión válida reciben 403 si falta CSRF válido.
5. **Mantener permisos y CORS en el backend.** No abrir `/api/auth/**` completo: solo permitir los dos métodos y rutas anónimas definidos en la spec. Conservar `@EnableMethodSecurity`, los permisos declarados por servicio y las excepciones de infraestructura de C-01. Configurar CORS en la cadena con un origen exacto, credenciales permitidas y cabecera CSRF explícita; sin comodines ni cambio de topología de la SPA.
6. **Inicializar solo el administrador de desarrollo.** Usar la identidad ficticia `admin@consultorio.test` definida en la KB. Si no hay secreto no crear la cuenta; si ya existe no redefinirla. No usar un valor por defecto en el código ni activar el inicializador fuera de `dev`. C-33 deberá reutilizar esa cuenta cuando esté presente.

## Risks / Trade-offs

- **Pérdida del contador por rollback o simultaneidad** → comprobar con PostgreSQL real el fallo confirmado y el quinto fallo bajo concurrencia.
- **CSRF no actualizado después del cambio de sesión** → cubrir el flujo completo con cookie y cabecera reales; los tests con usuarios simulados quedan como regresión de autorización y no sustituyen ese flujo.
- **Filtración del estado de cuenta por respuestas diferentes** → unificar los rechazos y coordinar con C-16 que el mensaje de UI sea genérico, sin indicar bloqueo por cuenta.
- **Mayor costo por BCrypt y espera por fila bajo concurrencia** → mantener acotada la transacción y no introducir operaciones de red dentro de ella.

## Migration Plan

- Hibernate crea la tabla en desarrollo según DD-08; no incorporar Flyway ni Liquibase. Usar `create-drop` con PostgreSQL real en pruebas.
- Completar el bloque de seguridad y documentar las variables existentes. `APP_COOKIE_SECURE` queda habilitado por defecto y solo se deshabilita explícitamente para HTTP local.
- Si se revierte el despliegue de C-03, se vuelve a la política base de C-01 y no se elimina automáticamente la tabla ni se preservan sesiones activas entre reinicios (SU-10).
