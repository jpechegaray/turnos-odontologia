# Verify — c-49-front-turno-por-token-y-oferta (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (una vez mergeado el PR)

## Validación estructural

`openspec validate c-49-front-turno-por-token-y-oferta --strict` dio: válido.

## Tareas

Hay 11 de 11 tareas completas. La tarea 4.2 se cierra con este informe.

## Matriz escenario → test

Rutas relativas a `frontend/src/features/reserva-publica/`.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| gestion-turno-token-frontend | El enlace válido muestra el turno sin pedir sesión | Cubierto | turno-token/PaginaTurnoToken.test.tsx:64 | Verifica horario en Buenos Aires, profesional, prestación y estado, que no haya opción de reprogramar y que solo se llame a `/api/public/` |
| gestion-turno-token-frontend | Un turno cancelado o iniciado no ofrece acciones | Cubierto | turno-token/PaginaTurnoToken.test.tsx:74 | Prueba `ATENDIDO` y `CANCELADO` |
| gestion-turno-token-frontend | Un token inválido muestra la pantalla genérica sin pistas | Cubierto | turno-token/PaginaTurnoToken.test.tsx:88 | La pantalla no muestra el token ni el motivo |
| gestion-turno-token-frontend | Un token que vence durante la gestión muestra la pantalla genérica | Cubierto | turno-token/PaginaTurnoToken.test.tsx:96 | Prueba el 404 al confirmar y al cancelar |
| gestion-turno-token-frontend | Confirmar pasa el turno a confirmado | Cubierto | turno-token/PaginaTurnoToken.test.tsx:110 | |
| gestion-turno-token-frontend | Un segundo clic en confirmar no repite el pedido | Cubierto | turno-token/PaginaTurnoToken.test.tsx:118 | Usa una promesa pendiente y espera 1 POST |
| gestion-turno-token-frontend | Cancelar a tiempo pide confirmación y cancela el turno | Cubierto | turno-token/PaginaTurnoToken.test.tsx:130 | Hace 1 POST y no vuelve a consultar después del 204 |
| gestion-turno-token-frontend | Una cancelación tardía avisa antes de confirmar | Cubierto | turno-token/PaginaTurnoToken.test.tsx:145 | |
| gestion-turno-token-frontend | Desistir de la cancelación no envía el pedido | Cubierto | turno-token/PaginaTurnoToken.test.tsx:154 | |
| gestion-turno-token-frontend | El límite de solicitudes permite reintentar la acción | Cubierto | turno-token/PaginaTurnoToken.test.tsx:163 | Prueba el 429 al confirmar y al cancelar, con reintento manual |
| gestion-turno-token-frontend | El token no queda en el almacenamiento del navegador | Cubierto | turno-token/PaginaTurnoToken.test.tsx:182 | Revisa storage, cookies, título y DOM |
| gestion-turno-token-frontend | La SPA declara la política de referencia sin referer | Cubierto | politicaReferencia.test.ts:8 | Se confirmó además en el navegador (`meta[name=referrer]` = `no-referrer`) |
| oferta-hueco-frontend | Una oferta disponible muestra el hueco y permite tomarlo | Cubierto | oferta/PaginaOferta.test.tsx:67 | Sin campos de texto |
| oferta-hueco-frontend | Una oferta tomada informa que el hueco ya fue tomado | Cubierto | oferta/PaginaOferta.test.tsx:76 | |
| oferta-hueco-frontend | Una oferta vencida informa que el horario ya empezó | Cubierto | oferta/PaginaOferta.test.tsx:83 | |
| oferta-hueco-frontend | Una oferta no disponible lo informa sin ofrecer tomarla | Cubierto | oferta/PaginaOferta.test.tsx:90 | |
| oferta-hueco-frontend | Un token de oferta inválido muestra la pantalla genérica | Cubierto | oferta/PaginaOferta.test.tsx:97 | |
| oferta-hueco-frontend | Tomar el hueco muestra el turno y su enlace | Cubierto | oferta/PaginaOferta.test.tsx:105 | El enlace es `href="/turno/{token}"` |
| oferta-hueco-frontend | Un segundo clic en tomar no repite el pedido | Cubierto | oferta/PaginaOferta.test.tsx:114 | |
| oferta-hueco-frontend | Llegar tarde a la toma informa que el hueco ya fue tomado | Cubierto | oferta/PaginaOferta.test.tsx:126 | |
| oferta-hueco-frontend | Tomar un hueco que ya empezó informa que venció | Cubierto | oferta/PaginaOferta.test.tsx:134 | |
| oferta-hueco-frontend | Otro rechazo de la toma informa que la oferta no está disponible | Cubierto | oferta/PaginaOferta.test.tsx:142 | Prueba `OFERTA_NO_DISPONIBLE` y un código desconocido |
| oferta-hueco-frontend | El límite de solicitudes permite reintentar la toma | Cubierto | oferta/PaginaOferta.test.tsx:153 | |
| oferta-hueco-frontend | Los tokens de la oferta y del turno no quedan en el navegador | Cubierto | oferta/PaginaOferta.test.tsx:164 | |

Los 24 escenarios quedaron cubiertos. En la primera pasada, cuatro escenarios tenían cobertura parcial porque probaban una sola rama del WHEN: el 404 y el 429 al cancelar, el estado `CANCELADO` y un código 409 desconocido. Se completaron los tests antes de emitir este veredicto.

## Ejecución de tests

- `npx vitest run` (en `frontend/`): 45 archivos y 408 tests pasados, 0 fallidos y 0 saltados.
- `npm run lint`: sin errores ni advertencias.
- `npx tsc -b` y `npm run build`: OK.
- Backend (`mvn verify`): no se ejecutó porque el change no modifica `backend/` (`git diff master -- backend` está vacío).

## Reglas de negocio y reglas duras

- RN-07 y RN-25 (alcance y vigencia del token): la SPA solo usa el token para armar la ruta de su propio recurso. Ante un 404 muestra la pantalla genérica sin distinguir el motivo y no ofrece reprogramar. El token no se persiste ni aparece en el título o en el DOM. La vigencia la controla el backend (C-39), que ya está probado.
- RN-38 (token de oferta): mismo tratamiento. El token del turno creado solo aparece en el `href` relativo.
- RN-15 y RN-24 (primero que confirma): el 409 `HUECO_YA_TOMADO` se informa y deja de ofrecer la toma.
- Datos ficticios: los tests y las capturas usan «Ana Ficticia» y tokens inventados. No hay datos reales.
- Secretos: no se agregaron credenciales.
- Reglas 12 y 13 (front): TypeScript estricto sin `any`, componentes en PascalCase y CSS solo con variables de `design-system/tokens.css`.

## Diferencias con design.md

- Menor: el design describía un hook por pantalla. Se implementó la máquina de estados dentro de cada página, con funciones de API separadas (`turnoTokenApi.ts` y `ofertaApi.ts`). Los estados y el comportamiento coinciden.
- Menor: el estado `error-recuperable` se dividió en dos. Uno es el error de carga, con botón «Volver a intentar». El otro es el aviso de acción, que conserva la vista.
- Ninguna de estas diferencias es mayor ni bloqueante.

## Verificación visual

La política del design system es `system-only`, así que no hay mockups. Se hizo un recorrido en el navegador con la API simulada a 390 × 844 y 1440 × 900, también con teclado. El foco pasa al paso de confirmación, al resultado y al título del turno tomado. No hay desplazamiento horizontal. Las capturas están en `verify/`.

## Qué NO se pudo verificar

- Integración contra el backend real: el recorrido usó respuestas simuladas con la forma de los DTO de C-39 y C-42. Queda para C-51 o para una prueba manual con el backend levantado.
- Que un lector de pantalla real anuncie los mensajes: se verificó la semántica (`role="status"`, `role="alert"` y el foco), pero no se probó con NVDA ni con VoiceOver.

## Próximo paso

Commit en `valentino`, PR a `master` (lo revisa Juan Pablo) y, una vez mergeado, `/opsx:archive c-49-front-turno-por-token-y-oferta`.
