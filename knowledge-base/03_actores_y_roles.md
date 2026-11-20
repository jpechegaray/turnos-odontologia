# Actores y Roles

## Actores del sistema

| Actor | Rol técnico | Descripción | Cómo interactúa |
|---|---|---|---|
| Odontólogo | `ODONTOLOGO` | **Eje del producto.** Profesional que atiende. Cada odontólogo con acceso al sistema tiene un usuario vinculado 1:1 a su ficha de Profesional. | Ve su agenda diaria y semanal (por profesional y por sillón) y la ficha básica de sus pacientes. **Es de solo lectura**: no da, reprograma ni cancela turnos, no marca asistencia o ausencia y no define horarios ni bloqueos. Si necesita un cambio, se lo pide a recepción **fuera del sistema**. |
| Recepción | `RECEPCION` | Personal administrativo. **Su uso tiene que ser lo más simple posible**: pocas pantallas, pocas decisiones. | Crea, reprograma y cancela turnos de todos los profesionales; **marca asistencia o ausencia**; **gestiona horarios de atención y bloqueos** de todos los profesionales (a pedido del odontólogo); gestiona fichas de pacientes; supervisa la lista de espera. |
| Administrador | `ADMIN` | Responsable del consultorio. | Configura prestaciones y duraciones, sillones/boxes, profesionales, usuarios y plazo de cancelación. Puede hacer todo lo que hace recepción y es el único que corrige una asistencia mal marcada (RN-20). |
| Paciente | (anónimo, sin rol) | No tiene login ni cuenta. Queda representado por un **token** (enlace único) asociado a su turno. | Reserva online desde un enlace público; con el token ve, confirma o cancela **solo su turno** (RN-07); **no puede reprogramar** (lo hace solo recepción). Si está en la lista de espera, recibe por email un enlace con token para tomar un hueco (RN-38). |
| Sistema (planificador) | n/a | Proceso interno programado. | Envía recordatorios, avisa a la lista de espera cuando se libera un hueco (RN-23) y reintenta emails fallidos (RN-29, RN-30). |

**Decisión del líder (2026-11-20, resuelve PA-03):** el odontólogo es **solo lectura**. Ve su agenda y la ficha básica de sus pacientes, pero no modifica nada. Las acciones que antes tenía se reasignan así:

- **Asistencia o ausencia → recepción** (y ADMIN para corregir). Recepción está en el consultorio durante la atención y es quien ya opera la agenda día a día.
- **Horarios de atención y bloqueos → recepción y administrador.** Los cambios de agenda impactan en turnos existentes (RN-17) y recepción es quien reprograma o avisa a los pacientes; centralizarlos ahí evita que se creen conflictos sin verlos. El administrador conserva el control total. **Suposición (SU-02):** ambos roles tienen CRUD sobre horarios y bloqueos de todos los profesionales; si se prefiere que recepción no toque horarios (cambian poco), solo cambia la matriz.

## RBAC: matriz de permisos

Leyenda: **C** crear, **R** leer, **U** modificar, **D** baja (lógica salvo indicación), **—** sin acceso. "propios" significa los recursos del profesional vinculado al usuario.

| Recurso | ODONTOLOGO | RECEPCION | ADMIN |
|---|---|---|---|
| Turnos: ver agenda | R (todos los profesionales y sillones; **Suposición:** lectura global para coordinar) | R (todos) | R (todos) |
| Turnos: crear / reprogramar / cancelar | — | C U (cancelar = U) | C U |
| Turnos: marcar asistencia o ausencia | — | U | U (también corrige) |
| Pacientes (ficha básica) | R (solo pacientes de sus turnos) | C R U | C R U D |
| Horarios de atención | R (propios) | CRUD de todos | CRUD de todos |
| Bloqueos | R (propios) | CRUD de todos | CRUD de todos |
| Profesionales | R (propio) | R | CRUD |
| Sillones/boxes | R | R | CRUD |
| Prestaciones | R | R | CRUD |
| Lista de espera | R | C R U D | C R U D |
| Usuarios del personal | — | — | CRUD |
| Configuración (plazo de cancelación, recordatorio, etc.) | — | R | R U |
| Notificaciones enviadas (registro) | — | R | R |

Reglas de aplicación:

- La autorización se aplica en el backend con Spring Security (`@PreAuthorize` por método de servicio y reglas por ruta); la SPA solo oculta opciones, nunca es la barrera.
- El rol `ODONTOLOGO` es **solo lectura**: el backend responde `403` a cualquier operación de escritura (alta, cambio, baja, asistencia, horarios, bloqueos), salvo login y logout. En la lectura de pacientes compara el usuario autenticado con `profesional.usuario_id` para limitar a los pacientes de sus turnos.
- No existe borrado físico de turnos, pacientes ni profesionales con historial: se usa baja lógica o estado (RN-37).

## Rutas públicas

Accesibles sin autenticación (sujetas a rate limit, RN-27):

| Ruta | Propósito |
|---|---|
| `POST /api/auth/login` | Login del personal |
| `GET /api/public/prestaciones`, `GET /api/public/profesionales` | Catálogo para la reserva online (solo datos no sensibles) |
| `GET /api/public/disponibilidad` | Horarios libres (sin datos de otros pacientes) |
| `POST /api/public/reservas` | Crear una reserva online (RN-08) |
| `GET /api/public/turnos/{token}` | Ver el turno propio |
| `POST /api/public/turnos/{token}/confirmar` | Confirmar el turno propio |
| `POST /api/public/turnos/{token}/cancelar` | Cancelar el turno propio (el paciente no reprograma) |
| `GET /api/public/ofertas/{token}` | Ver la oferta de un hueco liberado (o su estado: tomado, vencido) |
| `POST /api/public/ofertas/{token}/tomar` | Confirmar la oferta: el primero que confirma se queda con el turno (RN-24, RN-38) |
| Rutas de la SPA: `/reservar`, `/turno/:token`, `/oferta/:token` | Pantallas públicas de reserva y gestión con token |
| `GET /actuator/health` | Salud del servicio (**Suposición:** expuesto sin detalle) |

Todo lo demás requiere sesión de personal. Los endpoints `/api/public/**` no usan la cookie de sesión y quedan exentos de CSRF; el acceso al turno o a la oferta se decide únicamente por el token (RN-07, RN-25, RN-38).
