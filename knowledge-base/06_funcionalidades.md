# Funcionalidades

Organizadas por **épica** y luego por **historia de usuario** (US-NNN). Las épicas E1 a E9 son el **MVP** (`discovery.md` §5). Las épicas E10 a E17 están **fuera de alcance** (`discovery.md` §6): son épicas futuras, marcadas así, y no se implementan en esta entrega.

Los criterios de aceptación (CA) son verificables y se traducen en tests. Las épicas se corresponden con los módulos del backend (ver `08_arquitectura_propuesta.md`).

---

## Épica 1 (MVP): Autenticación y usuarios del personal

### US-001 — Iniciar y cerrar sesión
**Como** miembro del personal (odontólogo, recepción o administrador)
**Quiero** iniciar sesión con email y contraseña
**Para** acceder solo a lo que mi rol permite

**Criterios de aceptación**:
- [ ] Login correcto crea una sesión y devuelve el usuario y su rol.
- [ ] Credenciales incorrectas devuelven el mismo mensaje genérico sin distinguir causa.
- [ ] Tras 5 intentos fallidos la cuenta se bloquea temporalmente.
- [ ] Logout invalida la sesión.
- [ ] Un usuario inactivo no puede iniciar sesión.

**Reglas relacionadas**: RN-33, RN-34

### US-002 — Administrar usuarios
**Como** administrador
**Quiero** crear, modificar y desactivar usuarios y asignarles rol
**Para** controlar quién accede al sistema

**Criterios de aceptación**:
- [ ] Solo ADMIN accede a la gestión de usuarios.
- [ ] El email es único.
- [ ] Un usuario ODONTOLOGO se vincula a un profesional.
- [ ] Las contraseñas se guardan con BCrypt.

**Reglas relacionadas**: RN-33, RN-34

---

## Épica 2 (MVP): Configuración del consultorio (catálogos)

### US-003 — Administrar prestaciones y duraciones
**Como** administrador
**Quiero** definir las prestaciones con su duración por defecto
**Para** que los turnos tengan la duración correcta

**Criterios de aceptación**:
- [ ] Alta, modificación y baja lógica de prestaciones.
- [ ] La duración es un múltiplo de 5 entre 5 y 480 minutos.
- [ ] Una prestación inactiva no aparece al dar turnos ni en la reserva online.

**Reglas relacionadas**: RN-03, RN-09, RN-37

### US-004 — Administrar sillones o boxes
**Como** administrador
**Quiero** dar de alta y desactivar sillones
**Para** reflejar los lugares físicos de atención

**Criterios de aceptación**:
- [ ] Nombre único de sillón.
- [ ] No se puede desactivar un sillón con turnos futuros activos sin resolverlos antes.

**Reglas relacionadas**: RN-01, RN-37

### US-005 — Administrar profesionales
**Como** administrador
**Quiero** dar de alta profesionales y asignarles las prestaciones que realizan
**Para** que la agenda y la reserva online ofrezcan combinaciones válidas

**Criterios de aceptación**:
- [ ] Alta, modificación y baja lógica de profesionales.
- [ ] Asociación profesional-prestaciones.
- [ ] Vinculación opcional con un usuario ODONTOLOGO.

**Reglas relacionadas**: RN-11, RN-37

### US-006 — Configurar plazos y límites
**Como** administrador
**Quiero** configurar el plazo de cancelación y la anticipación del recordatorio
**Para** adaptar las reglas al consultorio

**Criterios de aceptación**:
- [ ] El plazo de cancelación por defecto es 24 hs y es modificable.
- [ ] Los valores se validan (RN-36).
- [ ] El cambio afecta solo operaciones posteriores.

**Reglas relacionadas**: RN-05, RN-29, RN-36

---

## Épica 3 (MVP): Horarios de atención y bloqueos

### US-007 — Definir horarios de atención
**Como** recepción (o administrador), a pedido del odontólogo
**Quiero** definir los tramos horarios de cada profesional por día de la semana
**Para** que solo se den turnos cuando atiende

**Criterios de aceptación**:
- [ ] Varios tramos por día, sin superponerse.
- [ ] Recepción y ADMIN editan los de todos los profesionales; el odontólogo solo los consulta (cualquier intento de modificar responde 403).
- [ ] Un turno fuera de los tramos se rechaza.

**Reglas relacionadas**: RN-04, RN-16, RN-33

### US-008 — Bloquear la agenda
**Como** recepción (o administrador), a pedido del odontólogo
**Quiero** bloquear un intervalo de un profesional (vacaciones, congreso, trámite)
**Para** que no se den turnos en ese período

**Criterios de aceptación**:
- [ ] Recepción y ADMIN crean y eliminan bloqueos de cualquier profesional; el odontólogo solo los ve (403 si intenta modificarlos).
- [ ] Un bloqueo con turnos activos superpuestos se rechaza y lista los conflictos.
- [ ] Un turno que empieza justo cuando termina el bloqueo se acepta.
- [ ] Se puede eliminar un bloqueo.

**Reglas relacionadas**: RN-02, RN-04, RN-17

---

## Épica 4 (MVP): Pacientes

### US-009 — Gestionar la ficha de paciente
**Como** recepción
**Quiero** crear y buscar pacientes con datos de contacto, obra social y notas
**Para** asignarles turnos rápidamente

**Criterios de aceptación**:
- [ ] Búsqueda por apellido, nombre, DNI o email.
- [ ] DNI único cuando se informa; al menos un medio de contacto.
- [ ] La obra social es un campo de texto.
- [ ] Baja lógica, nunca borrado físico con turnos.

**Reglas relacionadas**: RN-32, RN-35

---

## Épica 5 (MVP): Agenda y turnos (núcleo)

### US-010 — Crear un turno sin solapamientos
**Como** recepción
**Quiero** crear un turno eligiendo paciente, profesional, sillón, prestación y horario
**Para** no dar dos turnos al mismo tiempo

**Criterios de aceptación**:
- [ ] Se rechaza con 409 si se superpone con otro turno del mismo profesional o del mismo sillón.
- [ ] Un turno que empieza justo cuando termina otro se acepta.
- [ ] Se rechaza con 422 si cae fuera de horario, en un bloqueo, en el pasado o con recursos inactivos.
- [ ] Se puede usar cualquier sillón activo y libre: ninguna prestación exige un tipo de sillón (RN-11).
- [ ] La duración por defecto de la prestación se propone y se puede ajustar.
- [ ] Dos creaciones simultáneas sobre el mismo hueco: una tiene éxito y la otra recibe 409.
- [ ] Se muestran los horarios libres sugeridos para facilitar la elección.

**Reglas relacionadas**: RN-01, RN-02, RN-03, RN-04, RN-09, RN-11, RN-13, RN-15, RN-16

### US-011 — Reprogramar un turno
**Como** recepción
**Quiero** mover un turno a otro horario, profesional o sillón
**Para** acomodar cambios sin perder el historial

**Criterios de aceptación**:
- [ ] Se aplican las mismas validaciones que al crear, excluyendo al propio turno.
- [ ] Se libera el hueco anterior y se ofrece a la lista de espera.
- [ ] Fuera del plazo queda marcado como reprogramación tardía.
- [ ] El paciente recibe email de aviso si tiene email.
- [ ] Solo recepción (o ADMIN) reprograma; el paciente no puede hacerlo con su token.

**Reglas relacionadas**: RN-05, RN-18, RN-31

### US-012 — Cancelar un turno y liberar el horario
**Como** recepción
**Quiero** cancelar un turno
**Para** mantener la agenda al día y recuperar el hueco

**Criterios de aceptación**:
- [ ] El turno pasa a CANCELADO y el intervalo queda libre.
- [ ] Dentro del plazo no es tardía; fuera del plazo se registra como tardía.
- [ ] Se registra quién cancela, cuándo y el motivo.
- [ ] Tras cancelar, el sistema avisa solo a los pacientes compatibles de la lista de espera (US-018) y se muestran a recepción las entradas compatibles.

**Reglas relacionadas**: RN-05, RN-06, RN-10, RN-19

### US-013 — Ver la agenda diaria y semanal
**Como** odontólogo
**Quiero** ver mi agenda del día y de la semana, por profesional y por sillón
**Para** saber a quién atiendo y cuándo

**Criterios de aceptación**:
- [ ] Vistas día y semana, filtrables por profesional y por sillón.
- [ ] Se muestran horarios de atención, bloqueos y turnos con su estado.
- [ ] Los horarios se muestran en hora de Buenos Aires.
- [ ] Se puede abrir la ficha básica de los pacientes de sus turnos.
- [ ] Es solo lectura: no hay acciones de alta, reprogramación, cancelación, asistencia, horarios ni bloqueos; si las invoca por API responde 403. Para un cambio, el odontólogo se comunica con recepción fuera del sistema.

**Reglas relacionadas**: RN-14, RN-33

### US-014 — Marcar asistencia o ausencia
**Como** recepción (o administrador)
**Quiero** marcar si el paciente asistió o faltó
**Para** llevar el registro de la atención (el odontólogo me lo comunica fuera del sistema)

**Criterios de aceptación**:
- [ ] Solo desde el inicio del turno y sobre turnos PROGRAMADO o CONFIRMADO.
- [ ] Estados finales ATENDIDO o AUSENTE; solo ADMIN corrige una marca incorrecta.
- [ ] Un turno cancelado no admite asistencia.
- [ ] El odontólogo no puede marcar asistencia (403).

**Reglas relacionadas**: RN-12, RN-20

---

## Épica 6 (MVP): Reserva online del paciente (sin login, con token)

### US-015 — Reservar un turno online
**Como** paciente
**Quiero** elegir prestación, profesional y un horario libre y reservar sin crear cuenta
**Para** obtener un turno sin llamar al consultorio

**Criterios de aceptación**:
- [ ] Solo se ofrecen horarios realmente libres (RN-01 a RN-04).
- [ ] Se piden nombre completo, email, teléfono, obra social (texto libre) y aceptación de datos personales; **no se pide DNI**.
- [ ] Si ya existe una ficha con ese email (y teléfono si coincide), se reutiliza sin modificar sus datos.
- [ ] Si el horario se ocupó mientras se completaba el formulario, responde 409 y ofrece alternativas.
- [ ] Al reservar se recibe un email con el enlace con token y se muestra en pantalla.
- [ ] Se respeta el horizonte, la anticipación mínima y el tope por email.
- [ ] Los endpoints públicos tienen rate limit.

**Reglas relacionadas**: RN-08, RN-15, RN-25, RN-26, RN-27, RN-28

### US-016 — Ver, confirmar o cancelar mi turno con el token
**Como** paciente
**Quiero** abrir mi enlace para ver, confirmar o cancelar mi turno
**Para** gestionarlo sin cuenta

**Criterios de aceptación**:
- [ ] El token solo da acceso al turno al que pertenece y es válido desde la reserva hasta el inicio del turno o su cancelación.
- [ ] El token permite ver, confirmar y cancelar; **no permite reprogramar** (eso lo hace recepción).
- [ ] Un token inexistente, vencido o revocado responde 404.
- [ ] Confirmar pasa el turno a CONFIRMADO.
- [ ] Cancelar libera el hueco y se registra como tardía si corresponde, avisando al paciente antes de confirmar.
- [ ] El token no se guarda en claro ni se registra en logs.

**Reglas relacionadas**: RN-05, RN-06, RN-07, RN-19, RN-25

---

## Épica 7 (MVP, diferenciador): Lista de espera

### US-017 — Anotar a un paciente en la lista de espera
**Como** recepción
**Quiero** anotar a un paciente con su prestación, profesional preferido y rango de fechas
**Para** poder ofrecerle un hueco cuando se libere

**Criterios de aceptación**:
- [ ] Prestación obligatoria; profesional, fechas y franja opcionales según RN-21.
- [ ] No se duplican entradas activas del mismo paciente y prestación.
- [ ] El paciente debe tener email (el aviso del hueco es por email).
- [ ] Las entradas vencidas pasan a VENCIDA.

**Reglas relacionadas**: RN-21

### US-018 — Avisar automáticamente a los pacientes compatibles al liberarse un hueco
**Como** recepción
**Quiero** que, al cancelar o reprogramar, el sistema avise solo por email a los pacientes de la lista de espera compatibles con el hueco
**Para** recuperar el turno sin buscar a mano

**Criterios de aceptación**:
- [ ] Solo se avisa a entradas compatibles (profesional, prestación, duración que cabe, fechas y franja; cualquier sillón libre sirve).
- [ ] Se avisa a todos los compatibles a la vez, hasta el tope configurado (por antigüedad si hay más), con un solo aviso por paciente.
- [ ] Cada aviso trae un enlace con token propio de la oferta (RN-38).
- [ ] Una entrada de una prestación de 90 min no recibe aviso por un hueco de 30 min.
- [ ] Recepción ve las entradas compatibles y puede asignar manualmente el hueco; la asignación crea un turno con todas las validaciones y cierra la entrada.
- [ ] Si nadie confirma antes del inicio del hueco, el hueco queda libre.

**Reglas relacionadas**: RN-06, RN-22, RN-23, RN-24, RN-31, RN-38

### US-022 — Tomar un hueco desde el aviso de la lista de espera
**Como** paciente en la lista de espera
**Quiero** abrir el enlace del aviso y confirmar el turno
**Para** quedarme con el hueco antes que otros

**Criterios de aceptación**:
- [ ] El primero que confirma se queda con el turno; se crea con todas las reglas de solapamiento (RN-01..RN-04, RN-08) y su entrada pasa a ASIGNADA.
- [ ] A quien llega después, el enlace le informa que el hueco ya fue tomado (409 al confirmar).
- [ ] Un enlace de un hueco que ya empezó informa que venció.
- [ ] No carga datos: ya está identificado; recibe el email con el token de su nuevo turno.
- [ ] El token de la oferta se guarda hasheado y no da acceso a nada más.

**Reglas relacionadas**: RN-15, RN-24, RN-25, RN-38

---

## Épica 8 (MVP): Notificaciones por email

### US-019 — Recibir un recordatorio por email
**Como** paciente
**Quiero** recibir un recordatorio antes de mi turno
**Para** no olvidarme y poder cancelar a tiempo

**Criterios de aceptación**:
- [ ] Se envía con la anticipación configurada a turnos activos con email.
- [ ] No se envía dos veces el mismo recordatorio.
- [ ] Si el turno se reprograma, se envía uno nuevo para el nuevo horario.
- [ ] Los fallos de SMTP quedan registrados y se reintentan hasta 3 veces.
- [ ] El email incluye el enlace para ver o cancelar el turno.

**Reglas relacionadas**: RN-29, RN-30, RN-31

### US-020 — Consultar el registro de notificaciones
**Como** administrador o recepción
**Quiero** ver qué emails se enviaron o fallaron
**Para** detectar problemas de entrega

**Criterios de aceptación**:
- [ ] Listado filtrable por estado y turno.
- [ ] Se ve el último error de los envíos fallidos.

**Reglas relacionadas**: RN-30

---

## Épica 9 (MVP): Calidad transversal

### US-021 — Datos ficticios y entorno reproducible
**Como** integrante del equipo
**Quiero** levantar el sistema con un solo comando y datos ficticios
**Para** desarrollar y demostrar sin riesgos

**Criterios de aceptación**:
- [ ] `docker compose up` levanta PostgreSQL con `btree_gist` disponible.
- [ ] El seed solo contiene datos inventados.
- [ ] `.env.example` versionado; `.env` fuera del repositorio.
- [ ] Los tests de integración usan PostgreSQL real con Testcontainers.

**Reglas relacionadas**: RN-35

---

## Épicas futuras (FUERA DE ALCANCE del MVP)

> Estas épicas **no** se implementan en esta entrega. Se listan para que el diseño actual no las bloquee.

| Épica | Contenido | Preparación en el diseño actual |
|---|---|---|
| **E10 (futura)**: Historia clínica | Odontograma, periodontograma, historia clínica completa (Ley 26.529) | La ficha de paciente es extensible; no se guardan datos clínicos |
| **E11 (futura)**: WhatsApp | Recordatorios y confirmaciones por WhatsApp Business API (costo por mensaje, cuenta de Meta) | Puerto de notificaciones con canal; nuevo adaptador |
| **E12 (futura)**: Cobros | Mercado Pago y señas | Estado de turno extensible |
| **E13 (futura)**: Facturación ARCA | Factura electrónica (certificado y CUIT) | n/a |
| **E14 (futura)**: Obras sociales | Validación online de obras sociales y prepagas, cobertura y liquidación | La obra social ya es un dato del paciente |
| **E15 (futura)**: Multisede y multi-tenant | Varias sedes, varios consultorios, liquidaciones y reportes | Single-tenant hoy; modelar una `Sede` implicaría una migración |
| **E16 (futura)**: Campañas | Recuperación de pacientes, recall | `NotificacionEnviada` ya registra envíos |
| **E17 (futura)**: Asistentes con IA | Agenda por chat o WhatsApp | API REST reutilizable |
