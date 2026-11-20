# Flujos Principales

Cada flujo se documenta extremo a extremo. Los componentes siguen los módulos de `08_arquitectura_propuesta.md`. Siempre que se valida un horario se aplican RN-01, RN-02 y RN-04; la base de datos es la última defensa (RN-15).

## Flujo 1: Crear un turno con validación de solapamiento

**Disparador**: recepción confirma el formulario de nuevo turno.
**Actor**: Recepción (o ADMIN).

**Pasos**:
1. SPA (`features/agenda`) pide `GET /api/disponibilidad` con profesional, prestación y rango, para mostrar horarios libres sugeridos.
2. Recepción elige paciente (buscado o creado en `pacientes`), profesional, sillón, prestación, inicio y, opcionalmente, ajusta la duración (RN-03).
3. SPA envía `POST /api/turnos`.
4. `agenda.web` valida el formato (Bean Validation) y verifica el rol (RN-33).
5. `agenda.application.CrearTurnoService` carga los datos necesarios (profesional, sillón, prestación activos, horarios, bloqueos y turnos activos cercanos).
6. `agenda.domain` (funciones puras) evalúa en orden: recursos activos y prestación realizable (RN-11), inicio no pasado (RN-13), granularidad y duración (RN-09), horario contenido en un tramo (RN-16), no dentro de bloqueo (RN-04), sin solapamiento por profesional y por sillón con intervalos `[inicio, fin)` (RN-01, RN-02).
7. Si todo es válido, persiste el turno (`PROGRAMADO`, canal `INTERNO`, `creado_por_usuario_id`) dentro de una transacción.
8. PostgreSQL aplica `EXCLUDE USING gist` (profesional y sillón). Si otra transacción ocupó el hueco entre el paso 5 y el 7, la restricción falla y la capa de aplicación traduce el error a `409` (RN-15).
9. Se encola la notificación de confirmación si el paciente tiene email (RN-31) y se responde `201` con el turno.

**Diagrama de secuencia**:
```
Recepción → SPA → API(agenda.web) → CrearTurnoService → Dominio (reglas puras)
                                          │                   ← válido / error de regla (422)
                                          ├→ TurnoRepository → PostgreSQL (EXCLUDE gist)
                                          │                   ← OK / violación de restricción
                                          └→ NotificacionPort (si hay email)
                  ← 201 Turno | 409 Conflicto | 422 Regla incumplida
```

**Casos de error**:
- Solapamiento por profesional o por sillón → `409`, con indicación del eje en conflicto.
- Fuera de horario, dentro de un bloqueo, en el pasado o recurso inactivo → `422` con código de regla.
- Dos recepciones sobre el mismo hueco a la vez → una gana, la otra recibe `409` (RN-15).
- Paciente inexistente o inactivo → `404` / `422`.

## Flujo 2: Reprogramar un turno

**Disparador**: recepción cambia el horario, profesional o sillón de un turno existente.
**Actor**: Recepción (o ADMIN).

**Pasos**:
1. SPA envía `PUT /api/turnos/{id}/reprogramar` con el nuevo horario y recursos.
2. El servicio carga el turno y verifica que esté `PROGRAMADO` o `CONFIRMADO` (RN-12).
3. El dominio revalida RN-01, RN-02, RN-04, RN-11, RN-13 **excluyendo el propio turno** (RN-18).
4. Calcula si es reprogramación tardía: `(inicio_original - ahora) < plazo_horas` → marca `reprogramado_tarde` (RN-05, RN-18).
5. Actualiza el turno (con bloqueo optimista por `version`); si la restricción de exclusión falla, responde `409`.
6. Revoca/recalcula la expiración del token existente según el nuevo `inicio` (RN-25).
7. Registra el hueco liberado y dispara el aviso automático a la lista de espera compatible (Flujo 3, pasos 5 a 7; RN-23).
8. Envía email de reprogramación al paciente (RN-31); el recordatorio se reprograma porque cambia `inicio_referencia` (RN-30).

**Casos de error**:
- Turno cancelado, atendido o ausente → `422`.
- Edición concurrente del mismo turno (versión desactualizada) → `409`.
- Nuevo horario ocupado → `409`.

## Flujo 3: Cancelar y liberar el hueco hacia la lista de espera

**Disparador**: recepción cancela un turno (o el paciente lo cancela por token, Flujo 4). El mismo paso de "hueco liberado" lo dispara una reprogramación (Flujo 2).
**Actor**: Recepción, Administrador o Paciente (token).

**Pasos**:
1. SPA envía `POST /api/turnos/{id}/cancelar` con un motivo opcional.
2. El servicio verifica que el turno esté `PROGRAMADO` o `CONFIRMADO` y que no haya empezado (RN-05).
3. `PoliticaCancelacion` (dominio) calcula `tardia = (inicio - ahora) < cancelacion.plazo_horas` usando el `Clock` inyectado (RN-05).
4. El turno pasa a `CANCELADO` con `cancelado_en`, `cancelado_por`, `cancelacion_tardia` y motivo (RN-19). Al quedar fuera de las restricciones `EXCLUDE`, el intervalo queda libre (RN-06, RN-10).
5. `listaespera.application` recibe el evento "hueco liberado" (profesional, intervalo; el sillón es indistinto) y evalúa las entradas `ACTIVA` con la función pura de compatibilidad (RN-22).
6. **Aviso automático (RN-23):** por cada entrada compatible (hasta `lista_espera.max_avisos_por_hueco`, por antigüedad, un aviso por paciente) crea una `OfertaHueco` con su token (hash SHA-256, vigente hasta el inicio del hueco, RN-38) y envía un email con el enlace `…/oferta/{token}` (`OFERTA_HUECO`, RN-31). La respuesta de la cancelación incluye el turno cancelado y la lista de entradas compatibles avisadas.
7. Quien confirme primero se queda con el turno (Flujo 7). Recepción puede además asignar manualmente (`POST /api/lista-espera/{id}/asignar`): aplica el Flujo 1 sobre ese hueco y compite bajo RN-15.
8. Se envía email de cancelación al paciente si el turno fue cancelado por el personal (RN-31); el token del turno queda revocado en toda cancelación (RN-25).

**Diagrama de secuencia**:
```
Recepción → API → CancelarTurnoService → PoliticaCancelacion (¿tardía?)
                        ├→ TurnoRepository (estado = CANCELADO)
                        └→ ListaEsperaService → Compatibilidad (pura) → [entradas compatibles]
                                           └→ OfertaHueco (token hash) + NotificacionPort → email a cada una
            ← 200 {turno, tardia, avisados}
Primer paciente que confirma → /oferta/{token}/tomar → Flujo 7 (crear turno)
(opcional) Recepción → POST /lista-espera/{id}/asignar → Flujo 1 (crear turno)
```

**Casos de error**:
- Turno ya cancelado o finalizado → `422` (la operación es idempotente a nivel de resultado).
- No hay entradas compatibles → no se avisa a nadie; el hueco queda libre para reserva online (RN-08).
- Nadie confirma antes del inicio del hueco → el hueco queda libre y las ofertas vencen (RN-24).
- El hueco fue tomado por una reserva online o por recepción antes de confirmar → `409` al tomar la oferta, con el mensaje "hueco ya tomado" (RN-15).

## Flujo 4: Reserva online con token

**Disparador**: el paciente abre el enlace público de reserva.
**Actor**: Paciente (anónimo).

**Pasos**:
1. SPA pública (`/reservar`) pide `GET /api/public/prestaciones` y `GET /api/public/profesionales`; el paciente elige prestación y profesional.
2. SPA pide `GET /api/public/disponibilidad`; el backend calcula los horarios libres aplicando RN-01 a RN-04 y RN-27 (horizonte y anticipación mínima) con la duración por defecto de la prestación, sin devolver datos de otros pacientes.
3. El paciente elige un horario y completa nombre completo, email, teléfono y obra social (texto libre), y acepta el tratamiento de datos (RN-26). No se pide DNI.
4. SPA envía `POST /api/public/reservas`. Se aplica rate limit por IP y se verifica el tope de turnos activos por email (RN-27).
5. `reservapublica.application` resuelve el paciente (por email, y teléfono si coincide: reutiliza o crea según RN-28), **elige un sillón libre** para el intervalo (**Suposición:** el paciente no elige sillón; el sistema asigna el primer sillón activo libre) y crea el turno con canal `ONLINE` invocando el mismo servicio del Flujo 1.
6. Si la restricción de exclusión falla, responde `409` con nuevas opciones (RN-08, RN-15).
7. Genera un token de 256 bits con `SecureRandom`, guarda su hash SHA-256 y la expiración (RN-25).
8. Envía email de confirmación con el enlace `…/turno/{token}` y devuelve el token una única vez en la respuesta. El token vale hasta el inicio del turno o hasta que se cancele (RN-25).
9. El paciente abre `GET /api/public/turnos/{token}`: el backend hashea, busca, verifica expiración y revocación, y devuelve **solo ese turno** (RN-07). Con `POST …/confirmar` pasa a `CONFIRMADO`; con `POST …/cancelar` se ejecuta el Flujo 3 (con `cancelado_por = PACIENTE_TOKEN`), avisando antes si será tardía. **No hay endpoint para reprogramar con el token**: si el paciente quiere otro horario, cancela y reserva de nuevo, o llama a recepción.

**Diagrama de secuencia**:
```
Paciente → SPA pública → API(/public) → ReservaService → Flujo 1 (crear turno, canal ONLINE)
                                              ├→ TokenService (hash SHA-256)
                                              └→ NotificacionPort → email con enlace
Paciente → /turno/{token} → API → TokenService (hash→lookup→vigencia) → Turno (solo el suyo)
```

**Casos de error**:
- Horario ocupado entre la consulta y la reserva → `409` con alternativas.
- Demasiadas reservas desde la IP o el email → `429` / `422` (RN-27).
- Token inexistente, vencido o revocado → `404` sin diferenciar causa (RN-25).
- Email inválido o falla de SMTP al enviar → la reserva **no** se pierde: el turno queda creado, la notificación queda `FALLIDA` y se reintenta (RN-30); el token se muestra igual en pantalla.

## Flujo 5: Recordatorio por email

**Disparador**: planificador (`@Scheduled`, cada 5 minutos).
**Actor**: Sistema.

**Pasos**:
1. `notificaciones.application.PlanificadorRecordatorios` consulta turnos `PROGRAMADO` o `CONFIRMADO` con `inicio` dentro de la ventana `[ahora, ahora + anticipacion_horas]`, cuyo paciente tenga email y que no tengan una `NotificacionEnviada` del tipo `RECORDATORIO` con el mismo `inicio_referencia` (RN-29, RN-30).
2. Por cada turno inserta un registro `PENDIENTE` (la restricción única `(turno, tipo, inicio_referencia)` evita duplicados aun con dos ejecuciones simultáneas).
3. Arma el email (nombre del consultorio, fecha y hora en hora de Buenos Aires, prestación, profesional y enlace de token para ver o cancelar) y llama al puerto `NotificacionPort.enviar(...)`.
4. El adaptador SMTP envía con Spring Mail. Éxito → `ENVIADA` con `enviada_en`. Error → `FALLIDA`, incrementa `intentos` y guarda `ultimo_error`.
5. Un segundo paso reintenta los `FALLIDA` con menos de 3 intentos tras una espera creciente (RN-30).
6. Si el turno se reprograma o cancela antes del envío, el registro pendiente se descarta.

**Diagrama de secuencia**:
```
Scheduler → PlanificadorRecordatorios → TurnoRepository (ventana)
                  ├→ NotificacionRepository (UNIQUE turno,tipo,inicio_ref)
                  └→ NotificacionPort ── SmtpAdapter → servidor SMTP
                                       └─ (tests) InMemoryAdapter
```

**Casos de error**:
- SMTP caído o credenciales inválidas → `FALLIDA` visible en el registro (US-020); no se pierde el turno.
- Paciente sin email → se omite sin error.
- Ejecución duplicada del planificador → la restricción única impide el doble envío.
- Servicio reiniciado → los `PENDIENTE` se retoman en el siguiente ciclo.

## Flujo 6: Login del personal

**Disparador**: el usuario envía el formulario de login.
**Actor**: Odontólogo, Recepción o Administrador.

**Pasos**:
1. SPA obtiene el token CSRF (cookie `XSRF-TOKEN`) y envía `POST /api/auth/login` con email y contraseña.
2. `seguridad.application` busca el usuario por email (en minúsculas). Si está bloqueado (`bloqueado_hasta` futuro) o inactivo, responde con el error genérico (RN-34).
3. Verifica la contraseña con BCrypt. Si falla, incrementa `intentos_fallidos`; al llegar a 5 fija `bloqueado_hasta` a 15 minutos.
4. Si es correcta, reinicia el contador y Spring Security crea la sesión (cookie `JSESSIONID`, `HttpOnly`, `SameSite=Lax`, `Secure` en producción).
5. Responde con el usuario y su rol; la SPA guarda el rol en memoria y redirige: ODONTOLOGO a su agenda, RECEPCION a la agenda general, ADMIN a configuración o agenda.
6. Cada request posterior valida sesión y rol (`@PreAuthorize`); el rol ODONTOLOGO es solo lectura y recibe `403` ante cualquier escritura (RN-33). `POST /api/auth/logout` invalida la sesión.

**Diagrama de secuencia**:
```
Usuario → SPA → POST /api/auth/login → AuthService → UsuarioRepository
                                           ├→ BCrypt.matches
                                           └→ SecurityContext + HttpSession
        ← 200 {usuario, rol} + Set-Cookie JSESSIONID | 401 genérico
```

**Casos de error**:
- Credenciales incorrectas o usuario inexistente → `401` con el mismo mensaje.
- Cuenta bloqueada → `401` genérico (no se revela el bloqueo) y se registra el evento.
- Sesión vencida → `401`; la SPA redirige al login.
- Rol insuficiente → `403`.

## Flujo 7: Tomar un hueco ofrecido por la lista de espera

**Disparador**: el paciente abre el enlace `…/oferta/{token}` del email de aviso (Flujo 3, paso 6).
**Actor**: Paciente en lista de espera (anónimo, con token de oferta).

**Pasos**:
1. SPA pública (`/oferta/:token`) pide `GET /api/public/ofertas/{token}`; el backend hashea, busca la `OfertaHueco` y devuelve fecha, hora, profesional y prestación del hueco y su estado (RN-38).
2. Si el hueco ya fue tomado o ya empezó, la pantalla lo informa y no ofrece confirmar.
3. El paciente pulsa confirmar: `POST /api/public/ofertas/{token}/tomar`.
4. El servicio intenta crear el turno con el Flujo 1 (canal `ONLINE`): inicio = inicio del hueco, duración por defecto de la prestación, sillón activo libre; valida RN-01..RN-04, RN-08, RN-09, RN-11, RN-13 (RN-24).
5. Si la creación tiene éxito: la oferta pasa a `TOMADA`, la entrada a `ASIGNADA` (con `turno_asignado_id`), las demás ofertas del hueco quedan informativas ("hueco ya tomado"), se genera el token del nuevo turno (RN-25) y se envía el email de confirmación (RN-31). Responde `201` con el turno.
6. Si la restricción de exclusión falla (otro confirmó antes, o lo tomó una reserva online o recepción) → `409` con el mensaje "hueco ya tomado" (RN-15).

**Casos de error**:
- Token inexistente → `404`.
- Hueco vencido (empezó sin confirmar) → la oferta pasa a `VENCIDA`; el hueco queda libre.
- Falla de SMTP al enviar la confirmación → el turno no se pierde; la notificación queda `FALLIDA` y se reintenta (RN-30).

