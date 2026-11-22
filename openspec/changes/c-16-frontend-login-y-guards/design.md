# Design

## Context

Ver `proposal.md` para la motivación y `specs/sesion-frontend-personal/spec.md` para el contrato. `AppRutas` monta hoy todas las rutas registradas bajo `Layout`; `RutaFeature.roles` ya existe pero no se aplica. `clienteApi.ts` usa el proxy del mismo origen, devuelve `ErrorApi` y obtiene CSRF si falta la cookie. C-03 devuelve `{id, email, rol}` en login y `/me`, rota CSRF en los cambios de sesión y no distingue las causas del `401` de login.

## Goals / Non-Goals

**Goals:** una sola fuente de verdad de la sesión y un único criterio de acceso para rutas y menú, sin cambiar las declaraciones existentes de rutas internas.

**Non-Goals:** reemplazar la sesión del servidor, trasladar la autorización al cliente, agregar persistencia web o cambiar los contratos de C-03.

## Decisions

1. **Estado de sesión en `features/auth`.** Un proveedor React bajo `BrowserRouter` mantiene estados explícitos de comprobación, error recuperable, anonimato y sesión activa. Comprueba `/me` al entrar a login o al área interna, no al entrar solo al área pública. El login usa la identidad de la respuesta, no repite `/me`. Se evita montar contenido protegido mientras se comprueba la sesión; una respuesta tardía de un chequeo anterior no puede reemplazar una sesión nueva ni restaurar una cerrada. Se descarta `localStorage` porque el estado válido vive en el servidor.
2. **Un solo cliente HTTP y una invalidación acotada.** Las operaciones de auth usan `api` con rutas relativas `/auth/me`, `/auth/login` y `/auth/logout`. El cliente incorpora una notificación de `401` para operaciones internas no pertenecientes a `/auth/*` ni `/public/*`; el proveedor se suscribe con limpieza al desmontar, e invalida solo la sesión activa asociada a la solicitud, ignorando respuestas rezagadas de una sesión anterior. Las operaciones de auth manejan su propio resultado y todo error sigue llegando tipado al llamador. El router reacciona al estado con reemplazo de historial; no usar `window.location`, no convertir `403` en expiración y no reintentar escrituras automáticamente. Reusar la lectura de CSRF por escritura, que obtiene un token si la cookie rotada ya no está.
3. **Separar rutas públicas y privadas sin duplicar el menú.** Extender el metadato de `RutaFeature` para declarar explícitamente rutas públicas; la ausencia de marca conserva el carácter interno. Registrar `/login` en `features/auth/routes.ts` y renderizarlo fuera del layout privado. Componer `AppRutas` con rama privada que conserva layout, carga diferida y página no encontrada; `RequireRole` y el menú consumen el mismo criterio derivado de las rutas registradas, preservando orden y selección. Roles omitidos permiten a los tres roles del personal; una lista explícita vacía no permite a ninguno. Las features futuras son responsables de aplicar el mismo criterio a sus controles de escritura. Se descarta mantener una segunda lista manual de rutas y permisos.
4. **Formulario con el kit de C-09.** Usar `Campo`, `Boton`, `Alerta` y, si corresponde, `Tarjeta`, con etiquetas, `autocomplete` apropiado y gestión del envío pendiente. Mostrar mensajes locales en español, no el `detail` de `ErrorApi`; el `401` se trata en el formulario, no como expiración global. No tocar marca ni tokens.

## Risks / Trade-offs

- Respuestas tardías o simultáneas → asociar la comprobación y los `401` al ciclo de sesión que los originó y probar carreras con promesas controladas.
- C-09 todavía tiene deltas activos → conservar registro modular, orden, marca, navegación activa y 404 interno. Si C-09 se archiva antes de dar salida a C-16, conciliar sus requisitos de menú y layout con las excepciones de sesión y área pública, sin modificar los artefactos de C-09 en este propose.
- Aún no existen las pantallas objetivo de cada rol → Inicio es el único destino posterior al login; usar rutas de fixture para verificar acceso directo y visibilidad por rol.

## Migration Plan

No hay migración de datos, de API ni dependencias nuevas. Desplegar con el backend de C-03 disponible bajo el origen esperado; para revertir, restaurar el frontend anterior sin tocar sesiones ni base de datos.
