# Tasks

## 1. Base de la feature

- [x] 1.1 Con TDD, registrar `/lista-espera` con roles `ADMIN` y `RECEPCION` y entrada en el menú. Cubrir «Recepción ve la lista de espera en el menú» y «El odontólogo no accede a la lista de espera».
- [x] 1.2 Escribir los tipos, el cliente de la API y la carga de catálogos (pacientes, prestaciones y profesionales) con nombres de respaldo.

## 2. Listado y baja

- [x] 2.1 Con TDD, implementar el listado por estado con nombres legibles, el filtro y el mensaje de estado vacío.
- [x] 2.2 Con TDD, implementar la baja con confirmación inline, un solo DELETE, la opción de desistir y el 409 con recarga.

## 3. Alta

- [x] 3.1 Con TDD, armar el formulario con `SeleccionPaciente` y validar obligatorios, rango de fechas y franja.
- [x] 3.2 Con TDD, implementar el envío con un solo POST, la recarga del listado y los rechazos 409 duplicada, 422 sin email y 400 por campo, conservando el formulario.

## 4. Sugerencias y asignación

- [x] 4.1 Con TDD, armar el formulario del hueco, validar el horario y consultar las compatibles en Buenos Aires con el estado vacío.
- [x] 4.2 Con TDD, implementar la asignación con un solo POST, el 201 con recarga y los rechazos 409 (ocupado y no activa) y 422 (incompatible).

## 5. Verificación

- [x] 5.1 Recorrer la pantalla con datos ficticios a 390 × 844 y 1440 × 900, con teclado, y guardar las capturas en `verify/`.
- [x] 5.2 Ejecutar lint, `tsc`, Vitest, build y `openspec validate --strict`, y dejar `verify.md` con el mapeo entre escenarios y tests.

## Workflow follow-up

- Commit en `valentino` (previa confirmación), PR a `master` y, después del merge, `/opsx:archive` y actualizar `CHANGES.md`.
