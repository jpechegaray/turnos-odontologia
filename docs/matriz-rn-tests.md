# Matriz de reglas de negocio a tests

Esta matriz vincula cada regla de [`knowledge-base/05_reglas_de_negocio.md`](../knowledge-base/05_reglas_de_negocio.md) (RN-01 a RN-38) con la capacidad de OpenSpec que la expresa, el escenario y el test que la verifica. El test lleva el nombre exacto del escenario, en `@DisplayName` (JUnit) o en `it(...)` (Vitest), según la regla dura 11.

Hay una o dos filas por regla, con los tests más representativos; no es el inventario completo. Cada escenario de las specs tiene su test con el mismo nombre, así que para ver todos los tests de una regla alcanza con buscar sus escenarios en `openspec/specs/`.

La matriz se verifica con este comando, desde la raíz del repo:

```bash
node scripts/verificar-matriz-rn.mjs
```

El script comprueba tres cosas, y si falla indica la fila que hay que corregir:

- que estén las 38 reglas;
- que ninguna fila tenga el test vacío;
- que cada escenario citado exista como nombre de test en el archivo indicado.

Generada en C-51 (2026-11-26).

| RN | Regla | Capacidad | Escenario | Test |
|---|---|---|---|---|
| RN-01 | Sin sobreturnos | agenda-turnos | Un turno solapado por profesional se rechaza | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-01 | Sin sobreturnos | agenda-turnos | La base rechaza un solapamiento que esquivó la validación | `backend/src/test/java/ar/edu/utn/turnos/agenda/infrastructure/TurnosPersistenciaIT.java` |
| RN-02 | Borde no solapado | agenda-turnos | Un turno que empieza justo cuando termina otro no se solapa | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-02 | Borde no solapado | agenda-turnos | Un turno que empieza un minuto antes del fin de otro se solapa | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-03 | Duración por prestación | agenda-turnos | Sin duración se usa la de la prestación | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-03 | Duración por prestación | agenda-turnos | La duración se puede ajustar | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-04 | Horario y bloqueos | agenda-turnos | Un turno que sale del horario se rechaza | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-04 | Horario y bloqueos | agenda-turnos | Un turno dentro de un bloqueo se rechaza | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-05 | Plazo de cancelación | agenda-turnos | Cancelar dentro del plazo queda marcado como tardía y el paciente por token no lleva usuario | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/TurnoTest.java` |
| RN-05 | Plazo de cancelación | agenda-turnos | Con plazo cero nunca es tardía antes del inicio | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/PoliticaCancelacionTest.java` |
| RN-06 | Cancelar libera el horario | agenda-turnos | Tras cancelar se puede dar otro turno en el mismo horario | `backend/src/test/java/ar/edu/utn/turnos/agenda/web/TurnosCancelacionApiIT.java` |
| RN-06 | Cancelar libera el horario | recorrido-turno | Una cancelación termina con el hueco tomado desde la lista de espera | `backend/src/test/java/ar/edu/utn/turnos/recorridos/RecorridoTurnoIT.java` |
| RN-07 | Alcance del token | token-turno | El token del turno A no abre el turno B | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/token/ServicioTokenTurnoIT.java` |
| RN-07 | Alcance del token | recorrido-turno | Un paciente reserva online y gestiona su turno por el enlace | `backend/src/test/java/ar/edu/utn/turnos/recorridos/RecorridoTurnoIT.java` |
| RN-08 | Reserva online solo en libres | reserva-publica | Un hueco ocupado responde 409 con alternativas | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/web/ReservaOnlineConcurrenciaIT.java` |
| RN-08 | Reserva online solo en libres | reserva-publica | Un horario fuera de la atención del profesional responde 422 | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/web/ReservaOnlineApiIT.java` |
| RN-09 | Fin calculado y granularidad | agenda-turnos | Un inicio fuera de la grilla de cinco minutos se rechaza | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-09 | Fin calculado y granularidad | agenda-turnos | Una duración fuera de rango se rechaza | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-10 | Los turnos cancelados no ocupan lugar | agenda-turnos | Un turno cancelado no ocupa lugar | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-10 | Los turnos cancelados no ocupan lugar | agenda-turnos | Un turno atendido sigue ocupando lugar | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-11 | Recursos obligatorios y activos | agenda-turnos | El profesional no realiza la prestación | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-11 | Recursos obligatorios y activos | agenda-turnos | Un recurso inactivo se rechaza | `backend/src/test/java/ar/edu/utn/turnos/agenda/web/TurnosApiIT.java` |
| RN-12 | Estados y transiciones del turno | agenda-turnos | Transiciones permitidas desde un turno programado | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/TurnoTest.java` |
| RN-12 | Estados y transiciones del turno | agenda-turnos | Un estado final no cambia | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/TurnoTest.java` |
| RN-13 | No se dan turnos en el pasado | agenda-turnos | Un turno en el pasado se rechaza | `backend/src/test/java/ar/edu/utn/turnos/agenda/domain/ValidadorTurnoTest.java` |
| RN-14 | Zona horaria única | horarios-de-atencion | La contención se calcula en hora de Buenos Aires | `backend/src/test/java/ar/edu/utn/turnos/profesionales/horario/infrastructure/HorarioConsultaIT.java` |
| RN-14 | Zona horaria única | agenda-frontend | Un instante en UTC se muestra en hora de Buenos Aires | `frontend/src/features/agenda/Agenda.test.tsx` |
| RN-15 | Garantía ante concurrencia | agenda-turnos | Dos creaciones simultáneas sobre el mismo hueco | `backend/src/test/java/ar/edu/utn/turnos/agenda/web/TurnosConcurrenciaIT.java` |
| RN-15 | Garantía ante concurrencia | toma-oferta-hueco | Dos pacientes confirman a la vez y solo uno obtiene el turno | `backend/src/test/java/ar/edu/utn/turnos/listaespera/oferta/web/TomaOfertaConcurrenciaIT.java` |
| RN-16 | Horario por tramos | agenda-turnos | Un turno que cruza el corte entre dos tramos se rechaza | `backend/src/test/java/ar/edu/utn/turnos/agenda/web/TurnosApiIT.java` |
| RN-16 | Horario por tramos | horarios-de-atencion | Un tramo superpuesto en el mismo día se rechaza | `backend/src/test/java/ar/edu/utn/turnos/profesionales/horario/web/HorariosApiIT.java` |
| RN-17 | Bloqueos y turnos existentes | bloqueos-de-agenda | Crear un bloqueo superpuesto con turnos activos devuelve 409 con la lista de conflictos | `backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java` |
| RN-17 | Bloqueos y turnos existentes | bloqueos-de-agenda | Cancelar el turno activo permite crear el bloqueo antes rechazado | `backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java` |
| RN-18 | Reprogramar conserva la identidad | agenda-turnos | Recepción mueve un turno a otro horario | `backend/src/test/java/ar/edu/utn/turnos/agenda/web/TurnosReprogramacionApiIT.java` |
| RN-18 | Reprogramar conserva la identidad | recorrido-turno | Una reprogramación termina con el hueco tomado desde la lista de espera | `backend/src/test/java/ar/edu/utn/turnos/recorridos/RecorridoTurnoIT.java` |
| RN-19 | Registro de la cancelación | agenda-turnos | La cancelación guarda quién la hizo y cuándo | `backend/src/test/java/ar/edu/utn/turnos/agenda/web/TurnosCancelacionApiIT.java` |
| RN-20 | Asistencia | agenda-turnos | Recepción marca un turno como atendido | `backend/src/test/java/ar/edu/utn/turnos/agenda/web/TurnosAsistenciaApiIT.java` |
| RN-20 | Asistencia | agenda-turnos | El odontólogo no puede marcar asistencia | `backend/src/test/java/ar/edu/utn/turnos/agenda/web/TurnosAsistenciaApiIT.java` |
| RN-21 | Alta en lista de espera | lista-espera | Una segunda entrada activa para la misma prestación se rechaza | `backend/src/test/java/ar/edu/utn/turnos/listaespera/web/ListaEsperaApiIT.java` |
| RN-21 | Alta en lista de espera | lista-espera | Un paciente sin email no se puede anotar | `backend/src/test/java/ar/edu/utn/turnos/listaespera/web/ListaEsperaApiIT.java` |
| RN-22 | Compatibilidad | lista-espera | El profesional del hueco debe realizar la prestación | `backend/src/test/java/ar/edu/utn/turnos/listaespera/domain/CompatibilidadListaEsperaTest.java` |
| RN-22 | Compatibilidad | lista-espera | Una prestación de 90 minutos no cabe en un hueco de 30 | `backend/src/test/java/ar/edu/utn/turnos/listaespera/domain/CompatibilidadListaEsperaTest.java` |
| RN-23 | Aviso automático a todos los compatibles | oferta-hueco | Tras cancelar solo reciben aviso las entradas compatibles | `backend/src/test/java/ar/edu/utn/turnos/listaespera/oferta/application/AvisoHuecoLiberadoIT.java` |
| RN-23 | Aviso automático a todos los compatibles | oferta-hueco | Una reprogramación avisa por el horario que dejó libre | `backend/src/test/java/ar/edu/utn/turnos/listaespera/oferta/application/AvisoHuecoReprogramacionIT.java` |
| RN-24 | El primero que confirma se queda con el turno | toma-oferta-hueco | El paciente toma un hueco disponible | `backend/src/test/java/ar/edu/utn/turnos/listaespera/oferta/web/TomaOfertaIT.java` |
| RN-24 | El primero que confirma se queda con el turno | toma-oferta-hueco | Dos pacientes confirman a la vez y solo uno obtiene el turno | `backend/src/test/java/ar/edu/utn/turnos/listaespera/oferta/web/TomaOfertaConcurrenciaIT.java` |
| RN-25 | Token: generación, almacenamiento y vigencia | token-turno | La emisión entrega el secreto y guarda solo su hash | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/token/ServicioTokenTurnoIT.java` |
| RN-25 | Token: generación, almacenamiento y vigencia | token-turno | El token vence exactamente al inicio del turno | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/token/ServicioTokenTurnoIT.java` |
| RN-26 | Datos mínimos de la reserva online | reserva-publica | Una reserva sin email o sin obra social responde 400 | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/web/ReservaOnlineApiIT.java` |
| RN-26 | Datos mínimos de la reserva online | reserva-publica | La reserva no pide ni guarda DNI | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/web/ReservaOnlineApiIT.java` |
| RN-27 | Límites de la reserva online | limite-tasa-publico | Superar el umbral por IP en una ruta pública devuelve 429 con Retry-After | `backend/src/test/java/ar/edu/utn/turnos/shared/ratelimit/FiltroLimiteTasaPublicoTest.java` |
| RN-27 | Límites de la reserva online | reserva-publica | Se supera el tope de turnos activos por email | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/web/ReservaOnlineApiIT.java` |
| RN-28 | Identidad del paciente en reserva online | reserva-publica | Una reserva con una ficha existente no modifica la ficha | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/reserva/SeleccionFichaTest.java` |
| RN-28 | Identidad del paciente en reserva online | reserva-publica | Con varias fichas del mismo email se elige la que coincide en teléfono | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/reserva/SeleccionFichaTest.java` |
| RN-29 | Recordatorio | recordatorio-turno | Un turno creado con poca anticipación entra en el siguiente ciclo | `backend/src/test/java/ar/edu/utn/turnos/shared/notificacion/application/PlanificadorRecordatoriosIT.java` |
| RN-29 | Recordatorio | recorrido-turno | El recordatorio llega una sola vez y no llega al turno cancelado | `backend/src/test/java/ar/edu/utn/turnos/recorridos/RecorridoTurnoIT.java` |
| RN-30 | Idempotencia y registro | recordatorio-turno | Dos ciclos no duplican el recordatorio | `backend/src/test/java/ar/edu/utn/turnos/shared/notificacion/application/PlanificadorRecordatoriosIT.java` |
| RN-30 | Idempotencia y registro | recordatorio-turno | Un fallo de correo se reintenta hasta el tope | `backend/src/test/java/ar/edu/utn/turnos/shared/notificacion/application/PlanificadorRecordatoriosIT.java` |
| RN-31 | Emails transaccionales | notificaciones-email | La cancelación del personal envía un solo correo | `backend/src/test/java/ar/edu/utn/turnos/shared/notificacion/application/AvisosCambiosTurnoIT.java` |
| RN-31 | Emails transaccionales | toma-oferta-hueco | El paciente recibe el enlace de su nuevo turno | `backend/src/test/java/ar/edu/utn/turnos/listaespera/oferta/web/TomaOfertaIT.java` |
| RN-32 | Ficha de paciente | gestion-pacientes | Alta sin email ni teléfono se rechaza | `backend/src/test/java/ar/edu/utn/turnos/pacientes/domain/PacienteTest.java` |
| RN-32 | Ficha de paciente | gestion-pacientes | Un DNI repetido devuelve 409 | `backend/src/test/java/ar/edu/utn/turnos/pacientes/web/PacienteGestionIT.java` |
| RN-33 | Permisos por rol | control-acceso | Cada rol lee lo que la matriz le permite | `backend/src/test/java/ar/edu/utn/turnos/seguridad/web/MatrizPermisosIT.java` |
| RN-33 | Permisos por rol | control-acceso | Toda escritura de un odontólogo responde 403 | `backend/src/test/java/ar/edu/utn/turnos/seguridad/web/EscriturasOdontologoIT.java` |
| RN-34 | Autenticación del personal | control-acceso | El quinto fallo bloquea hasta el vencimiento exacto | `backend/src/test/java/ar/edu/utn/turnos/seguridad/infrastructure/AutenticacionPersonalIT.java` |
| RN-34 | Autenticación del personal | administracion-usuarios | Un usuario desactivado no puede iniciar sesión | `backend/src/test/java/ar/edu/utn/turnos/seguridad/infrastructure/UsuariosApiIT.java` |
| RN-35 | Datos personales y de salud | reserva-publica | La respuesta no revela si la ficha ya existía | `backend/src/test/java/ar/edu/utn/turnos/reservapublica/web/ReservaOnlineApiIT.java` |
| RN-35 | Datos personales y de salud | gestion-pacientes | El odontólogo no consulta la ficha de un paciente de otro profesional | `backend/src/test/java/ar/edu/utn/turnos/pacientes/web/PacienteFichaOdontologoIT.java` |
| RN-36 | Configuración validada | configuracion-parametros | Un valor inválido devuelve RN-36 sin guardar los demás | `backend/src/test/java/ar/edu/utn/turnos/configuracion/ConfiguracionApiIT.java` |
| RN-36 | Configuración validada | configuracion-frontend | Un plazo de cancelación fuera de rango bloquea el guardado sin llamar a la API | `frontend/src/features/configuracion/Configuracion.test.tsx` |
| RN-37 | Bajas lógicas del catálogo | catalogo-prestaciones | La baja deja la prestación inactiva pero consultable | `backend/src/test/java/ar/edu/utn/turnos/prestaciones/web/CatalogoPrestacionesIT.java` |
| RN-37 | Bajas lógicas del catálogo | catalogo-profesionales | El catálogo no permite borrar físicamente un profesional | `backend/src/test/java/ar/edu/utn/turnos/profesionales/profesional/web/ProfesionalesApiIT.java` |
| RN-38 | Oferta de hueco: token y vigencia | oferta-hueco | Solo se guarda el hash del token | `backend/src/test/java/ar/edu/utn/turnos/listaespera/oferta/application/AvisoHuecoLiberadoIT.java` |
| RN-38 | Oferta de hueco: token y vigencia | oferta-hueco | Una oferta vence al inicio del hueco | `backend/src/test/java/ar/edu/utn/turnos/listaespera/oferta/application/CicloOfertaHuecoIT.java` |
