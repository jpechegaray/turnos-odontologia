# Proposal

## Why

C-01 protege la API pero no permite que el personal inicie sesión. C-03 implementará US-001 y definirá el contrato que necesita C-16, manteniendo los permisos de RN-33 y la política de ingreso de RN-34.

## What Changes

- Incorporar usuarios internos con email único normalizado, contraseña hasheada con BCrypt, estado activo y rol `ODONTOLOGO`, `RECEPCION` o `ADMIN`.
- Incorporar login, consulta de identidad y logout con sesión de servidor, sin JWT. Rechazar intentos fallidos con una respuesta genérica, sin revelar si la cuenta existe, está inactiva o bloqueada.
- Aplicar el bloqueo temporal previsto en RN-34: cinco fallos consecutivos, quince minutos, sujeto a aprobación de Juan Pablo.
- Dar a la SPA una forma de obtener el token CSRF antes del login y renovarlo cuando cambie la sesión. Restringir CORS al origen configurado y conservar la excepción de `/api/public/**`.
- Inicializar un administrador únicamente en desarrollo con `SEED_ADMIN_PASSWORD`, sin exponer credenciales.
- Fuera de alcance: ABM y vínculo con el profesional (C-13), interfaz y guards del frontend (C-16), límite de tasa de endpoints públicos (C-17) y tokens de pacientes (C-32/C-39).

## Capabilities

### New Capabilities

- Ninguna.

### Modified Capabilities

- `control-acceso`: agrega autenticación y ciclo de sesión, bloqueo temporal, contrato CSRF/CORS y alta inicial de administrador de desarrollo. Ajusta las excepciones de acceso anónimo para permitir login y obtención de CSRF sin abrir otras rutas.

## Impact

- Afecta `backend/` en el módulo `seguridad`, su configuración y sus pruebas. Reutiliza el `Clock`, las respuestas Problem Details, Maven y PostgreSQL/Testcontainers de C-01.
- Contrato propuesto: `GET /api/auth/csrf`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`. C-03 desbloquea C-13, C-16, C-19 y C-33.
- Gobernanza **CRITICAL**: solo planificación hasta contar con aprobación explícita de Juan Pablo antes de escribir código.
