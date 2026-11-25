# Tasks

## 1. Preparación de pruebas

- [x] 1.1 Extender `frontend/src/app/sesion.fixture.ts` para que `simularSesionActiva` acepte un rol (por defecto `ADMIN`) y una función que responda las rutas que no son `/api/auth/me`, conservando la firma actual; verificar con `npm test` que los tests existentes siguen en verde.

## 2. Prestaciones — listado

- [x] 2.1 RED: crear `frontend/src/features/prestaciones/Prestaciones.test.tsx` con un test llamado exactamente «El catálogo lista las prestaciones con su duración y estado» que monte la ruta real y espere la tabla con nombre, duración y estado; verificar que falla.
- [x] 2.2 GREEN: implementar `PaginaPrestaciones` con `GET /api/prestaciones?soloActivas=false`, estado de carga y `Tabla` con duración `numerica`; verificar que el test de 2.1 pasa.
- [x] 2.3 TRIANGULATE: agregar los tests «El catálogo vacío muestra un mensaje» y «Un fallo al listar permite reintentar» e implementar el mensaje vacío y la alerta con reintento; verificar los tres tests de listado.
- [x] 2.4 REFACTOR: extraer el estado de carga/error a un bloque reutilizable de la página y limpiar duplicación; verificar `npm test` y `npm run lint` en `frontend/`.

## 3. Prestaciones — validación local de duración y nombre

- [x] 3.1 RED: agregar el test «Una duración que no es múltiplo de 5 bloquea el envío sin llamar a la API», que envía duración 7 y espera el mensaje de RN-09 sin escrituras; verificar que falla.
- [x] 3.2 GREEN: crear el formulario de prestación con `Campo` para nombre y duración, `noValidate` y la validación mínima de múltiplo de 5; verificar que el test de 3.1 pasa.
- [x] 3.3 TRIANGULATE: agregar los tests «Una duración menor a 5 bloquea el envío sin llamar a la API» (0), «Una duración mayor a 480 bloquea el envío sin llamar a la API» (481), «Una duración no entera bloquea el envío sin llamar a la API» (7,5), «Un nombre vacío bloquea el envío sin llamar a la API» y «Las duraciones límite 5 y 480 se aceptan»; verificar los seis escenarios de validación.
- [x] 3.4 REFACTOR: extraer `validarPrestacion` y el mensaje de RN-09 a un módulo de la feature, sin cambiar el comportamiento; verificar que los tests de validación siguen en verde.

## 4. Prestaciones — alta

- [x] 4.1 RED: agregar el test «El administrador da de alta una prestación y la lista se refresca», que espera `POST /api/prestaciones`, un `GET` posterior y la fila nueva; verificar que falla.
- [x] 4.2 GREEN: conectar el envío válido a `POST /api/prestaciones`, refrescar la lista, limpiar el formulario y anunciar el éxito con `Toast`; verificar que el test de 4.1 pasa.
- [x] 4.3 TRIANGULATE: agregar los tests «Un nombre duplicado se muestra como alerta sin perder los datos» (`409 PRESTACION-DUPLICADA`) y «Un 422 de duración del backend se muestra como alerta» (`422 RN-09`); verificar que conservan lo ingresado y no refrescan la lista.
- [x] 4.4 REFACTOR: extraer el mapeo de `ErrorApi` de prestaciones por `codigo` a un módulo y reutilizarlo; verificar `npm test` y `npm run lint`.

## 5. Prestaciones — modificación y reactivación

- [x] 5.1 RED: agregar el test «El administrador edita una prestación y la lista se refresca», que espera `PUT /api/prestaciones/{id}` con los datos nuevos y un `GET` posterior; verificar que falla.
- [x] 5.2 GREEN: implementar la edición que precarga el formulario y envía el `PUT` con `activa` actual, refrescando la lista; verificar que el test de 5.1 pasa.
- [x] 5.3 TRIANGULATE: agregar los tests «El administrador reactiva una prestación dada de baja» (acción de reactivar con `activa: true`) y «Editar una prestación inexistente muestra una alerta y refresca la lista» (`404 PRESTACION-NO-ENCONTRADA`); verificar ambos.
- [x] 5.4 REFACTOR: unificar el formulario de alta y edición en un solo componente sin romper los tests de 4.x y 5.x; verificar `npm test`.

## 6. Prestaciones — baja lógica

- [x] 6.1 RED: agregar el test «El administrador da de baja una prestación y sigue visible como inactiva», que confirma la baja y espera `DELETE /api/prestaciones/{id}`, un `GET` posterior y el estado inactivo; verificar que falla.
- [x] 6.2 GREEN: implementar la confirmación en la fila y el `DELETE` con refresco de la lista; verificar que el test de 6.1 pasa.
- [x] 6.3 TRIANGULATE: agregar el test «Dar de baja una prestación inexistente muestra una alerta» (`404`), con alerta y refresco; verificar que pasa.
- [x] 6.4 REFACTOR: extraer las acciones de fila a un componente pequeño y verificar `npm test` y `npm run lint`.

## 7. Prestaciones — permisos y menú

- [x] 7.1 RED: agregar el test «El administrador ve los controles de alta, edición y baja» montando la ruta real con rol `ADMIN`; verificar que falla.
- [x] 7.2 GREEN: crear `features/prestaciones/routes.ts` con `/prestaciones`, `menu` orden 20 y roles de los tres roles del personal, y renderizar los controles de escritura solo para `ADMIN`; verificar que el test de 7.1 pasa.
- [x] 7.3 TRIANGULATE: agregar los tests «Recepción consulta el catálogo sin controles de escritura», «El odontólogo consulta el catálogo sin controles de escritura» y «La pantalla de prestaciones aparece en el menú para los tres roles»; verificar que no se emiten escrituras en los roles de consulta.
- [x] 7.4 REFACTOR: centralizar la condición de administrador de la feature y verificar `npm test` y `npm run lint`.

## 8. Configuración — carga

- [x] 8.1 RED: crear `frontend/src/features/configuracion/Configuracion.test.tsx` con el test «La configuración se precarga con las siete claves vigentes», que espera los siete campos con sus valores; verificar que falla.
- [x] 8.2 GREEN: implementar `PaginaConfiguracion` con `GET /api/configuracion`, estado de carga y los siete `Campo` con etiquetas en español; verificar que el test de 8.1 pasa.
- [x] 8.3 TRIANGULATE: agregar los tests «Un fallo al consultar la configuración permite reintentar» y «La pantalla no envía escrituras al cargar»; verificar la alerta con reintento y que solo se emite `GET`.
- [x] 8.4 REFACTOR: extraer el catálogo de claves, etiquetas y rangos a un módulo de la feature; verificar `npm test`.

## 9. Configuración — validación RN-36 por campo

- [x] 9.1 RED: agregar el test «Un plazo de cancelación fuera de rango bloquea el guardado sin llamar a la API» con `169` en `cancelacion.plazo_horas`; verificar que falla.
- [x] 9.2 GREEN: implementar la validación al enviar, el mensaje de RN-36 junto al campo y el bloqueo del `PUT`; verificar que el test de 9.1 pasa.
- [x] 9.3 TRIANGULATE: agregar los tests «Una anticipación de recordatorio fuera de rango bloquea el guardado sin llamar a la API» (0), «Un límite positivo en cero bloquea el guardado sin llamar a la API» (0 en horizonte), «Un valor no entero bloquea el guardado sin llamar a la API» (2,5), «Un nombre de consultorio en blanco bloquea el guardado sin llamar a la API» y «Los valores límite permitidos se aceptan» (0 y 168); verificar los seis escenarios de validación.
- [x] 9.4 REFACTOR: extraer `validarConfiguracion` y el helper de entero estricto a un módulo, sin cambiar mensajes ni comportamiento; verificar `npm test` y `npm run lint`.

## 10. Configuración — guardado

- [x] 10.1 RED: agregar el test «El administrador guarda las siete claves y ve la confirmación», que espera `PUT /api/configuracion` con exactamente las siete claves y el `Toast` de éxito; verificar que falla.
- [x] 10.2 GREEN: implementar el `PUT` completo, adoptar la respuesta y mostrar la confirmación; verificar que el test de 10.1 pasa.
- [x] 10.3 TRIANGULATE: agregar los tests «Un 422 RN-36 del backend se muestra como alerta sin perder los valores ingresados» y «Un 400 del backend se muestra como error de operación»; verificar que los mensajes son distintos y no se pierde lo ingresado.
- [x] 10.4 REFACTOR: extraer el mapeo de `ErrorApi` de configuración por `estado`/`codigo` a un módulo; verificar `npm test`.

## 11. Configuración — permisos y menú

- [x] 11.1 RED: agregar el test «El administrador puede editar y guardar» montando la ruta real con rol `ADMIN`; verificar que falla.
- [x] 11.2 GREEN: crear `features/configuracion/routes.ts` con `/configuracion`, `menu` orden 90 y roles `ADMIN` y `RECEPCION`, y habilitar la edición solo para `ADMIN`; verificar que el test de 11.1 pasa.
- [x] 11.3 TRIANGULATE: agregar los tests «Recepción consulta la configuración en solo lectura y no envía PUT», «El odontólogo no puede abrir la configuración» y «La pantalla de configuración aparece en el menú solo para ADMIN y RECEPCION»; verificar que `ODONTOLOGO` recibe el aviso de permiso sin `GET` de configuración.
- [x] 11.4 REFACTOR: unificar la condición de solo lectura de la feature y verificar `npm test` y `npm run lint`.

## 12. Integración y verificación

- [x] 12.1 Ejecutar `npm test`, `npm run lint` y `npm run build` en `frontend/`; verificar que todo termina sin errores.
- [x] 12.2 Comprobar en desarrollo contra el backend real de C-04 y C-08 con cuentas ficticias: `ADMIN` crea una prestación y cambia `cancelacion.plazo_horas` desde la UI; `RECEPCION` consulta ambas pantallas sin botones de escritura y `ODONTOLOGO` no abre configuración. Registrar la evidencia sin credenciales ni cookies.
- [x] 12.3 Confirmar con `git diff --name-only` que no se modificó ningún archivo de `backend/`; si aparece un hueco real de API, detenerse y reportarlo en lugar de tocar el backend.

## Workflow follow-up

- Revisar el PR (lo revisa Juan Pablo) y archivar el change con `openspec-archive-change` cuando esté mergeado.
