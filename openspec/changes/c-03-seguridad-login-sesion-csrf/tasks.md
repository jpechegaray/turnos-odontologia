# Tasks

## 1. Revisión previa

- [x] 1.1 Registrar la aprobación explícita de Juan Pablo antes de escribir código; verificar que la aprobación cubra los valores 5 fallos / 15 minutos y el contrato de error genérico que consumirá C-16.

Nico confirmó en la conversación que Juan Pablo aprobó la propuesta de C-03 presentada para revisión, que incluía el bloqueo de 5 fallos / 15 minutos y el error genérico para C-16.

## 2. Usuarios y política de ingreso

- [x] 2.1 Modelar usuario, roles y email normalizado único; comprobar con PostgreSQL/Testcontainers que no se admiten duplicados por capitalización y que se guarda el hash BCrypt, nunca la contraseña.
- [x] 2.2 Implementar por TDD la política con `Clock`; cubrir quinto fallo, vencimiento exacto, reinicio por éxito, nueva serie al vencer y que no se extienda un bloqueo vigente.
- [x] 2.3 Integrar la verificación de credenciales y el registro transaccional de fallos; comprobar en PostgreSQL que el fallo persiste, que no se pierden incrementos concurrentes y que una cuenta inactiva no puede ingresar.

## 3. Sesión, autorización y CSRF

- [x] 3.1 Incorporar `GET /api/auth/csrf`, el tratamiento de cookie/cabecera y CORS de origen único; verificar con MockMvc la obtención anónima, los orígenes permitido y rechazado, el rechazo sin CSRF válido y la exención de `/api/public/**`.
- [x] 3.2 Implementar login JSON y `GET /api/auth/me`; verificar login y consulta en la misma sesión, rotación del identificador, respuesta genérica en los cuatro rechazos, `401` Problem Details sin sesión y atributos de la cookie en un servidor de prueba.
- [x] 3.3 Incorporar logout y validar su `204`, rechazo sin CSRF válido, invalidación de la sesión anterior y obtención de un token usable antes del login y después de login/logout. Capturar logs del flujo y comprobar que no se exponen secretos.
- [x] 3.4 Mantener la autorización a nivel método; verificar que `ODONTOLOGO` no puede escribir y que `RECEPCION` sí puede hacerlo en una operación de prueba con CSRF válido. Mantener verdes los escenarios de C-01 y documentar el contrato de sesión/CSRF para C-16.

## 4. Inicialización de desarrollo

- [x] 4.1 Crear la cuenta inicial con `SEED_ADMIN_PASSWORD` únicamente en `dev`; comprobar idempotencia, ausencia de cuenta fuera de `dev` o sin secreto y conservación del hash existente. Documentar las variables y el arranque local sin publicar credenciales.

## 5. Verificación de integración

- [x] 5.1 Ejecutar `mvnw.cmd verify` desde `backend/` con PostgreSQL/Testcontainers; comprobar que pasan la regresión de C-01 y los flujos de C-03. Mapear cada escenario de la spec a su `@DisplayName` exacto para `openspec-verify`.
