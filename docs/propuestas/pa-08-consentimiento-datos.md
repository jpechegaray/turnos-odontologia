# PA-08: consentimiento y datos personales en la reserva online

- **Estado:** propuesta, pendiente de aprobación del equipo y de consulta con la cátedra.
- **Fecha:** 2026-11-25.
- **Autor:** Lionel.
- **Bloquea:** C-41 `reserva-publica-crear-reserva` (US-015) y el texto del formulario de C-48.
- **Documento para revisar y comentar:** https://claude.ai/artifact/X47F4HGGBs3u5gNBSkUZeQ

**Propuesta en resumen:**

- Un checkbox obligatorio y sin marcar, acompañado de un aviso informativo que cumple el art. 6 de la Ley 25.326.
- Se guardan la fecha y la versión del texto que aceptó el paciente.
- En el MVP académico, que usa solo datos ficticios, la base no se inscribe ante la AAIP. Queda documentado como requisito para un consultorio real.
- Los datos se conservan hasta 2 años después del último turno.

> Esta propuesta no es asesoramiento legal. Los artículos citados salen de búsquedas, así que conviene confirmarlos en InfoLEG antes de cerrar PA-08 (ver "Fuentes y qué falta verificar").

---

## 1. Qué hay que decidir

PA-08 bloquea C-41 porque RN-26 exige que el paciente acepte el tratamiento de sus datos y todavía no hay un texto acordado. Hay que responder tres preguntas:

1. **Texto de consentimiento:** qué lee y acepta el paciente antes de reservar.
2. **Inscripción ante la AAIP:** si el consultorio tiene que registrar la base de datos.
3. **Retención:** cuánto tiempo se guardan los datos y cómo se pide borrarlos.

La reserva pide nombre, apellido, email, teléfono y obra social, sin DNI (RN-26). Además, elegir una prestación odontológica puede revelar información de salud, que la ley trata como dato sensible. Por eso conviene redactar todo con ese criterio.

## 2. Texto de consentimiento

El consentimiento tiene dos partes:

- **Checkbox obligatorio**, **sin marcar por defecto**, junto al botón Reservar.
- **Aviso informativo**, que se despliega desde un enlace.

El aviso cubre lo que exige el art. 6: quién trata los datos, para qué, quién los recibe, si son obligatorios y qué derechos tiene el titular. Si el checkbox no está marcado, el backend responde 422.

**Checkbox (texto exacto):**

> Leí y acepto que [Nombre del consultorio] trate mis datos personales para gestionar mis turnos, como se explica en *Cómo usamos tus datos*.

**Aviso informativo "Cómo usamos tus datos":**

> **Responsable.** [Nombre del consultorio], [domicilio], [email de contacto], es responsable de la base de datos de pacientes.
>
> **Para qué.** Usamos tu nombre, email, teléfono y obra social solo para registrar tu turno, enviarte la confirmación con el enlace para gestionarlo, recordatorios y, si te anotás en la lista de espera, avisos de horarios libres. No los usamos para publicidad.
>
> **Quién los ve.** Solo el personal del consultorio. Los emails se envían por un proveedor de correo que actúa por cuenta nuestra. No cedemos tus datos a terceros.
>
> **Son necesarios.** Sin estos datos no podemos reservar el turno ni avisarte cambios. Si son inexactos, puede que no recibas la confirmación.
>
> **Tus derechos.** Podés pedir acceso a tus datos, corregirlos o suprimirlos escribiendo a [email de contacto]. El acceso es gratuito a intervalos no inferiores a seis meses, salvo interés legítimo (art. 14 inc. 3, Ley 25.326). La Agencia de Acceso a la Información Pública, órgano de control de la Ley 25.326, atiende las denuncias y reclamos por incumplimiento de las normas de protección de datos personales.
>
> **Cuánto tiempo.** Conservamos tus datos mientras seas paciente y hasta 2 años después de tu último turno.

Lo único que cambia de un consultorio a otro son los datos entre corchetes. En el MVP salen de la configuración (C-08) o se completan con un nombre ficticio. Cada versión del texto lleva un identificador (por ejemplo, `2026-11-v1`) para saber qué aceptó cada paciente.

## 3. Inscripción ante la AAIP

Propuesta: **no inscribir la base en el MVP** y dejar escrito que es un requisito previo a usar el sistema con pacientes reales.

- **Qué dice la ley:** el art. 21 obliga a inscribir las bases públicas y las privadas "destinadas a dar informes". En la práctica, la AAIP también pide registrar bases privadas como la de un consultorio. El responsable se inscribe antes que la base. El trámite se hace en línea, por TAD.
- **Por qué no ahora:** el proyecto es académico, no hay un consultorio real que sea responsable y solo se usan datos ficticios (regla dura 1). No existe una base real que inscribir.
- **Qué dejamos hecho:**
  - Un supuesto nuevo en `knowledge-base/09_decisiones_y_supuestos.md`: "un consultorio que adopte el sistema inscribe su base ante la AAIP antes de cargar pacientes reales".
  - Una línea en el manual de puesta en marcha de C-51.

El art. 8 permite a los profesionales de la salud tratar datos de salud de sus pacientes, siempre que respeten el secreto profesional. Por eso el consultorio sí puede guardar qué prestación reservó cada paciente.

## 4. Política de retención

Propuesta: conservar la ficha y sus turnos **hasta 2 años después del último turno** y atender a mano los pedidos de supresión. El borrado automático queda fuera del MVP.

| Dato | Cuánto se guarda | Cómo se borra en el MVP |
| --- | --- | --- |
| Ficha del paciente (nombre, contacto, obra social) | Mientras sea paciente y hasta 2 años después de su último turno | A pedido por email: ADMIN da de baja la ficha |
| Turnos y su historial | Igual que la ficha | Junto con la ficha |
| Registro de notificaciones enviadas | Igual que la ficha | Junto con la ficha |
| Token del turno | Vence al inicio del turno o al cancelarse (RN-25) | Ya resuelto: se guarda hasheado |
| Fecha y versión del consentimiento | Igual que la ficha | Junto con la ficha |

El sistema no guarda historia clínica (RN-35), así que no se aplica el plazo de conservación de la Ley 26.529. Por ahora, un pedido de supresión solo puede resolverse con una baja lógica. Para suprimir o anonimizar de verdad hace falta un change nuevo, que proponemos anotar como pregunta abierta para después del MVP.

## 5. Impacto en C-41

El cambio es chico: `Paciente` ya tiene el campo `consentimientoDatosEn`, y falta guardar qué versión del texto se aceptó.

- `POST /api/public/reservas` recibe `aceptaTratamientoDatos: true` y la versión del texto que mostró el front. Si no viene la aceptación, responde 422. Si la versión no es la vigente, también.
- **Paciente nuevo:** se guardan `consentimientoDatosEn`, con el instante del reloj inyectable, y la versión.
- **Paciente existente (RN-28):** la ficha no se modifica, así que el consentimiento de esa reserva se guarda en el turno, en un campo `consentimientoDatosEn` propio. Así se cumple que "el consentimiento figura en el registro", el criterio de salida de C-41, sin tocar la ficha ajena.
- El texto vive en el front (C-48) con su identificador de versión. El backend solo conoce la versión vigente, como constante o como parámetro de configuración.
- Tests:
  - Reservar sin aceptar responde 422.
  - Reservar aceptando guarda la fecha y la versión.
  - Con una ficha existente, la ficha queda igual y el turno lleva el consentimiento.

## 6. Decisiones para el equipo

- [ ] Checkbox obligatorio, sin marcar, con el texto propuesto.
- [ ] Aviso informativo "Cómo usamos tus datos" con los seis puntos propuestos.
- [ ] Versionar el texto (`2026-11-v1`) y guardar la fecha y la versión aceptadas.
- [ ] Con una ficha existente, guardar el consentimiento en el turno y no en la ficha (respeta RN-28).
- [ ] No inscribir la base ante la AAIP en el MVP, y documentarlo como supuesto y en el manual de C-51.
- [ ] Retención de 2 años desde el último turno, supresión a pedido mediante baja lógica y borrado real fuera del MVP.
- [ ] Consultar con la cátedra si esta propuesta alcanza o si pide algo más.

## 7. Fuentes y qué falta verificar

Los artículos citados salen de búsquedas, no de una lectura completa del texto oficial. Antes de cerrar PA-08, alguien del equipo debería confirmar en InfoLEG los arts. 6, 8, 14 y 21, y que siga vigente la Resolución AAIP 14/2018, que derogó la Disposición DNPDP 10/2008.

- [Ley 25.326, texto actualizado (argentina.gob.ar)](https://www.argentina.gob.ar/normativa/nacional/ley-25326-2000-64790/actualizacion)
- [Resolución AAIP 14/2018](https://www.argentina.gob.ar/normativa/nacional/norma-307621/texto)
- [Trámites de la AAIP sobre datos personales](https://www.argentina.gob.ar/aaip/datospersonales/tramites)
- [Resumen de Marval sobre las resoluciones de 2018](https://www.marval.com/publicacion/nuevas-resoluciones-en-materia-de-proteccion-de-datos-personales-13167/)
- KB del proyecto: RN-26, RN-28 y RN-35 en `knowledge-base/05_reglas_de_negocio.md`; PA-08 y PA-16 en `knowledge-base/10_preguntas_abiertas.md`.

---

## Cómo arrancar C-41 cuando el equipo apruebe

1. Volcar en este archivo lo que el equipo cambió, desde los comentarios del documento compartido, y pasar el **Estado** a `aprobada (<fecha>, por el equipo)`.
2. Abrir una sesión nueva en la rama `lionel`, actualizada con `master`, y pegar este pedido:

> Proponé C-41 `c-41-reserva-publica-crear-reserva` (governance HIGH: proponé y esperá mi revisión antes de escribir código). PA-08 quedó resuelta con `docs/propuestas/pa-08-consentimiento-datos.md`, que es la fuente de verdad para el consentimiento, la versión del texto, la AAIP y la retención; no reabras esas decisiones. Antes de proponer: leé la ficha de C-41 en `CHANGES.md` y sus "Leer antes", marcá PA-08 como resuelta en `knowledge-base/10_preguntas_abiertas.md` (y RN-26 si cambia algo), y agregá el supuesto de la AAIP en `09_decisiones_y_supuestos.md`. PA-09 sigue abierta: proponé tratarla como hoy (el paciente elige un profesional y el sistema asigna el sillón, SU-03). Reusá C-38 (`reservapublica`, `DisponibilidadPublica`) y el servicio de crear turno de `agenda`. Ojo: `lionel` tiene commits sin pushear (el archivo de C-38) que van en el mismo PR.
