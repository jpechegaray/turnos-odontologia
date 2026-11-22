# Proposal

## Why

Hoy el único usuario del sistema es el administrador de desarrollo que crea C-03. No hay forma de dar de alta a recepción ni a los odontólogos, ni de quitarle el acceso a alguien que deja el consultorio. Además, C-06 dejó preparado el acceso del odontólogo a su propio profesional, pero falta el vínculo usuario → profesional: mientras no exista, todo odontólogo recibe 403. C-13 implementa US-002 y desbloquea C-24 (pantalla de usuarios).

## What Changes

- `/api/usuarios`, solo para ADMIN (RN-33): listar, consultar, dar de alta con rol y contraseña inicial, modificar (nombre, email, rol, vínculo) y desactivar o reactivar. El email es único sin distinguir mayúsculas. La contraseña se guarda con BCrypt (RN-34). La API nunca devuelve el hash.
- **Vínculo con el profesional:** un usuario `ODONTOLOGO` se vincula 1:1 con un profesional activo, validado por el puerto `ProfesionalConsulta` de C-06. Los otros roles no tienen vínculo. C-13 provee la implementación de `ProfesionalVinculadoConsulta` que C-06 ya consume, así el odontólogo vinculado puede ver su propio registro.
- **Sesiones al desactivar:** desactivar un usuario, o cambiarle el rol o el email, deja sin efecto sus sesiones abiertas desde el siguiente request. Un cambio de vínculo rige de inmediato, porque se resuelve en cada consulta.
- **Protección del último ADMIN:** no se puede desactivar ni quitar el rol al último administrador activo.
- Fuera de alcance: restablecer o cambiar la contraseña después del alta, desbloquear manualmente una cuenta (el bloqueo de RN-34 vence solo), la pantalla de administración (C-24) y el seed de usuarios ficticios (C-33).

## Capabilities

### New Capabilities

- `administracion-usuarios`: gestión de las cuentas del personal por el administrador (alta, modificación, desactivación, rol y vínculo con el profesional) y la consulta del vínculo para otros módulos.

### Modified Capabilities

- `control-acceso`: se agrega que una sesión abierta deja de ser válida cuando su usuario se desactiva o cambia de rol o de email. Los requisitos existentes de C-01 y C-03 no cambian.

## Impact

- `backend/`, módulo `seguridad`. **Archivos existentes que se modifican, todos estrictamente necesarios y dentro del mismo módulo, como prevé la ficha ("Edita `Usuario` de C-03"):**
  - `Usuario.java`: agrega `profesional_id` (único, nullable) y métodos de modificación y desactivación.
  - `UsuarioJpaRepository.java`: agrega consultas sin bloqueo pesimista.
  - `IdentidadPersonal.java`: implementa `java.security.Principal`, así `getName()` devuelve el id del usuario en lugar del `toString()` del record, que es lo que hoy recibe C-06. El JSON de `/api/auth/me` no cambia.
  - `SecurityConfig.java`: agrega la regla por ruta `/api/usuarios/**` solo para ADMIN (el mismo patrón que usó C-12) y el filtro que revalida la sesión.
- **Discrepancia con la KB:** KB 04 dibuja `usuario_id` en `Profesional`, pero la ficha de C-13 lo pone como `profesional_id` en `Usuario`. Se sigue la ficha, porque C-06 no creó esa columna y así el vínculo queda dentro de `seguridad` sin tocar el módulo `profesionales`. Si se aprueba, hay que actualizar KB 04.
- **Gobernanza CRITICAL:** este change no escribe código hasta que Juan Pablo apruebe explícitamente esta propuesta.
