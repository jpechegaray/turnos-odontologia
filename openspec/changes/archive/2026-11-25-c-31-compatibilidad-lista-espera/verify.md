# Verify — c-31-compatibilidad-lista-espera (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes; 22 de 22 escenarios cubiertos; sin desvíos del design)

## Validación estructural

`openspec validate c-31-compatibilidad-lista-espera --strict` → `Change 'c-31-compatibilidad-lista-espera' is valid`.

## Tareas

9/9 completas. La 4.2 se completa con esta verificación. Las del `## Workflow follow-up` (PR, merge, archivo y CHANGES.md) quedan fuera del seguimiento, como define el schema.

## Matriz escenario → test

Spec `lista-espera` (delta): 5 requisitos y 22 escenarios. Las rutas son relativas a `backend/src/test/java/ar/edu/utn/turnos/listaespera/`.

| Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|
| Una prestación de 90 minutos no cabe en un hueco de 30 | Cubierto | domain/CompatibilidadListaEsperaTest.java:43 | También por la API (web/ListaEsperaCompatiblesApiIT.java:101) |
| Una prestación que dura exactamente el hueco es compatible | Cubierto | domain/CompatibilidadListaEsperaTest.java:52 | Con 31 minutos ya no cabe |
| El profesional del hueco debe realizar la prestación | Cubierto | domain/CompatibilidadListaEsperaTest.java:62 | Con la base: application/BuscadorCompatiblesIT.java:66 |
| Una prestación inactiva no es compatible | Cubierto | domain/CompatibilidadListaEsperaTest.java:69 | Ídem |
| Se respeta el profesional preferido | Cubierto | domain/CompatibilidadListaEsperaTest.java:76 | También por la API |
| Una entrada sin profesional preferido acepta a cualquiera que realice la prestación | Cubierto | domain/CompatibilidadListaEsperaTest.java:86 | |
| Una entrada que no está activa no es compatible | Cubierto | domain/CompatibilidadListaEsperaTest.java:94 | Parametrizado: `ASIGNADA`, `CANCELADA` y `VENCIDA` |
| La fecha del hueco debe estar dentro del rango de la entrada | Cubierto | domain/CompatibilidadListaEsperaTest.java:107 | Los dos extremos incluidos; la consulta JPQL en infrastructure/CandidatasListaEsperaIT.java:27 |
| La fecha se toma en hora de Buenos Aires | Cubierto | domain/CompatibilidadListaEsperaTest.java:122 | 22:00 en Buenos Aires es el día siguiente en UTC |
| El turno resultante debe caer dentro de la franja | Cubierto | domain/CompatibilidadListaEsperaTest.java:133 | Además, cruzar la medianoche queda fuera (línea 156) |
| Una entrada sin franja acepta cualquier hora | Cubierto | domain/CompatibilidadListaEsperaTest.java:149 | |
| El tope de avisos elige a los más antiguos | Cubierto | domain/SelectorDeAvisosTest.java:29 | Con el tope de la configuración: application/BuscadorCompatiblesIT.java:78 |
| Un paciente con dos entradas compatibles recibe un solo aviso | Cubierto | domain/SelectorDeAvisosTest.java:42 | |
| El paciente repetido no ocupa un lugar del tope | Cubierto | domain/SelectorDeAvisosTest.java:53 | También con la base (BuscadorCompatiblesIT.java:78) |
| No se avisa por un hueco que ya empezó | Cubierto | domain/SelectorDeAvisosTest.java:65 | Borde exacto y un milisegundo antes; con la base: BuscadorCompatiblesIT.java:101 |
| Recepción ve solo las entradas compatibles | Cubierto | web/ListaEsperaCompatiblesApiIT.java:101 | `ADMIN` y `RECEPCION` |
| Las sugerencias se ordenan por antigüedad y muestran todas las entradas del paciente | Cubierto | web/ListaEsperaCompatiblesApiIT.java:118 | Sin tope ni deduplicación |
| Un hueco que ya empezó no tiene sugerencias | Cubierto | web/ListaEsperaCompatiblesApiIT.java:130 | Inicio igual a "ahora" y anterior |
| La consulta rechaza un hueco inválido | Cubierto | web/ListaEsperaCompatiblesApiIT.java:143 | Faltantes, mal formados, no numérico, `fin` igual y anterior |
| La consulta rechaza un profesional inexistente o inactivo | Cubierto | web/ListaEsperaCompatiblesApiIT.java:160 | `422` con su código |
| El odontólogo no consulta las entradas compatibles | Cubierto | web/ListaEsperaCompatiblesApiIT.java:172 | `403` también sin parámetros; `@PreAuthorize` se prueba aparte (línea 191) |
| Sin sesión no se consultan las entradas compatibles | Cubierto | web/ListaEsperaCompatiblesApiIT.java:183 | |

**Resumen:** 22 Cubiertos, 0 Parciales y 0 Sin test.

## Ejecución de tests

| Comando | Resultado |
|---|---|
| `./mvnw verify` (backend, Testcontainers con PostgreSQL 16) | `BUILD SUCCESS`. Unitarios: 265 de 265. Integración: 487 de 487. 0 fallidos y 0 saltados |

C-31 no toca el frontend.

**Pruebas de mutación durante el apply.** Se rompió el código a propósito y después se restauró.

| Mutación | Resultado |
|---|---|
| Sin matcher en `SecurityConfig` y sin `@PreAuthorize` en el servicio | Fallan `odontologo` (200 en lugar de 403) y `servicioRechazaAlOdontologo` |

## Reglas de negocio y reglas duras

- **RN-22:** las cinco condiciones como función pura, sin Spring ni base, con los bordes de duración, fechas y franja.
- **RN-23 y SU-14:** antigüedad con desempate por `id`, un aviso por paciente antes del tope y nada si el hueco empezó.
- **SU-15:** recepción ve todas las compatibles, sin tope.
- **Regla dura 4:** el odontólogo recibe 403 en el filtro y en el servicio.
- **Regla 6:** `Instant`, `Clock` inyectable y fecha y franja en `America/Argentina/Buenos_Aires`.
- **Regla 9:** la respuesta reutiliza el DTO `RespuestaEntrada`; JPQL parametrizado.
- **Regla 10:** Testcontainers, sin H2.
- **Regla 11:** los 22 escenarios tienen `@DisplayName` con su nombre exacto.
- **Datos ficticios:** fixtures de `DatosFicticios` y `DatosAgenda`.

## Diferencias con design.md

Ninguna.

## Para los changes siguientes

- **C-36:** escuchar `HuecoLiberado` con `AFTER_COMMIT`, convertirlo a `new Hueco(profesionalId, hueco.inicio(), hueco.fin())` y llamar a `BuscadorCompatibles.aAvisar(hueco)`, que no exige sesión.
- **C-43:** antes de asignar, comprobar la compatibilidad con `BuscadorCompatibles.compatibles(hueco)` y crear el turno con todas las validaciones de C-19.
- **C-47:** consumir `GET /api/lista-espera/compatibles` con `inicio` y `fin` en ISO-8601 con zona.

## Qué NO se pudo verificar

- C-31 no tiene UI (`Decision: no (no UI)`), así que no hay verificación visual.
- La compatibilidad no comprueba que el hueco esté libre ni dentro del horario (design, Non-Goals): lo garantiza la creación del turno en C-36 y C-43.

## Próximo paso

PR de `juan-pablo-c-31` a `master` con "Closes #31". Con el CI en verde, merge y `/opsx:archive c-31-compatibilidad-lista-espera`.
