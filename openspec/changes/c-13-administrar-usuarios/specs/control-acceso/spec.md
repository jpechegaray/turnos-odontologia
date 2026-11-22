# Spec Delta

## ADDED Requirements

### Requirement: Sesión vigente solo con el usuario habilitado
Cada request con sesión del personal SHALL verificar que el usuario siga existiendo, activo y con el mismo rol y email que tenía al iniciar sesión. Si no, la sesión MUST invalidarse y el request MUST tratarse como anónimo (401 en rutas protegidas). El usuario tiene que volver a iniciar sesión. El vínculo con el profesional no se guarda en la sesión: se resuelve en cada consulta.

#### Scenario: La sesión de un usuario desactivado deja de ser válida
- **GIVEN** un usuario con sesión iniciada que consulta `GET /api/auth/me` con éxito
- **WHEN** el administrador lo desactiva y el usuario repite el request con la misma cookie de sesión
- **THEN** la respuesta tiene estado 401 y esa sesión ya no sirve aunque el usuario sea reactivado después

#### Scenario: La sesión de un usuario al que se le cambió el rol deja de ser válida
- **GIVEN** un usuario `RECEPCION` con sesión iniciada
- **WHEN** el administrador le cambia el rol a `ADMIN` y el usuario repite un request con la misma sesión
- **THEN** la respuesta tiene estado 401 y, tras iniciar sesión de nuevo, `GET /api/auth/me` informa el rol `ADMIN`

#### Scenario: La sesión de un usuario sin cambios sigue vigente
- **GIVEN** un usuario con sesión iniciada
- **WHEN** el administrador modifica solo el nombre de ese usuario y el usuario repite un request
- **THEN** la respuesta no es 401
