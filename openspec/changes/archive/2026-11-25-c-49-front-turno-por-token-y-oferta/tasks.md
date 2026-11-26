# Tasks

## 1. Base común

- [x] 1.1 Con TDD, agregar `<meta name="referrer" content="no-referrer">` en `frontend/index.html` y cubrir el escenario «La SPA declara la política de referencia sin referer».
- [x] 1.2 Crear la pantalla compartida `EnlaceNoDisponible`, sin token ni motivo, y registrar `/turno/:token` y `/oferta/:token` con ámbito público en `features/reserva-publica/routes.ts`.

## 2. Turno por token (`/turno/:token`)

- [x] 2.1 Con TDD, escribir el cliente y la validación de las respuestas de `GET`, `POST confirmar` y `POST cancelar`, y el hook con sus estados (`cargando`, `no-disponible`, `listo`, `error-recuperable`). Cubrir la consulta, el 404 al consultar y el 404 durante una acción.
- [x] 2.2 Con TDD, armar la vista: datos en Buenos Aires, acciones según el estado y ninguna opción de reprogramar. Cubrir la confirmación con un solo POST y el estado confirmado.
- [x] 2.3 Con TDD, agregar la cancelación con paso de confirmación inline, el aviso de cancelación tardía, la opción de desistir sin enviar y el estado cancelado local. Agregar el aviso del 429 con reintento manual.
- [x] 2.4 Con TDD, cubrir «El token no queda en el almacenamiento del navegador»: storage, cookies y título.

## 3. Oferta (`/oferta/:token`)

- [x] 3.1 Con TDD, escribir el cliente y la validación de `GET` y `POST tomar`, y la tabla única de mensajes por estado y por código 409. Cubrir los cuatro estados de la consulta y el 404.
- [x] 3.2 Con TDD, implementar la toma: un solo POST, el turno creado con el enlace relativo `/turno/{token}`, los tres rechazos 409 y el 429 con reintento manual.
- [x] 3.3 Con TDD, cubrir «Los tokens de la oferta y del turno no quedan en el navegador».

## 4. Verificación

- [x] 4.1 Recorrer las dos pantallas con datos ficticios a 390 × 844 y 1440 × 900, con teclado: foco, anuncios, ausencia de desplazamiento horizontal y pantalla genérica. Guardar las capturas en `openspec/changes/c-49-front-turno-por-token-y-oferta/verify/`.
- [x] 4.2 Ejecutar lint, `tsc`, Vitest, build y `openspec validate --strict`. Dejar `verify.md` con el mapeo entre escenarios y tests.

## Workflow follow-up

- Abrir el PR desde `valentino` hacia `master` (lo revisa Juan Pablo), con un commit por etapa previa confirmación.
- Actualizar el estado y el responsable (Valentino) de C-49 en `CHANGES.md` y, de paso, los estados atrasados de C-39 y C-42.
- Archivar con `/opsx:archive` una vez mergeado.
