# Tasks

## 1. Sesión y formulario del personal

- [x] 1.1 Implementar el estado de sesión en memoria y `/auth/me` usando el cliente de C-09; cubrir en Vitest la espera sin contenido interno, la restauración en la misma ruta, el `401` inicial y el reintento tras un fallo técnico.
- [x] 1.2 Registrar `/login` y conectar el formulario accesible al estado de sesión; cubrir con Vitest el `200` y destino Inicio, el envío pendiente, el `401` uniforme y los fallos no relacionados con las credenciales.

## 2. Rutas, roles y fin de sesión

- [x] 2.1 Componer las ramas pública e interna en el router y aplicar `RequireRole` y el filtrado del menú con el mismo criterio; cubrir en Vitest la separación pública, acceso directo, orden y selección del menú, caso sin permiso, lectura sin escritura para `ODONTOLOGO` y regresión del 404 interno.
- [x] 2.2 Integrar el cierre de sesión en el layout y la invalidación por `401` de peticiones internas; cubrir en Vitest `204` y `401` de logout, fallo recuperable de logout, `401` interno, `403` sin cierre, error público sin redirección, y respuestas tardías de ciclos anteriores con promesas controladas.

## 3. Integración en desarrollo

- [x] 3.1 Ejecutar `npm test`, `npm run lint` y `npm run build` en `frontend/`. Comprobar en desarrollo con C-03 real y cuentas ficticias el rechazo uniforme, login, restauración al recargar, una escritura de sesión con CSRF tras el login (logout), el `401` posterior de `/me` y la ausencia de opciones o controles operativos de escritura para `ODONTOLOGO`. Registrar la evidencia sin credenciales ni cookies.

Evidencia de `apply`: 87 tests aprobados, lint y build exitosos. En navegador contra el backend de desarrollo de C-03 y PostgreSQL 16 aislado, los cuatro rechazos mostraron el mismo mensaje y ayuda. Un `ODONTOLOGO` inició sesión, recuperó el rol al recargar y vio Inicio y «Cerrar sesión», sin controles de escritura operativa. `POST /api/auth/logout` respondió `204`; después, `/api/auth/me` devolvió `401` y la SPA quedó en `/login`.
