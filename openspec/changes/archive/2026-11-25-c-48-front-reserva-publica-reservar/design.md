# Diseño

## Context

`AppRutas` admite rutas de ámbito público. C-38 devuelve intervalos libres recortados por las reglas del backend. C-41 publica el aviso de consentimiento versionado mediante los datos de `GET /api/public/consultorio`, acepta la reserva y devuelve el token en el 201. La API ya exime de CSRF a `/api/public/**`.

## Goals / Non-Goals

- Completar el flujo a 390 × 844, con teclado y lectores de pantalla.
- Usar los endpoints públicos existentes y el kit de C-09. La vista que abre el enlace del turno corresponde a C-49.

## Decisions

- Declarar `/reservar` en `features/reserva-publica/routes.ts`, con ámbito público. Mantener cuatro pasos explícitos y un resumen de la selección antes de confirmar.
- Consultar prestaciones y profesionales en ese orden. Consultar disponibilidad por semanas con límites en la zona del consultorio y conservar el instante ISO de la selección. Cambiar la prestación invalida profesional y horario. Cambiar el profesional invalida el horario. Ignorar respuestas tardías de selecciones anteriores.
- Pedir nombre y apellido por separado para respetar el DTO. Usar un campo de texto libre para obra social conforme a US-015. Mostrar «Cómo usamos tus datos» de PA-08 con los valores públicos del consultorio y dejar la casilla inicialmente desmarcada. Enviar la versión correspondiente al texto que se mostró. Si cambia la versión, volver a cargar los datos y pedir aceptación otra vez. Bloquear el envío si la SPA carece del texto de la versión vigente.
- Usar el cliente API común y omitir la solicitud de CSRF en el segmento exacto `/public/`, igual que la excepción del backend. Los errores conservan el estado y las propiedades tipadas de Problem Details. Leer `alternativas` como datos de la API y validar su estructura antes de mostrarlas.
- Un 409 por horario ocupado vuelve al paso de horario con las alternativas y conserva el formulario en memoria. Con lista vacía, permite consultar otras fechas. El paciente revisa el nuevo horario antes de volver a enviar. Un 400 muestra los errores de los campos y un 429 permite reintentar manualmente.
- Durante el envío, impedir dobles clics y no reintentar el POST automáticamente. Mantener los datos personales y el token en memoria, limpiar el formulario al confirmar y construir el enlace relativo `/turno/{token}`.
- Reutilizar el sistema visual y los formateadores de Buenos Aires. Usar controles etiquetados, foco visible, mensajes anunciados y objetivos táctiles según el sistema.

## Risks / Trade-offs

- La disponibilidad es preliminar. El 409 del servidor resuelve la concurrencia al confirmar.
- Recargar la página reinicia el flujo. El enlace sigue disponible en el correo de confirmación de C-41.
- Validar el enlace hasta la salida de C-48. La vista de gestión puede integrarse de punta a punta cuando esté disponible C-49.
