# Design

## Context

La motivación está en `proposal.md` (sección Why). Los requisitos están en `specs/pacientes-frontend/spec.md`. Este documento explica cómo se construye la pantalla sobre lo que ya hay en `master`.

**Contrato real del backend (C-07, `backend/.../pacientes/web/PacienteController.java`).** Se verificó leyendo el código, no la ficha:

| Operación | Método y ruta | Roles (`@PreAuthorize`) | Respuesta |
|---|---|---|---|
| Buscar | `GET /api/pacientes?apellido=&nombre=&dni=&email=` (los cuatro son opcionales) | `RECEPCION`, `ADMIN` | `200` con `PacienteResponse[]` y sin paginación |
| Consultar | `GET /api/pacientes/{id}` | `RECEPCION`, `ADMIN` | `200` (este change no lo usa) |
| Alta | `POST /api/pacientes` | `RECEPCION`, `ADMIN` | `201` con `PacienteResponse` |
| Edición | `PUT /api/pacientes/{id}` | `RECEPCION`, `ADMIN` | `200` con `PacienteResponse` |
| Baja lógica | `DELETE /api/pacientes/{id}` | solo `ADMIN` | `204`, idempotente sobre una ficha ya inactiva |

- **Búsqueda (`PacienteRepositorioJpa.buscar`):** solo trae fichas `activo = true`. Apellido y nombre buscan por coincidencia parcial y sin distinguir mayúsculas; DNI busca por coincidencia exacta; email, por coincidencia exacta sin distinguir mayúsculas. Los filtros se combinan con AND y el resultado sale ordenado por apellido y nombre. Si no se manda ningún filtro, devuelve todas las fichas activas.
- **`PacienteRequest`:** `nombre` y `apellido` son `@NotBlank` y `@Size(max = 50)`. `dni` admite hasta 20 caracteres, `email` 100, `telefono` 20, `obraSocial` 200, `nroAfiliado` 50 y `notas` 1000, y todos son opcionales. No hay `activo` en la solicitud.
- **`PacienteResponse`:** `id`, `nombre`, `apellido`, `dni`, `email`, `telefono`, `obraSocial`, `nroAfiliado`, `notas`, `activo` y `creadoEn`.
- **Errores (`ManejadorErrores`, Problem Details con `codigo`):**
  - `409` `RN-32`: el DNI ya pertenece a una ficha activa.
  - `422` `RN-32`: falta el medio de contacto, falta nombre o apellido después de recortar, o se intentó editar una ficha inactiva.
  - `404` `PACIENTE_NO_ENCONTRADO`: la ficha no existe.
  - `400`: Bean Validation, con `errores: [{campo, mensaje}]`.
  - `403`: el rol no tiene permiso.
- **Normalización del backend:** recorta los textos y convierte los vacíos en `null`.

**Convenciones del frontend (C-09, C-16, C-20 y C-21):**
- Cada feature declara sus rutas en `features/<feature>/routes.ts` (`RutaFeature` con `ruta`, `componente`, `menu` y `roles`). `app/rutas/modulos.ts` las descubre con `import.meta.glob` y no se edita. Cada ruta queda envuelta en `RequireSesion` y `RequireRole`, y este último muestra `Alerta` "No tenés permiso" cuando el rol no está en la lista.
- La API se llama solo con `api` (`shared/api/clienteApi`), que agrega CSRF a las escrituras y rechaza con `ErrorApi` (`tipo`, `estado`, `codigo`, `campos`).
- Cada feature tiene su propio `errores.ts`, que traduce `ErrorApi` por `codigo` o `estado` y nunca muestra `detalle`; su `permisos.ts`, con `useEsAdministrador()` leyendo `useSesion()`; su `tipos.ts` con tipos `Readonly`; su `validacion.ts` con funciones puras; y un hook `useX` de carga con un contador que descarta las respuestas viejas.
- El kit `shared/ui` ofrece `Campo` (input con `etiqueta`, `error` y `ayuda`, conectados por aria), `Boton` (variantes `primaria`, `secundaria`, `peligro` y `peligro-atenuado`; tamaño `chico`), `Tabla` (`columnas`, `filas`, `claveFila`, `mensajeVacio`), `Tarjeta`, `Alerta` y `Toast`. No hay componente de área de texto.
- Los tests son de integración de pantalla: `render(<MemoryRouter><AppRutas registro={construirRegistro({ pacientes: { rutas } })} /></MemoryRouter>)` con `simularSesionActiva(rol, responder)` (`app/sesion.fixture.ts`), que simula `fetch` y `/api/auth/me`. Se busca por rol accesible (`findByRole("row")`, `getByRole("button", { name })`) y las escrituras se cuentan filtrando `fetch.mock.calls` por método.

## Goals / Non-Goals

**Goals:**
- Una pantalla única `/pacientes` con búsqueda, tabla de resultados y formulario de alta o edición. El formulario es el mismo y cambia de modo, como en sillones y prestaciones.
- Mostrar cada error de RN-32 en su campo, como pide el criterio de salida de la ficha.
- Reutilizar sin cambios `shared/`, `app/` y el patrón de archivos de C-20 y C-21.

**Non-Goals:**
- No se toca `shared/ui`: no se agrega un componente de área de texto compartido. Ver la decisión D6.
- No se agrega lógica de dominio que el backend no tenga, como validar el formato del email o del DNI, reactivar fichas o paginar.
- No se cubre la vista del odontólogo (C-28).

## Decisions

### D1. Estructura de `features/pacientes/`
Archivos: `routes.ts`, `PaginaPacientes.tsx`, `BusquedaPacientes.tsx`, `FormularioPaciente.tsx`, `AccionesFilaPaciente.tsx`, `usePacientes.ts`, `tipos.ts`, `validacion.ts`, `errores.ts`, `permisos.ts` y `Pacientes.test.tsx`. Se borra el `.gitkeep`. Los tests puros de validación y búsqueda pueden ir en `validacion.test.ts`.
- **Alternativa descartada:** separar listado y formulario en rutas distintas (`/pacientes/nuevo`, `/pacientes/:id`). Ninguna feature existente lo hace, y recepción tiene que tener "pocas pantallas" (KB 03).

### D2. Ruta, menú y roles
`{ ruta: "/pacientes", menu: { etiqueta: "Pacientes", orden: 10 }, roles: ["ADMIN", "RECEPCION"] }`. El orden 10 la deja entre Inicio (0) y Prestaciones (20), porque es la pantalla de uso diario de recepción. `ODONTOLOGO` queda fuera porque el backend le responde `403` en todo `/api/pacientes` (escenario de C-07 "El odontólogo no accede a las fichas"). `RequireRole` ya muestra el aviso sin montar la página, así que no se emite ninguna consulta.
- **Alternativa descartada:** incluir `ODONTOLOGO` en solo lectura, como en prestaciones y sillones. Solo vería errores `403`. La lectura limitada a sus pacientes llega con C-28, que va a ampliar `roles`.

### D3. Carga inicial y búsqueda
`usePacientes()` guarda los filtros vigentes y expone `buscar(filtros)` y `recargar()`. Al montar consulta `GET /api/pacientes` sin filtros, porque el backend devuelve todas las fichas activas, igual que las otras pantallas cargan su catálogo al abrir. `BusquedaPacientes` es un `<form>` con cuatro `Campo`: Apellido, Nombre, DNI y Email. DNI y Email llevan `ayuda="Coincidencia exacta."` porque así busca el backend. Al enviar, se arma la consulta con `URLSearchParams` solo con los valores recortados no vacíos, en el orden fijo apellido, nombre, dni, email. Después de cada escritura, `recargar()` repite la consulta con los filtros vigentes. Para evitar carreras se usa el mismo contador que `useSillones`.
- **Alternativa A, descartada:** no consultar hasta que el usuario busque. Agrega un estado "todavía no buscaste" que ninguna otra pantalla tiene y obliga a buscar para ver algo recién creado.
- **Alternativa B, descartada:** filtrar en el cliente. Duplica la semántica del backend (parcial o exacta) y puede divergir.

### D4. Validación local (RN-32) y armado del cuerpo
`validarPaciente(valores)` es una función pura y devuelve `{ nombre?, apellido?, contacto? }`. Los mensajes son "El nombre es obligatorio.", "El apellido es obligatorio." y "Ingresá al menos un medio de contacto: email o teléfono.". El error de contacto se muestra en el `Campo` de email y en el de teléfono. `normalizarPaciente(valores)` recorta y convierte los vacíos en `null` en los ocho campos, igual que el backend. Los largos máximos se aplican con `maxLength` en cada input (50/50/20/100/20/200/50/1000), lo que espeja `@Size` sin escribir otra validación. No se valida el formato del email ni del DNI porque el backend no lo hace.
- **Alternativa descartada:** validar solo en el backend. Generaría escrituras inútiles y el patrón de C-20 (RN-09) valida antes de enviar.

### D5. Mapeo de errores a campos
`errores.ts` expone `erroresDeApiPaciente(error): { campos: Partial<Record<CampoPaciente, string>>; alerta?: string }`:

| Respuesta de la API | Dónde se muestra | Mensaje |
|---|---|---|
| `409` + `codigo: "RN-32"` | Campo DNI | "Ya existe un paciente activo con ese DNI." |
| `422` + `codigo: "RN-32"` | Campos email y teléfono | "Ingresá al menos un medio de contacto: email o teléfono." (el mismo mensaje que la validación local) |
| `400` con `campos[]` | El campo indicado por `campo` (`nombre`, `apellido`, `dni`, `email`, `telefono`, `obraSocial`, `nroAfiliado` o `notas`) | "Revisá este dato: no es válido o es demasiado largo." |
| `400` sin campos conocidos | Alerta del formulario | "No pudimos guardar la ficha. Revisá los datos e intentá de nuevo." |
| `404` + `codigo: "PACIENTE_NO_ENCONTRADO"` | Alerta de la página, y se refresca la lista | "El paciente ya no existe." |
| `403` | Alerta del formulario o de la página | "No tenés permiso para realizar esta acción." |
| Otro (`500`, red) | Alerta | "No pudimos completar la operación. Intentá de nuevo." |

El mensaje del `409` no menciona a la otra ficha (RN-35). El texto del `campo.mensaje` del `400` no se muestra, porque es un mensaje del servidor. El formulario conserva lo ingresado ante cualquier error. Los tests importan las constantes de mensaje desde `errores.ts` y `validacion.ts`.
- **Alternativa descartada:** mostrar todo como `Alerta` arriba del botón, como hacen sillones y prestaciones. Contradice el criterio de salida de la ficha ("los errores del backend se muestran en el campo correspondiente"), que es la fuente de verdad.

### D6. Campo de notas
Como el kit no tiene área de texto, `FormularioPaciente` usa un `<textarea>` nativo envuelto con las mismas clases del kit (`field`, `input`, `error`) y el mismo cableado aria que `Campo` (`useId`, `aria-invalid` y `aria-describedby`). Así quedan solo tokens y no hay estilos fijos.
- **Alternativa A, descartada:** agregar un `AreaTexto` a `shared/ui`. Toca el kit compartido (y sus tests de estilos) fuera del alcance de la ficha. Puede hacerlo C-50.
- **Alternativa B, descartada:** usar `Campo` (input de una línea) para notas. Es válido, pero incómodo para 1000 caracteres.

### D7. Acciones por fila y baja
`AccionesFilaPaciente` se crea dentro de la feature. No se importa `features/profesionales/AccionesFila`, porque ese componente obliga a pasar `onReactivar` y exige la baja, y las features no se importan entre sí. Muestra "Editar" a `ADMIN` y `RECEPCION`, y "Dar de baja" (variante `peligro-atenuado`) solo a `ADMIN`. La confirmación se hace en la propia fila con "Confirmar baja" (`peligro`) y "Cancelar", como en C-20 y C-21. Con un `204` aparece el `Toast` "Paciente dado de baja." y se llama a `recargar()`, así que la ficha desaparece de la tabla porque la búsqueda excluye inactivas. No hay botón "Reactivar", porque el backend no lo permite.

### D8. Columnas de la tabla
Las columnas son Apellido, Nombre, DNI, Email, Teléfono, Obra social y Acciones. La marca de vacío es "—", como en sillones. El número de afiliado y las notas no van en la tabla, para que quede legible; se ven y se editan en el formulario.

### D9. Permisos en la UI
`permisos.ts` exporta `useEsAdministrador()` con la misma forma que las otras features, para la baja. Alta y edición no necesitan chequeo, porque solo llegan a la pantalla `ADMIN` y `RECEPCION`. La SPA solo oculta controles y la autorización efectiva sigue en el backend (KB 03).

### D10. Estrategia de tests
- **Pantalla (`Pacientes.test.tsx`):** usa `simularSesionActiva(rol, responder)` con un `responder` que despacha por `metodo` y `url`. No usa `vi.mock` del cliente, porque las otras features simulan `fetch`. Así se prueba de verdad el armado de la URL, el CSRF y la traducción de Problem Details. Las respuestas de error se arman con `Response.json({ status, codigo, detail, errores }, { status, headers: { "Content-Type": "application/problem+json" } })`.
- **Funciones puras (`validacion.test.ts`):** cubren el armado de filtros y la normalización, si conviene separarlas.
- **Datos:** solo ficticios (por ejemplo, "Ana Ficticia", `ana.ficticia@example.test`, DNI "30111222", teléfono "1100000000"). No se usan nombres ni DNI de personas reales.
- **Nombres:** cada `it(...)` lleva el texto exacto de un `#### Scenario:` de la spec, y hay 26 en total.

## Risks / Trade-offs

- **[Riesgo] El código `RN-32` es el mismo para varios `422`** (contacto, nombre o apellido vacío, ficha inactiva). El front no puede distinguirlos sin leer `detalle`, que la convención prohíbe. → **Mitigación:** la validación local evita los casos de contacto y nombre, así que un `422` que llega igual se muestra como contacto faltante, que es el mapeo que pide la ficha. El caso de ficha inactiva solo aparece si otra persona la da de baja mientras se edita; el mensaje no es exacto, pero no se pierden datos y la próxima recarga ya no muestra la ficha. Queda registrado en las preguntas abiertas.
- **[Riesgo] No hay paginación:** con muchas fichas activas, la carga inicial trae todas. → **Mitigación:** el volumen esperado de un consultorio de 2 a 5 profesionales es chico, y la búsqueda reduce el resultado. Si hiciera falta, la paginación sería un change de backend.
- **[Trade-off] Una baja no se puede revertir desde la UI:** la ficha desaparece y no hay forma de reactivarla. → **Mitigación:** se pide confirmación y la baja queda limitada a `ADMIN`, como define el backend.
- **[Riesgo] Diferencia visual con sillones y prestaciones:** esas pantallas muestran los errores remotos como alerta, y esta los muestra en el campo. → **Mitigación:** es lo que pide la ficha. Se usa el mismo `Campo.error` del kit, así que la diferencia es solo de ubicación.

## Migration Plan

No hace falta migración: es una pantalla nueva que no cambia el backend ni los datos. Para revertir alcanza con quitar `features/pacientes/` (y restaurar el `.gitkeep`).

## Open Questions

1. **(No bloqueante) Códigos de error distintos para los `422` de RN-32.** El backend usa `RN-32` tanto para el contacto faltante como para editar una ficha inactiva. **Recomendación:** mantener el mapeo de D5 en este change y, si el equipo lo considera útil, proponer en un change de backend posterior un código propio para la ficha inactiva, por ejemplo `PACIENTE_INACTIVO`. Eso no cambia esta spec.
2. **(No bloqueante) Orden del menú.** Ni la KB ni CHANGES fijan el orden de las entradas. **Recomendación:** 10, entre Inicio y Prestaciones (D2). C-50 puede reordenar.

## UI Mockups

- Design system revision: 1
- Decision: no (project policy), 2026-11-25
- Mockups: n/a (el modo del proyecto es `system-only`)
- Approval: n/a
- Preview: n/a
- Visual verify: pendiente (se revisa contra `design-system/components.html` y los tokens durante verify)
