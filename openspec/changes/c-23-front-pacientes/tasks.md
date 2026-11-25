# Tasks

> Se trabaja con TDD estricto: RED, GREEN, TRIANGULATE y REFACTOR. Cada `it(...)` lleva el nombre exacto de un `#### Scenario:` de `specs/pacientes-frontend/spec.md` (regla dura 11) y se usan solo datos ficticios (regla dura 1). Todos los comandos se corren en `frontend/`. Para simular `fetch` se usa `simularSesionActiva(rol, responder)` de `app/sesion.fixture.ts`, como en C-20 y C-21 (design D10).

## 0. Red de seguridad

- [x] 0.1 Correr `npm ci` y `npm test` antes de tocar nada y anotar la línea de base («N tests en verde»). Si alguno falla, se informa como falla previa y no se corrige.

## 1. Ruta, menú y acceso por rol

- [x] 1.1 RED: crear `frontend/src/features/pacientes/Pacientes.test.tsx` con los helpers `montar(rol, responder)`, `escrituras(fetch)` y `consultasPacientes(fetch)` (con la misma forma que en `Sillones.test.tsx`) y el test «La pantalla de pacientes aparece en el menú para administración y recepción», que recorre `ADMIN` y `RECEPCION`. Verificar que falla.
- [x] 1.2 GREEN: crear `features/pacientes/routes.ts` con `{ ruta: "/pacientes", menu: { etiqueta: "Pacientes", orden: 10 }, roles: ["ADMIN", "RECEPCION"] }` y una `PaginaPacientes` mínima, y borrar `features/pacientes/.gitkeep`. Verificar que pasan 1.1 y `app/estructura.test.ts`.
- [x] 1.3 TRIANGULATE: agregar «El odontólogo no ve la pantalla de pacientes», que verifica que el menú no tiene la entrada, que aparece el aviso «No tenés permiso» y que hay 0 consultas a `/api/pacientes`. Verificar que pasa.

## 2. Listado inicial y búsqueda

- [x] 2.1 RED: agregar «Al abrir la pantalla se listan las fichas activas», con dos fichas ficticias, el `GET /api/pacientes` exacto sin parámetros y las seis columnas de datos. Verificar que falla.
- [x] 2.2 GREEN: crear `tipos.ts` (`PacienteRespuesta`, `PacienteSolicitud`, `FiltrosPacientes`, todos `Readonly`), `usePacientes.ts` (carga al montar con el contador antirrespuestas viejas de `useSillones`) y la `Tabla` de `PaginaPacientes` con las columnas Apellido, Nombre, DNI, Email, Teléfono y Obra social. Verificar que pasa 2.1.
- [x] 2.3 TRIANGULATE: agregar «Un dato ausente de la ficha se muestra con una marca de vacío» y «Un fallo al consultar pacientes permite reintentar», e implementar la marca «—», el estado `Cargando…` y la `Alerta` con «Reintentar» que repite la última consulta. Verificar los tres tests.
- [x] 2.4 RED: agregar «La búsqueda por apellido envía solo ese filtro», que espera exactamente `/api/pacientes?apellido=G%C3%B3mez`. Verificar que falla.
- [x] 2.5 GREEN: crear `BusquedaPacientes.tsx` con cuatro `Campo` (Apellido, Nombre, DNI y Email, estos dos con `ayuda="Coincidencia exacta."`) y una función pura `consultaDePacientes(filtros)` que arme la ruta con `URLSearchParams` solo con los valores recortados no vacíos, en el orden apellido, nombre, dni, email. Conectar `buscar(filtros)` de `usePacientes`. Verificar que pasa 2.4.
- [x] 2.6 TRIANGULATE: agregar «Los filtros de búsqueda se combinan», «La búsqueda por DNI envía el DNI recortado» y «Una búsqueda sin coincidencias muestra un mensaje» (`mensajeVacio` «No se encontraron pacientes.»). Verificar que pasan los nueve tests de las tareas 1.x y 2.x.
- [x] 2.7 REFACTOR: mover `consultaDePacientes` a un módulo propio, si todavía no lo está, y eliminar duplicación entre la carga inicial y la búsqueda. Verificar que `npm test` y `npm run lint` quedan en verde.

## 3. Validación local y alta

- [x] 3.1 RED: agregar «Una ficha sin nombre o apellido bloquea el envío sin llamar a la API», que espera los dos mensajes junto a sus campos y `escrituras(fetch)` en 0. Verificar que falla.
- [x] 3.2 GREEN: crear `FormularioPaciente.tsx` (`Tarjeta`, `form noValidate`, ocho campos con `maxLength` 50/50/20/100/20/200/50/1000 y `<textarea>` de notas con las clases y el aria del kit, según design D6) y `validacion.ts` con `validarPaciente` y sus constantes de mensaje. Verificar que pasa 3.1.
- [x] 3.3 TRIANGULATE: agregar «Una ficha sin email ni teléfono bloquea el envío sin llamar a la API», con el mensaje de contacto en email y teléfono. Verificar que pasa.
- [x] 3.4 RED: agregar «Recepción da de alta un paciente y la lista se refresca», que espera `POST /api/pacientes` con los ocho campos, el `Toast` «Paciente agregado.», el formulario vacío y un `GET` posterior. Verificar que falla.
- [x] 3.5 GREEN: agregar `normalizarPaciente` (recorte y vacío a `null`) y conectar el envío válido a `api.post("/pacientes", …)` con `recargar()`. Verificar que pasa 3.4.
- [x] 3.6 TRIANGULATE: agregar «Una ficha con solo teléfono se acepta» (`email: null` en el cuerpo) y «La obra social se envía como texto libre». Verificar los cinco tests de validación y alta.
- [x] 3.7 REFACTOR: dejar `validarPaciente` y `normalizarPaciente` como funciones puras sin dependencias de React y revisar que no haya colores, espaciados ni fuentes fijos (regla dura 13). Verificar que `npm test` y `npm run lint` quedan en verde.

## 4. Errores del backend en el campo

- [x] 4.1 RED: agregar «Un DNI duplicado se muestra en el campo DNI sin perder los datos», con un `409` `application/problem+json` con `codigo: "RN-32"` y un `detail` que no debe aparecer en pantalla. Verificar que falla.
- [x] 4.2 GREEN: crear `errores.ts` con `erroresDeApiPaciente(error)` y las constantes de mensaje de design D5, y mostrar `campos.dni` en el `Campo` DNI sin vaciar el formulario. Verificar que pasa 4.1.
- [x] 4.3 TRIANGULATE: agregar «Un contacto faltante del backend se muestra en los campos de contacto» (`422` `RN-32`), «Un campo inválido del backend se muestra en su campo» (`400` con `errores: [{ campo: "notas", mensaje: "…" }]`) y «Un rechazo por permiso se muestra como alerta» (`403` al alta). Generalizar el mapeo. Verificar los cuatro tests.
- [x] 4.4 REFACTOR: unificar en `FormularioPaciente` la combinación de errores locales y remotos (los remotos se limpian al editar el campo, como en `FormularioSillon`). Verificar que `npm test` queda en verde.

## 5. Edición

- [x] 5.1 RED: agregar «Recepción edita una ficha y la lista se refresca», que espera `PUT /api/pacientes/{id}` con los datos precargados y el teléfono nuevo, el `Toast` «Paciente actualizado.» y un `GET` posterior con los filtros vigentes. Verificar que falla.
- [x] 5.2 GREEN: crear `AccionesFilaPaciente.tsx` con «Editar», precargar `FormularioPaciente` (`key` por id, `valorInicial`, título «Editar paciente», envío «Guardar cambios») y enviar el `PUT`. Verificar que pasa 5.1.
- [x] 5.3 TRIANGULATE: agregar «Cancelar la edición vuelve al formulario de alta» y «Editar una ficha inexistente muestra una alerta y refresca la lista» (`404` `PACIENTE_NO_ENCONTRADO`, alerta «El paciente ya no existe.» y recarga). Verificar los tres tests.
- [x] 5.4 REFACTOR: unificar alta y edición en un solo flujo de envío en `PaginaPacientes`, sin romper los tests de las secciones 3, 4 y 5. Verificar que `npm test` queda en verde.

## 6. Baja lógica y controles por rol

- [x] 6.1 RED: agregar «El administrador da de baja una ficha y deja de aparecer en la lista», que confirma en la fila y espera `DELETE /api/pacientes/{id}`, el `Toast` «Paciente dado de baja.» y un `GET` posterior que ya no trae la ficha. Verificar que falla.
- [x] 6.2 GREEN: crear `permisos.ts` con `useEsAdministrador()` y sumar a `AccionesFilaPaciente` «Dar de baja» (solo `ADMIN`) con la confirmación en la fila («Confirmar baja» y «Cancelar»). Verificar que pasa 6.1.
- [x] 6.3 TRIANGULATE: agregar «Cancelar la confirmación no da de baja la ficha», «Dar de baja una ficha inexistente muestra una alerta», «El administrador ve los controles de alta, edición y baja» y «Recepción ve alta y edición pero no la baja». Verificar los cinco tests.
- [x] 6.4 REFACTOR: revisar `PaginaPacientes` según las reglas de React de `vercel-react-best-practices` que aplican a Vite: estado derivado en el render, sin componentes definidos dentro de componentes, ternarios en lugar de `&&` donde puede aparecer un `0`, e imports directos sin barrel nuevos. Verificar que `npm test` y `npm run lint` quedan en verde.

## 7. Verificación final

- [x] 7.1 Verificar que `Pacientes.test.tsx` (y `validacion.test.ts`, si existe) tiene exactamente los 26 escenarios de la spec, con el texto idéntico en cada `it(...)`, y que no hay `any` ni datos reales.
- [x] 7.2 En `frontend/`, correr `npm ci`, `npm run build`, `npm test` y `npm run lint`. Verificar que los cuatro terminan sin errores y que `npm test` supera la línea de base de 0.1.
  - Resultado (2026-11-25, Node v26.13.1): `npm ci` y `npm run build` OK (113 módulos, build en 1.10 s); `npm test`: 25 de 26 archivos en verde, 205 tests en verde y 1 falla (206 en total; línea de base 179 + 26 de C-23); `npm run lint` sin errores y sin recrear `frontend/.vite/`. La única falla es `src/features/auth/Login.test.tsx > El personal inicia sesión y llega a Inicio`: es del entorno (Node 26 expone su propio `localStorage` global y rompe el de jsdom), ya fallaba en `master` antes de C-23 y el CI usa Node 22.
- [x] 7.3 Prueba manual con el backend levantado (`docker compose up -d`, el backend en perfil dev y `npm run dev`): entrar como admin del seed, crear un paciente ficticio, buscarlo por apellido y por DNI, editar su teléfono, provocar un DNI duplicado (que tiene que verse en el campo) y darlo de baja (que tiene que desaparecer de la búsqueda). Repetir el alta y la edición con un usuario `RECEPCION` y confirmar que no ve «Dar de baja». Anotar el resultado en el PR. **Resultado (2026-11-25):** el usuario hizo la prueba manual con sesión iniciada y confirmó que funciona.

## Workflow follow-up

- Correr `openspec-verify` y dejar `verify.md` con el veredicto. La verificación visual es manual, contra `design-system/components.html`.
- Preguntar antes de cada commit (regla dura 16) y abrir el PR a `master` desde `santino-c-23-front-pacientes`, para que lo revise Juan Pablo.
- Después del merge, correr `/opsx:archive c-23-front-pacientes`.
