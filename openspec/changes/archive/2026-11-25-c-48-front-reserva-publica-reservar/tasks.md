# Tareas

## 1. Acceso público y selección

- [x] 1.1 Con TDD, adaptar el cliente compartido para las peticiones públicas y probar la excepción de CSRF y las escrituras del personal.
- [x] 1.2 Con TDD, registrar `/reservar` y conectar prestaciones, profesionales y disponibilidad. Cubrir el acceso anónimo, el cambio de selecciones, las respuestas tardías y los rangos vacíos.

## 2. Formulario y resultado

- [x] 2.1 Con TDD, integrar los campos y el aviso de PA-08. Cubrir la casilla sin marcar, los errores por campo y los cambios de versión del consentimiento.
- [x] 2.2 Con TDD, conectar el POST y la confirmación. Cubrir el enlace, el doble envío, las alternativas del 409 y el reintento manual del 429.

## 3. Verificación

- [x] 3.1 Recorrer una reserva con datos ficticios a 390 × 844, usando teclado y controles táctiles. Verificar foco, mensajes de error, ausencia de desplazamiento horizontal y confirmación.
- [x] 3.2 Ejecutar lint, Vitest, build, OpenSpec estricto y la suite de integración aplicable. Dejar el informe de verificación con el mapeo entre escenarios y pruebas.
