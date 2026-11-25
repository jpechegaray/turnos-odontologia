# Tasks

**Reglas para todas las tareas:**
- TDD estricto en cada tarea: primero el test que falla, después el código mínimo y por último el refactor.
- Solo datos ficticios.
- Cuando un test cubra un escenario de la spec, su `@DisplayName` lleva el nombre exacto del escenario.
- Un commit por etapa, con confirmación previa.

## 1. Dominio puro

- [ ] 1.1 Crear `listaespera.domain.Hueco` (design D1). Se verifica con JUnit:
  - rechaza nulos;
  - rechaza `fin <= inicio`;
  - expone la duración.
- [ ] 1.2 Crear `PrestacionParaHueco` y `CompatibilidadListaEspera.esCompatible(...)` (design D2). Se verifica con JUnit sin Spring:
  - "Una prestación de 90 minutos no cabe en un hueco de 30";
  - "Una prestación que dura exactamente el hueco es compatible";
  - "El profesional del hueco debe realizar la prestación";
  - "Una prestación inactiva no es compatible";
  - "Se respeta el profesional preferido";
  - "Una entrada sin profesional preferido acepta a cualquiera que realice la prestación";
  - "La fecha del hueco debe estar dentro del rango de la entrada";
  - "La fecha se toma en hora de Buenos Aires";
  - "El turno resultante debe caer dentro de la franja";
  - "Una entrada sin franja acepta cualquier hora";
  - "Una entrada que no está activa no es compatible";
  - además, un turno resultante que cruza la medianoche no cae en la franja.
- [ ] 1.3 Crear `SelectorDeAvisos.seleccionar(...)` (design D3). Se verifica con JUnit:
  - "El tope de avisos elige a los más antiguos";
  - "Un paciente con dos entradas compatibles recibe un solo aviso";
  - "El paciente repetido no ocupa un lugar del tope";
  - "No se avisa por un hueco que ya empezó";
  - además, el desempate por `id` y un tope de 0.

## 2. Persistencia y aplicación

- [ ] 2.1 Agregar `ListaEsperaPersistencia.candidatas(profesionalId, fecha)` (design D4). Se verifica con Testcontainers:
  - trae solo entradas `ACTIVA`, del profesional o sin preferido, con la fecha dentro del rango, incluidos los extremos;
  - las devuelve ordenadas por antigüedad.
- [ ] 2.2 Crear `BuscadorCompatibles` con `compatibles(Hueco)` y `aAvisar(Hueco)` (design D5). Se verifica con Testcontainers:
  - filtra por duración, por `realiza` y por prestación inactiva;
  - `aAvisar` aplica el tope de la configuración y la deduplicación;
  - funciona sin usuario autenticado.

## 3. API y acceso

- [ ] 3.1 Agregar `ServicioListaEspera.compatibles(...)` y `GET /api/lista-espera/compatibles` (design D5 y D6). Se verifica con integración:
  - "Recepción ve solo las entradas compatibles";
  - "Las sugerencias se ordenan por antigüedad y muestran todas las entradas del paciente";
  - "Un hueco que ya empezó no tiene sugerencias";
  - "La consulta rechaza un hueco inválido";
  - "La consulta rechaza un profesional inexistente o inactivo".
- [ ] 3.2 Agregar el matcher en `SecurityConfig` y `@PreAuthorize` (design D7). Se verifica con:
  - "El odontólogo no consulta las entradas compatibles";
  - "Sin sesión no se consultan las entradas compatibles";
  - un test que llama al servicio como odontólogo y espera `AccessDeniedException`.

## 4. Verificación final

- [ ] 4.1 Correr la suite completa del backend (`./mvnw verify`).
- [ ] 4.2 Escribir `verify.md` con el veredicto y la trazabilidad entre escenarios y tests.
- [ ] 4.3 Actualizar el estado de C-31 en `CHANGES.md`.
