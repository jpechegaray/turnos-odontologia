# Verify — c-09-frontend-base-ui-y-cliente-api (2026-11-21)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes; archivar después del merge del PR)

## Validación estructural

`openspec validate c-09-frontend-base-ui-y-cliente-api --strict` → `Change 'c-09-frontend-base-ui-y-cliente-api' is valid`.

## Tareas

9/9 completas. Ninguna pendiente.

## Matriz escenario → test

Rutas relativas a `frontend/src/`. Cada `it(...)` usa el nombre exacto del escenario. Se leyó cada test y se confirmó que afirma el THEN.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| cliente-api-frontend | Una lectura usa la base de la API y envía la cookie de sesión | Cubierto | shared/api/clienteApi.test.ts:35 | URL `/api/prestaciones`, `credentials: same-origin`, `Accept` JSON y cuerpo interpretado |
| cliente-api-frontend | Una respuesta 204 no devuelve cuerpo | Cubierto | shared/api/clienteApi.test.ts:48 | |
| cliente-api-frontend | Una escritura envía la cabecera con el token vigente | Cubierto | shared/api/clienteApi.test.ts:57 | Además, test de rotación del token entre escrituras |
| cliente-api-frontend | Sin cookie CSRF el cliente obtiene el token antes de escribir | Cubierto | shared/api/clienteApi.test.ts:82 | Orden de las dos llamadas comprobado. Además: dos escrituras simultáneas piden el token una sola vez |
| cliente-api-frontend | Una lectura no envía la cabecera CSRF | Cubierto | shared/api/clienteApi.test.ts:114 | |
| cliente-api-frontend | Un 409 se traduce a un error de conflicto con su código y recurso | Cubierto | shared/api/errorApi.test.ts:12 | |
| cliente-api-frontend | Un 422 se traduce a un error no procesable con el código de la regla | Cubierto | shared/api/errorApi.test.ts:25 | |
| cliente-api-frontend | Los errores 404 y 403 se traducen a sus tipos | Cubierto | shared/api/errorApi.test.ts:31 | |
| cliente-api-frontend | Un 400 de validación conserva los campos inválidos | Cubierto | shared/api/errorApi.test.ts:36 | |
| cliente-api-frontend | Un 401 se traduce a no autenticado | Cubierto | shared/api/errorApi.test.ts:45 | |
| cliente-api-frontend | Una respuesta de error sin Problem Details igual se tipa por su estado | Cubierto | shared/api/errorApi.test.ts:49 | El mensaje no incluye el HTML recibido |
| cliente-api-frontend | Un fallo de red se informa como error de red | Cubierto | shared/api/errorApi.test.ts:63; shared/api/clienteApi.test.ts (fetch rechazado) | |
| fechas-frontend | Un instante UTC se muestra en hora de Buenos Aires | Cubierto | shared/fecha/zonaHoraria.test.ts:24 | Corre con `TZ` del sistema en UTC, Asia/Tokyo y America/Los_Angeles |
| fechas-frontend | Un turno de noche se muestra en el día local anterior al día UTC | Cubierto | shared/fecha/zonaHoraria.test.ts:31 | Ídem |
| fechas-frontend | La medianoche local se muestra como 00:00 del día local | Cubierto | shared/fecha/zonaHoraria.test.ts:38 | Ídem |
| fechas-frontend | Una hora local se convierte al instante UTC | Cubierto | shared/fecha/zonaHoraria.test.ts:53 | Ídem |
| fechas-frontend | Una hora local cercana a la medianoche cae en el día UTC siguiente | Cubierto | shared/fecha/zonaHoraria.test.ts:59 | Ídem |
| fechas-frontend | Una hora local mal formada se rechaza | Cubierto | shared/fecha/zonaHoraria.test.ts:65 | También fecha inexistente (30/02) y formatos inválidos |
| fechas-frontend | Intervalos contiguos no se solapan | Cubierto | shared/fecha/intervalo.test.ts:8 | En ambos órdenes |
| fechas-frontend | Intervalos que comparten un minuto se solapan | Cubierto | shared/fecha/intervalo.test.ts:15 | |
| fechas-frontend | Un intervalo contenido en otro se solapa | Cubierto | shared/fecha/intervalo.test.ts:22 | En ambos órdenes; además, el mismo intervalo exacto |
| fechas-frontend | El fin de un intervalo no está contenido en él | Cubierto | shared/fecha/intervalo.test.ts:34 | |
| fechas-frontend | Un intervalo vacío o invertido se rechaza | Cubierto | shared/fecha/intervalo.test.ts:41 | |
| kit-ui | El kit UI no usa colores fuera de tokens.css | Cubierto | shared/ui/estilosKitUi.test.ts | Recorre `shared/ui/*.css` y `app/*.css`, con triangulación del detector |
| kit-ui | El botón ejecuta su acción y respeta la variante | Cubierto | shared/ui/kitUi.test.tsx:7 | |
| kit-ui | Un botón deshabilitado no ejecuta su acción | Cubierto | shared/ui/kitUi.test.tsx:19 | |
| kit-ui | El campo asocia la etiqueta y la ayuda al control | Cubierto | shared/ui/kitUi.test.tsx:37 | |
| kit-ui | Un campo con error queda marcado como inválido | Cubierto | shared/ui/kitUi.test.tsx:45 | |
| kit-ui | Cada estado de turno se muestra con su texto y su estilo | Cubierto | shared/ui/kitUi.test.tsx:66 | Los 7 estados, con ícono `aria-hidden` |
| kit-ui | El estado cancelado se muestra tachado | Cubierto | shared/ui/kitUi.test.tsx:77; shared/ui/estilosKitUi.test.ts | jsdom no calcula estilos: se comprueba la clase en el componente y la regla `line-through` en `ui.css` |
| kit-ui | La tabla muestra título, encabezados y filas | Cubierto | shared/ui/kitUi.test.tsx:91 | |
| kit-ui | Una tabla sin filas muestra el mensaje de vacío | Cubierto | shared/ui/kitUi.test.tsx:105 | |
| kit-ui | El toast se anuncia y ejecuta su acción | Cubierto | shared/ui/kitUi.test.tsx:112 | |
| kit-ui | Una alerta de peligro se anuncia de inmediato | Cubierto | shared/ui/kitUi.test.tsx:121 | Además: la alerta informativa no usa `role="alert"` |
| kit-ui | La tarjeta muestra su título y contenido | Cubierto | shared/ui/kitUi.test.tsx:135 | |
| rutas-por-feature | Un routes.ts de ejemplo aparece en el router sin editar archivos centrales | Cubierto | app/rutas/registro.test.tsx:22 | Fixture `app/rutas/__fixtures__/features/ejemplo/routes.ts` |
| rutas-por-feature | Dos features con la misma ruta producen un error explícito | Cubierto | app/rutas/registro.test.tsx:31 | |
| rutas-por-feature | Un routes.ts de ejemplo aparece en el menú sin editar archivos centrales | Cubierto | app/rutas/registro.test.tsx:46 | |
| rutas-por-feature | El menú respeta el orden declarado | Cubierto | app/rutas/registro.test.tsx:53 | Incluye el desempate por etiqueta |
| rutas-por-feature | Una ruta sin entrada de menú no aparece en la navegación | Cubierto | app/rutas/registro.test.tsx:62 | |
| rutas-por-feature | El layout muestra la marca, la navegación y la pantalla actual | Cubierto | app/Layout.test.tsx:17 | Usa el glob real de `features/*` (feature `inicio`) |
| rutas-por-feature | Una ruta desconocida muestra página no encontrada | Cubierto | app/Layout.test.tsx:29 | También navega con el enlace de vuelta |

42/42 escenarios cubiertos.

## Ejecución de tests

- `npm run lint` en `frontend/` → sin errores ni advertencias (exit 0).
- `npm test` (Vitest 1.6, jsdom) → 14 archivos, **64 tests**, 0 fallidos. Incluye los 8 de C-02, con `App.test.tsx` sin cambios.
- `npm run build` (`tsc -b` + `vite build`) → OK.
- Backend (`mvn verify`): **no ejecutado**, porque el change no toca `backend/`.

## Reglas de negocio y reglas duras

| Regla | Cobertura |
|---|---|
| RN-14 (zona única, persistencia UTC, sin desfase fijo) | `zonaHoraria.ts` calcula el desfase con `Intl` para cada fecha. Los tests corren con la zona del sistema en tres zonas distintas |
| RN-02 (borde `[inicio, fin)`) | `intervalo.ts`: contiguos, un minuto, contenido, idéntico y fin excluido |
| RN-33 (el backend es la barrera) | El campo `roles` queda reservado y sin efecto, documentado en `tipos.ts` |
| RN-35 (no exponer datos) | Las respuestas sin Problem Details y los fallos de red muestran mensajes genéricos |

Reglas duras del frontend:
- Regla 12: TypeScript estricto, sin `any` (lint en verde, con la regla en error) y componentes en PascalCase.
- Regla 13: no hay colores, espaciados ni fuentes fijos. No hay hexadecimales ni `style={}` en los `.ts`/`.tsx` nuevos, y el test de estilos recorre las hojas nuevas.
- Regla 1: no hay datos de pacientes (los fixtures son prestaciones y una pantalla de ejemplo).
- Regla 2: no hay secretos.
- Archivos existentes: solo se modificó `frontend/src/App.tsx`, como prevé el design. `package.json`, `vite.config.ts`, `main.tsx`, `design-system/` y el backend no cambiaron.

## Diferencias con design.md

- **Menor:** el design nombraba los colores fijos del toast como "fondo y texto". En `ui.css` también se reemplazaron el color del ícono y el de la acción, que heredan `currentColor`.
- **Menor:** se agregó un test de la regla `line-through` en `ui.css`, porque jsdom no calcula estilos.

No hay diferencias mayores ni bloqueantes.

## Qué NO se pudo verificar

- **Inspección visual en el navegador:** no se levantó `npm run dev` ni se tomaron capturas. La política del proyecto es `system-only`, que solo exige usar tokens, y eso está verificado por test. Conviene una mirada rápida al layout en el PR.
- **Integración real con el backend** (CSRF y cookie de sesión en el navegador): se probó con `fetch` simulado. La prueba de punta a punta llega con el login de C-16.
- El caso de un cambio de regla horaria entre la estimación y el resultado de `aInstanteUtc`: Argentina no tiene horario de verano vigente, así que no es reproducible con datos reales.

## Próximo paso

1. Commit y PR `C-09 frontend-base-ui-y-cliente-api` de `valentino` a `master`, que revisa Juan Pablo.
2. Después del merge: `/opsx:archive c-09-frontend-base-ui-y-cliente-api`.
