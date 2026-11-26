# Design

## Context

El motivo está en proposal.md. En el backend hay 160 clases de prueba. Las de integración (`*IT`, Failsafe) heredan de `PruebaIntegracionBase`, que da un contexto completo con PostgreSQL 16 en Testcontainers, MockMvc y CSRF real. Para no levantar contextos (y contenedores) de más, las clases importan configuraciones compartidas en lugar de `@MockitoBean`. Lo que se reutiliza:

- `RelojFijoConfig`: el reloj de la app queda en martes 1/12/2026 09:00 de Buenos Aires.
- `InMemoryNotificacionAdapter`: captura los emails enviados, con su tipo, destinatario y cuerpo.
- Los armadores de datos ficticios `DatosAgenda` y `DatosFicticios`.
- La limpieza por ids creados en `@AfterEach`, que sigue el patrón de `EscenarioTomaOfertaIT`.

Las pruebas de arquitectura (`ArquitecturaTest`, ArchUnit) son unitarias y corren con Surefire. `EscriturasOdontologoIT` ya descubre las rutas de escritura con `RequestMappingHandlerMapping`. Un relevamiento rápido encontró una sola dependencia entre módulos sobre `infrastructure`: `listaespera.asignacion.application.AsignarHuecoService` usa `agenda.infrastructure.ConsultaOcupacionDisponibilidad`, que no es entidad ni repositorio. No se encontró ningún uso de entidades JPA de otro módulo.

## Goals / Non-Goals

**Goals:**
- Que cada recorrido de `specs/recorrido-turno` sea una prueba por HTTP que cruza módulos sin atajos. El token del turno y el de la oferta se leen del email capturado, igual que haría el paciente.
- Que la matriz de permisos se declare una sola vez, como tabla, y que una ruta nueva sin fila haga fallar la build.
- Que la matriz RN→test se pueda comprobar con un script, no solo leyéndola.

**Non-Goals:**
- Pruebas de navegador del frontend, porque la ficha excluye `frontend/src`.
- Medir rendimiento o cobertura mínima: JaCoCo sigue solo como informe.
- Reescribir pruebas por módulo que ya cubren una regla; la matriz las cita.

## Decisions

1. **Recorridos en un paquete propio `ar.edu.utn.turnos.recorridos`** y no repartidos por módulo. Son pruebas transversales: ubicarlas en un módulo sugeriría una dependencia que no existe. Alternativa descartada: alargar `EscenarioTomaOfertaIT`, que inserta las ofertas directo en la base y saltea el aviso real.

2. **Token desde el email capturado.** El recorrido toma el enlace del cuerpo del email de confirmación o de aviso con una expresión regular sobre la ruta pública. Así se verifica de paso que el email lleva un enlace usable. Alternativa descartada: insertar un token conocido, que no prueba la emisión.

3. **Datos propios por prueba y limpieza por ids**, como en los escenarios existentes, con un profesional, un sillón y una prestación nuevos. Los sillones activos ajenos se desactivan durante la prueba para que la elección del sillón sea determinista. No se usa el seed: el seed es del perfil `dev`, y depender de él acoplaría la prueba a sus datos.

4. **Matriz de permisos como tabla en la prueba** (`MatrizPermisosIT`): cada fila es método + patrón de ruta + rol + resultado esperado (`PERMITIDO` o `403`). La prueba:
   - lista las rutas registradas del personal y falla si alguna no tiene fila;
   - verifica `403` sin cambio de filas para lo denegado;
   - para lo permitido solo exige que no sea `401` ni `403`, porque el cuerpo `{}` o los ids ficticios pueden dar `400`, `404` o `422`, y eso ya lo cubren las pruebas de cada módulo;
   - comprueba `401` sin sesión.
   `EscriturasOdontologoIT` se conserva porque cubre su escenario de C-28. Alternativa descartada: generar la matriz desde las anotaciones `@PreAuthorize`, que probaría el código contra sí mismo y no contra KB 03.

5. **Regla de módulos con ArchUnit por raíz de paquete.** Los módulos son los paquetes de primer nivel bajo `ar.edu.utn.turnos` (`agenda`, `configuracion`, `listaespera`, `pacientes`, `prestaciones`, `profesionales`, `reservapublica`, `seguridad`). `shared` queda fuera, como módulo común. La regla prohíbe depender de clases de otro módulo anotadas con `@Entity` o asignables a `Repository` de Spring Data. Se escribe como condición propia sobre la raíz del paquete, sin `allowEmptyShould`. Si la regla destapa una violación hoy no detectada, se informa antes de tocar código de producción.

6. **Matriz RN→test en Markdown con verificación por script.** `docs/matriz-rn-tests.md` tiene una fila por RN, con la spec y el escenario, y la clase y el `@DisplayName` (o el `it(...)`) que la verifican. Un script de shell versionado en `docs/` (o `scripts/`) comprueba tres cosas: que estén las 38 RN, que ninguna fila tenga la celda de test vacía y que cada nombre de escenario citado aparezca literalmente en un archivo de test. Se mapea por nombre de escenario y no por el comentario "RN-XX", porque hay reglas (RN-07, RN-08, RN-14, RN-19, RN-28, RN-29, RN-34) que no citan su código en los tests aunque están cubiertas.

7. **Defectos que aparezcan.** Si una prueba nueva falla por un defecto de negocio (MEDIUM), se arregla en este change con el test en rojo primero y se muestra la decisión. Si falla por permisos (CRITICAL), se frena, se analiza y se propone el arreglo, que espera aprobación explícita. En ningún caso se ajusta la expectativa de la prueba para que pase sin justificarlo en la matriz y en este design.

## Risks / Trade-offs

- [Los recorridos comparten el contexto y la base con el resto de la suite; datos de otras clases pueden alterar la selección de la lista de espera] → Profesional y prestación nuevos por prueba: la compatibilidad exige el mismo profesional, así que las entradas ajenas no compiten.
- [El límite de tasa de la API pública puede cortar un recorrido con varios pedidos] → Cada pedido público usa una IP distinta, como en los escenarios de C-42.
- [El aviso a la lista de espera es asíncrono en producción: un ejecutor propio lo corre después del commit] → En el perfil `test` ese ejecutor es sincrónico, así que al volver la cancelación o la reprogramación las ofertas y los emails ya están y la prueba no necesita esperar. Los avisos al paciente (cancelación y reprogramación) también salen después del commit, por eso los recorridos confirman las transacciones y limpian lo que crean.
- [La matriz de permisos con rutas parametrizadas usa ids ficticios (`1`); una ruta permitida puede dar `404`] → Lo permitido solo exige "ni 401 ni 403" (decisión 4).
- [El script de la matriz RN→test depende de nombres literales; renombrar un escenario la rompe] → Es buscado: obliga a mantener la matriz al día, y el error indica qué fila corregir.

## Migration Plan

No hay migración ni cambios de esquema. Se suman pruebas, documentos y un script. Para volver atrás alcanza con revertir el PR.

## Hallazgos de la implementación

- **Matriz de permisos sin huecos (2026-11-26).** `MatrizPermisosIT` recorre las 48 rutas del personal con los tres roles y sin sesión, y coincide con KB 03 §RBAC. Hay tres comportamientos que no son defectos y que la prueba tiene en cuenta:
  - Al odontólogo sin profesional vinculado se le niega la agenda y la lectura de profesionales (403), porque lo que ve depende de su profesional (RN-33). La prueba usa un odontólogo ficticio vinculado, y las filas que dependen de que el recurso sea propio las cubren las pruebas de titularidad de cada módulo.
  - `GET /api/agenda/disponibilidad` valida los parámetros antes de autorizar, como pide su spec. Sin parámetros responde 400 a cualquier rol, así que la prueba manda parámetros válidos; con ellos, solo recepción y administración pasan, y el odontólogo recibe 403.
  - En las escrituras de catálogo (profesionales, sillones y prestaciones), un cuerpo inválido de recepción da 400 antes que 403: la validación del cuerpo corre antes que el chequeo de rol del servicio. No cambia datos ni expone información, así que no se toca en este change (permisos es dominio CRITICAL). La prueba manda cuerpos válidos para llegar al chequeo de rol, y ahí recepción recibe 403.
- **Reglas de arquitectura.** El código cumple las dos reglas. Las clases de ejemplo que las violan están en `src/test/java/ejemplo/arquitectura`, fuera de `ar.edu.utn.turnos`, para que Spring no las escanee como entidades ni repositorios en las pruebas de integración.
- **Recorridos sin defectos.** Los cuatro recorridos y los dos escenarios nuevos de `oferta-hueco` pasaron sin tocar código de producción. La reprogramación ya avisaba a la lista de espera por cada tramo liberado (C-35 + C-36); faltaban la spec y el test. Las pruebas son de caracterización: cubren comportamiento que ya existía.
