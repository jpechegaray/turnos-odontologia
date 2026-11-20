# Preguntas Abiertas

Este archivo reúne: (1) las inconsistencias detectadas, (2) las preguntas abiertas del Discovery (`discovery.md` §11) y las derivadas del diseño, priorizadas, y (3) los datos de mercado "No evidenciado" o dudosos del informe, agrupados.

**Suposición:** los nombres de los changes de la columna "Bloquea" son provisionales (`auth`, `catalogos`, `agenda-turnos`, `reserva-publica`, `lista-espera`, `notificaciones`, `configuracion`); el roadmap definitivo los fija después. "Ninguno" significa que no impide escribir código: afecta al informe o a decisiones futuras.

## Inconsistencias detectadas

### IN-01 — Precio de DentalCore con dos valores
**Informe (A.1) dice**: $75.000/mes en ARS con 2 usuarios (+$15.000 por usuario) en la página de precios, y USD 50/mes en la FAQ del mismo sitio.
**Verificación de ejemplo (informe, sección "Verificación de fuentes") dice**: el precio se muestra geolocalizado (en esa prueba, COP 167.000 desde Colombia), lo que explicaría la discrepancia. Esa fila es un **ejemplo del agente, no una verificación del equipo**.
**Impacto**: el posicionamiento de precio frente a DentalCore podría estar mal; no afecta al producto.
**Resolución propuesta**: que un integrante lo verifique personalmente desde Argentina y deje constancia (ver PA-07).

### IN-02 — RN-05 habla de "cancelar o reprogramar" pero solo define cancelación tardía
**Discovery (RN-05) dice**: "Cancelar o reprogramar no tiene penalidad hasta 24 hs antes... Fuera de ese plazo, la cancelación queda registrada como tardía."
**Falta**: qué ocurre con una reprogramación fuera de plazo (¿se permite, se registra, se bloquea?) y si el paciente puede reprogramar. RN-07 solo da al token ver, confirmar y cancelar.
**Impacto**: define el comportamiento de `ReprogramarTurno` y del campo `reprogramado_tarde`.
**Resolución propuesta**: permitirla al personal y registrarla (SU-07, RN-18).
**Estado: resuelta en parte (2026-11-20, decisión del líder)**: el paciente **no** reprograma con su token, solo cancela (RN-18, RN-19); reprograma solo recepción. Sigue pendiente de confirmar la política de la reprogramación tardía del personal (PA-06).

### IN-03 — Paciente "sin cuenta" frente a la necesidad de una ficha
**Discovery dice**: el paciente no tiene login ni cuenta (§2), pero RN-08 y los casos de uso exigen que reserve un turno, que requiere un paciente asociado (ficha con contacto).
**Impacto**: hay que decidir qué datos se piden y cómo se evitan duplicados o la modificación de fichas ajenas.
**Resolución propuesta**: RN-26 y RN-28.
**Estado: resuelta (2026-11-20, decisión del líder)**: la reserva online pide nombre completo, email, teléfono y obra social, sin DNI; los duplicados se evitan por email (y teléfono si coincide), reutilizando la ficha sin modificar sus datos (RN-26, RN-28, SU-04).

### IN-04 — La verificación de fuentes del informe es un ejemplo del agente
**Informe dice** (en su propia sección de verificación): las 6 filas fueron completadas por el agente "solo como modelo de redacción"; el equipo debe verificar personalmente y reemplazarlas. El estado del proyecto lo refleja con `source_verification: example-by-agent-pending-team`.
**Impacto**: las afirmaciones de mercado que respaldan DD-09 a DD-14 no están verificadas por el equipo; la cátedra exige verificación personal.
**Resolución propuesta**: cada integrante verifica y firma al menos una fila (PA-07).

### IN-05 — Recuento de sillones: qué sistemas son "indirectos"
**Informe C.3.1 dice**: 7 sistemas muestran gestión por sillón, "los dos primeros de forma indirecta" (DriCloud y Open Dental).
**Informe D.3 dice**: DriCloud, Open Dental y **Dentally** son indirectos (tres).
**Impacto**: bajo; el total de 7 coincide, pero cambia cuántos se apoyan en evidencia indirecta.
**Resolución propuesta**: unificar el criterio al revisar el informe.

### IN-06 — La restricción de no contactar proveedores frente a la recomendación de demos
**Discovery §9 dice**: no se crean cuentas ni se contacta a los proveedores analizados.
**Informe D.1 recomienda**: demos y pruebas gratuitas de cinco competidores (algunas requieren registro o solicitud por WhatsApp).
**Impacto**: no se pueden ejecutar las demos recomendadas sin violar la restricción o sin ajustarla.
**Resolución propuesta**: tratar D.1 como siguiente paso opcional condicionado a que el líder y la cátedra lo autoricen; mientras tanto, no se hacen.
**Estado: resuelta (2026-11-20, decisión del líder)**: el análisis "mediante demo" del informe se hace **solo con demos públicas o videos oficiales, sin crear cuentas ni contactar proveedores**. Así se respeta la restricción de `discovery.md` §9 y no hace falta ajustarla ni pedir autorización adicional (ver NE-22).

### IN-07 — Rango de precios argentinos del Discovery
**Discovery §4 dice**: los precios argentinos publicados van de unos $20.000 a $75.000 por mes.
**Informe (A.1) muestra**: Medicloud PRO a $18.400 (por debajo del rango) y un bot de WhatsApp a $150.000/mes (por encima); además DentalSoft publica "$" sin código de moneda.
**Impacto**: bajo; solo afecta el texto de posicionamiento.
**Resolución propuesta**: reformular el rango aclarando qué planes incluye.

### IN-08 — Relevancia de Dentalink para Argentina
**Informe (A.1) dice**: relevancia Alta.
**Verificación de ejemplo dice**: el módulo para Argentina existe pero el cuerpo de los artículos es un video y no aparece ARCA; sugiere bajarla a Media. Además, la empresa figura en Chile.
**Impacto**: bajo; cambia el ranking de relevancia, no el producto.
**Resolución propuesta**: revisar tras la verificación del equipo (PA-07).

### IN-09 — Stack "libre, lo propone kb-creator" frente a la decisión del equipo (resuelta)
**Discovery §9 dice**: el stack es libre y lo propone `kb-creator`.
**Realidad**: el equipo ya decidió el stack (DD-01, DD-08). Se documenta como resuelta; no hay acción.

## Preguntas abiertas (priorizadas)

### Prioridad alta

| ID | Pregunta | Origen | Bloquea | Decisor |
|---|---|---|---|---|
| PA-01 | **Resuelta (2026-11-20, decisión del líder).** ¿Cuánto tiempo es válido el token y qué datos mínimos pide la reserva online? ¿Cómo se evitan duplicados de pacientes? **Respuesta:** el token vale desde la reserva hasta el inicio del turno o su cancelación (lo que ocurra primero), hasheado y de un solo turno (RN-25, DD-06); la reserva pide nombre completo, email, teléfono y obra social (texto libre), **sin DNI** (RN-26); duplicados por email y teléfono si coincide (RN-28, SU-04) | `discovery.md` §11, IN-03 | Ninguno (resuelta) | Líder |
| PA-02 | **Resuelta (2026-11-20, decisión del líder).** La lista de espera, ¿notifica sola al paciente o la recepción elige? **Respuesta:** avisa sola por email a todos los pacientes compatibles, cada uno con un enlace con token; el primero que confirma se queda con el turno y si nadie confirma antes del inicio del hueco queda libre (RN-23, RN-24, RN-38, DD-10, SU-05; detalles en SU-14) | `discovery.md` §11 | Ninguno (resuelta) | Líder |
| PA-03 | **Resuelta (2026-11-20, decisión del líder).** ¿El odontólogo puede dar turnos o solo los ve? **Respuesta:** es solo lectura: ve su agenda y la ficha básica de sus pacientes y no modifica nada; asistencia → recepción; horarios y bloqueos → recepción y administrador (RN-20, RN-33, SU-02, matriz en `03`) | `discovery.md` §11 | Ninguno (resuelta) | Líder |
| PA-04 | **Resuelta (2026-11-20, decisión del líder).** ¿Una prestación puede requerir un tipo específico de sillón? **Respuesta:** no; cualquier sillón libre sirve y no se modelan tipos de sillón (RN-11, SU-03) | `discovery.md` §11 | Ninguno (resuelta) | Líder |
| PA-06 | **Resuelta en parte (2026-11-20, decisión del líder).** ¿Qué pasa al reprogramar fuera de plazo? ¿El paciente puede reprogramar por token? **Respuesta:** el paciente **no** reprograma, solo cancela (RN-18, RN-19) y reprograma solo recepción. **Sigue abierto:** si la reprogramación tardía del personal se permite, se registra o se penaliza (hoy: se permite y se registra, SU-07) | IN-02 | `agenda-turnos` (solo la política de reprogramación tardía) | Equipo / líder |
| PA-07 | ¿Quién verifica personalmente las fuentes del informe y cuándo? Cada integrante debe comprobar y reemplazar las filas de ejemplo | IN-04, requisito de la cátedra | Ninguno (entrega del informe) | Líder del equipo |

### Prioridad media

| ID | Pregunta | Origen | Bloquea | Decisor |
|---|---|---|---|---|
| PA-05 | ¿Con cuánta anticipación se manda el recordatorio (24 hs, 48 hs, configurable)? ¿Uno solo o varios? (hoy: 24 hs configurable, uno) | `discovery.md` §11 | `notificaciones` | Equipo |
| PA-08 | Ley 25.326: ¿el consultorio debe inscribir la base de datos ante la AAIP y qué texto de consentimiento y política de retención se muestra al paciente? | `discovery.md` §10 | `reserva-publica` (texto de consentimiento) | Equipo / cátedra |
| PA-09 | En la reserva online, ¿el paciente puede elegir "cualquier profesional"? ¿Cómo se asigna el sillón? (hoy: elige profesional y el sistema asigna el sillón, SU-03) | Flujo 4 | `reserva-publica` | Equipo |
| PA-13 | ¿Qué política se aplica a los pacientes con ausencias reiteradas (AUSENTE)? ¿Se limitan sus reservas online? | RN-20 | Ninguno (futuro) | Equipo |
| PA-14 | ¿Hay turnos que pueden requerir más de un profesional o más de un sillón a la vez (por ejemplo, con asistente)? El modelo actual asume uno de cada uno | RN-01 | `agenda-turnos` | Equipo |

### Prioridad baja

| ID | Pregunta | Origen | Bloquea | Decisor |
|---|---|---|---|---|
| PA-10 | Si el proyecto pasa a producción, ¿se adopta Flyway (o Liquibase) en lugar de `ddl-auto`? `update` no borra ni renombra columnas y no versiona cambios | DD-08 | Ninguno (futuro) | Equipo |
| PA-11 | ¿Se necesita una bitácora de auditoría completa (quién modificó qué) más allá de los campos actuales? | Seguridad | Ninguno (futuro) | Equipo |
| PA-12 | ¿Se necesita bloquear un sillón por un intervalo (mantenimiento) además de desactivarlo? (hoy: solo por profesional) | SU-08 | `catalogos` | Equipo |
| PA-15 | ¿Se necesita compartir la sesión entre instancias o más de un backend? (hoy: una instancia) | SU-10 | Ninguno (futuro) | Equipo |
| PA-16 | ¿Los pacientes pueden quedar sin email? Hoy se exige email o teléfono, y sin email no hay recordatorio ni se los puede anotar en la lista de espera (RN-21); la reserva online sí exige email (RN-26) | RN-32 | `notificaciones` | Equipo |

## Datos de mercado "No evidenciado" o dudosos (del informe)

Ninguno bloquea código. Se agrupan para que el equipo los resuelva con verificación personal o los deje explícitos como límites del análisis. Las fichas de `docs/discovery/sources/` pueden consultarse para un dato puntual.

### Marcados como no evidenciados en `discovery.md` §11

| ID | Dato | Prioridad |
|---|---|---|
| NE-01 | Precios en ARS de los sistemas extranjeros | Baja |
| NE-02 | Alcance real de la integración ARCA de DriCloud (solo afirmación comercial) | Baja |
| NE-03 | Si Open Dental considera a Argentina dentro de su tarifa "Developing Countries" (gratis) u "Other Countries" (US$89/mes); su página no menciona a Argentina | Media |
| NE-04 | Precio de DentalCore (dos valores que no coinciden, IN-01) | Media |
| NE-05 | Contenido de Doctoralia AR y de Gesden, cuyos sitios no se pudieron leer (Gesden se analizó solo con terceros: ficha de baja confianza) | Baja |

### Agenda y reglas de negocio de los competidores

| ID | Dato | Prioridad |
|---|---|---|
| NE-06 | Detalle de agenda: solapamientos, sobreturnos, duración variable y bloqueos casi no se evidencian (solo 2 de 23 declaran prevención de solapamientos) | Media |
| NE-07 | Política de cancelación con plazo configurable y registro de tardías: no aparece en ninguna ficha | Media |
| NE-08 | Reserva online del paciente en DentalCore, DentalTec y Dentia (Dentia ofrece un bot de WhatsApp, no reserva propia) | Media |
| NE-09 | Si alguna lista de espera filtra por compatibilidad (ninguna ficha lo describe) | Media |
| NE-10 | Qué sistemas distintos de Open Dental comprueban reserva online y lista ASAP en funcionamiento (las demás son afirmaciones comerciales) | Baja |

### Integraciones y cumplimiento

| ID | Dato | Prioridad |
|---|---|---|
| NE-11 | ARCA, validación en línea de obras sociales y WhatsApp: ninguno comprobado (5 declaran ARCA; solo DentalTec declara validación en línea) | Baja |
| NE-12 | Contenido real del módulo "exclusivo para Argentina" de Dentalink (solo se leyeron los títulos; el cuerpo es un video) | Baja |
| NE-13 | Cumplimiento de la Ley 25.326: solo Dentia lo declara; DentalCore es inconsistente entre páginas; DriCloud aloja datos en Europa | Media |
| NE-14 | Exportación de datos: poco evidenciada | Baja |

### Precios, origen y calidad de datos

| ID | Dato | Prioridad |
|---|---|---|
| NE-15 | Precios no publicados (Dentalink, DentalTec el software, Curve Dental, Docturno, Dentrix Ascend, Dentidesk, Medilink, Calu el monto mensual; la página de planes de AgendaPro devolvió 404) | Baja |
| NE-16 | Moneda no explícita ("$" sin código) en DentalSoft, Encuadrado, Calu y Dentrix Ascend | Baja |
| NE-17 | País de origen no declarado en Calu, Medilink y AgendaPro; razón social de DriCloud, Nubimed, Curve Dental y otros | Baja |
| NE-18 | Si Medicloud, Calu, Docturno, Medilink, Encuadrado y AgendaPro atienden odontología (no evidenciado; sirven solo como referencia de agenda) | Baja |
| NE-19 | Discrepancias a verificar: Clinicorp (R$ 149,90 en la landing frente a R$ 159,90 en planes), Simples Dental (montos mensuales sin modalidad), DriCloud (510 €/año frente a 12 × 42 € = 504 €), Dentrix Ascend (uptime 99,99 % frente a 99,99987 %) | Baja |
| NE-20 | Datos obtenidos solo de buscadores (Capterra, prensa de Doctocliq); contadores "0+" en Clinic Cloud y DentalTec que no renderizaron; tabla de planes de Dentally parcialmente incierta; WhatsApp de Dentidesk anunciado como "próximamente" | Baja |
| NE-21 | Adopción real: casi no hay reseñas verificables; las cifras de clientes son afirmaciones comerciales | Baja |
| NE-22 | Ningún producto fue probado por el equipo con cuenta propia ni contactando proveedores; el análisis "mediante demo" se limita a demos públicas o videos oficiales (IN-06, resuelta 2026-11-20) | Media |
