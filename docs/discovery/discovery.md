# Discovery — Turnos Odontología

> Resultado estructurado de `discovery-research` (checklist de 11 puntos), confirmado por el líder del equipo el 2026-11-20.
> Insumos: tres rondas de preguntas con el equipo y la investigación de mercado de 23 sistemas en [`sources/`](sources/). El análisis comparativo completo (tablas A–D) está en [`informe-discovery.md`](informe-discovery.md).

## 1. Problema que resuelve

Los consultorios odontológicos argentinos de 2 a 5 profesionales necesitan una agenda que garantice que no haya turnos superpuestos, ni por profesional ni por sillón o box, y que les permita recuperar los huecos que deja una cancelación. Hoy tienen dos opciones:

- **Sistemas locales:** son jóvenes y fragmentados (DentalCore, Dentia, DentalSoft, DentalTec), sin reseñas verificables y con poca evidencia pública de cómo funcionan.
- **Sistemas extranjeros:** están maduros (Open Dental, Dentally, Curve), pero no tienen adaptación evidenciada a Argentina.

El producto se enfoca en la **agenda y los turnos**, no en la gestión clínica integral.

## 2. Usuarios / roles

| Rol | Verbo principal | Notas |
|---|---|---|
| Odontólogo | **Consulta** su agenda | Es el eje del producto. Ve su agenda diaria y semanal y la ficha básica de sus pacientes, en **solo lectura**: no modifica nada. Si necesita un cambio, se comunica con recepción (decisión del líder, 2026-11-20). |
| Recepción | **Agenda** turnos | Su uso tiene que ser lo más simple posible. Da, reprograma y cancela turnos de todos los profesionales; marca asistencia o ausencia; gestiona horarios y bloqueos junto con el administrador. |
| Administrador | **Configura** el consultorio | Prestaciones y sus duraciones, sillones o boxes, profesionales, usuarios y plazo de cancelación. |
| Paciente | **Reserva** online | No tiene login ni cuenta. Queda representado por un **token** (enlace único) con el que ve, confirma o cancela su turno. |

## 3. Casos de uso

1. Como **recepción**, quiero crear un turno sin que se superponga con otro del mismo profesional o del mismo sillón, para no dar dos turnos al mismo tiempo.
2. Como **recepción**, quiero reprogramar o cancelar un turno y liberar el horario, para mantener la agenda al día.
3. Como **paciente**, quiero reservar un turno online desde un enlace público y ver o cancelar mi turno con mi token, sin crear una cuenta (el paciente no reprograma).
4. Como **odontólogo**, quiero ver mi agenda del día y de la semana, por profesional y por sillón, en solo lectura.
5. Como **paciente en lista de espera**, quiero que el sistema me avise solo por email cuando se libere un hueco compatible, para tomarlo si soy el primero en confirmar.
6. Como **paciente**, quiero recibir un recordatorio por email antes de mi turno.

## 4. Competidores / soluciones existentes

Se relevaron 23 sistemas (fecha de consulta: 2026-11-20). Las fichas están en [`sources/`](sources/).

- **Argentina (7):** DentalCore, Dentia, DentalSoft, DentalTec, Medicloud, Calu y Docturno.
- **Latinoamérica (8):** Dentalink, Simples Dental, Clinicorp, Doctocliq, Medilink, Encuadrado, AgendaPro y Dentidesk.
- **Internacionales (8):** DriCloud/XDentalCloud, Open Dental, Nubimed, Dentally, Curve Dental, Dentrix Ascend, Clinic Cloud y Gesden.

**Hallazgos que orientan el producto:**

- Ningún sistema argentino relevado evidencia a la vez **agenda por sillón o box, lista de espera y reserva online completa del paciente**. Ese es el hueco que ataca el MVP.
- Dentalink es el único de la región con un módulo de ayuda específico para Argentina (obras sociales y receta electrónica). DriCloud es el único internacional que declara facturación ARCA y Mercado Pago. En los dos casos es afirmación comercial.
- WhatsApp es estándar, pero casi todos los proveedores lo cobran aparte. Los precios argentinos publicados van de unos $20.000 a $75.000 por mes.
- Casi toda la evidencia pública es **afirmación comercial**. Lo comprobado se limita a precios publicados, documentación técnica (por ejemplo, la API de Open Dental) y algunos centros de ayuda.

## 5. Funcionalidades necesarias (MVP)

- Agenda por profesional y por sillón o box, con **prevención de solapamientos**.
- Duración del turno según la prestación, ajustable al darlo.
- Horarios de atención y bloqueos por profesional.
- Crear, reprogramar y cancelar turnos.
- Reserva online del paciente sin login, con token.
- Ficha básica de paciente: datos de contacto, obra social como dato y notas.
- Recordatorio por email (SMTP real).
- Roles odontólogo, recepción y administrador.
- **Diferenciador:** lista de espera que avisa sola por email a los pacientes compatibles cuando se libera un hueco; el primero que confirma se queda con el turno.

## 6. Funcionalidades opcionales (etapas posteriores)

- Odontograma, periodontograma e historia clínica completa.
- WhatsApp (recordatorios y confirmaciones).
- Mercado Pago y señas.
- Facturación electrónica ARCA.
- Validación online de obras sociales y prepagas.
- Multisede, liquidaciones y reportes avanzados.
- Campañas y recuperación de pacientes.
- Asistentes con IA.

## 7. Reglas de negocio

- **RN-01 — Sin sobreturnos.** Un profesional no puede tener dos turnos superpuestos, y un sillón o box tampoco.
- **RN-02 — Borde no solapado.** Un turno que empieza exactamente cuando termina otro **no** se considera solapado (los intervalos son semiabiertos: `[inicio, fin)`).
- **RN-03 — Duración por prestación.** Cada prestación tiene una duración por defecto, que se puede ajustar al dar el turno.
- **RN-04 — Horario y bloqueos.** No se puede dar un turno fuera del horario de atención del profesional ni dentro de un bloqueo.
- **RN-05 — Plazo de cancelación.** Cancelar o reprogramar no tiene penalidad hasta 24 hs antes del turno; el plazo lo configura el administrador. Fuera de ese plazo, la cancelación queda registrada como **tardía**.
- **RN-06 — Cancelar libera el horario.** El hueco liberado vuelve a estar disponible y se ofrece a la lista de espera.
- **RN-07 — Alcance del token.** El token del paciente solo da acceso al turno al que pertenece: verlo, confirmarlo o cancelarlo.
- **RN-08 — Reserva online solo en libres.** Un paciente solo puede reservar un horario que realmente esté libre; se aplican las mismas reglas RN-01 a RN-04.

## 8. Integraciones

| Integración | En el MVP | Notas |
|---|---|---|
| Email (SMTP) | **Sí, real** | Recordatorios. Las credenciales van en `.env` (excluido del repo) y se versiona solo un `.env.example`. |
| WhatsApp Business API | No | Requiere cuenta de Meta y tiene costo por mensaje. |
| Mercado Pago, ARCA, obras sociales, Google Calendar | No | Para etapas posteriores. |

## 9. Restricciones

- Proyecto académico con fecha de entrega de la cátedra y un equipo de 6: Juan Pablo (líder), Santino, Valentino, Lionel, Julián y Nico.
- Alcance técnico: API con lógica de dominio y tests automatizados, más un front web simple.
- El stack es libre y lo propone `kb-creator`.
- Solo datos ficticios de pacientes. Nunca secretos ni credenciales en el repositorio.
- No se crean cuentas ni se contacta a los proveedores analizados.

## 10. Riesgos

- **Evidencia de mercado débil.** La mayoría de las funcionalidades de los competidores son afirmación comercial, así que el posicionamiento puede estar sobreestimado. Mitigación: verificar personalmente las fuentes clave.
- **Token del paciente.** Si se filtra o se puede adivinar, otra persona podría cancelar el turno. Necesita entropía suficiente, expiración y alcance limitado (RN-07).
- **Fechas y zonas horarias.** Los errores de zona horaria o de borde en los intervalos rompen la prevención de solapamientos. Se fija `America/Argentina/Buenos_Aires` y se cubre con tests de borde.
- **Concurrencia.** Dos reservas simultáneas (recepción y paciente online) sobre el mismo hueco. Se necesita una validación atómica.
- **Envío de email.** Los fallos de SMTP pueden pasar sin que nadie se entere, así que los envíos tienen que dejar registro.
- **Datos de salud.** Aplican la Ley 25.326 (datos personales) y la Ley 26.529 (derechos del paciente e historia clínica), aunque el MVP solo guarde una ficha básica.

## 11. Preguntas abiertas

- ~~¿Cuánto tiempo es válido el token y qué datos mínimos pide la reserva online (nombre, email, teléfono, DNI)?~~ **Resuelta (2026-11-20, decisión del líder).** El token es válido desde la reserva hasta el inicio del turno o hasta que el turno se cancele, lo que ocurra primero; se guarda hasheado y es de un solo turno. La reserva online pide nombre completo, email, teléfono y obra social (texto libre); **no pide DNI**. Los pacientes se deduplican por email (y teléfono si coincide). Ver `knowledge-base/05_reglas_de_negocio.md` (RN-25, RN-26, RN-28).
- ¿Con cuánta anticipación se manda el recordatorio por email (24 hs, 48 hs, configurable)?
- La lista de espera, ¿notifica sola al paciente o la recepción elige a quién ofrecerle el hueco?
- ¿Una prestación puede requerir un tipo específico de sillón o box?
- ¿El odontólogo puede dar turnos él mismo o solo los ve?
- Datos de mercado sin respaldo público, marcados como "No evidenciado" en el informe:
  - precios en ARS de los sistemas extranjeros;
  - alcance real de la integración ARCA de DriCloud;
  - si Open Dental considera a Argentina dentro de su tarifa para "Developing Countries";
  - el precio de DentalCore (aparecen dos valores que no coinciden);
  - el contenido de Doctoralia AR y de Gesden, cuyos sitios no se pudieron leer.
