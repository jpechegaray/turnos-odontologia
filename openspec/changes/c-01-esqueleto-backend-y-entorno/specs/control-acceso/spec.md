# Spec Delta

## Purpose

Define la política base de acceso a la API. Todo está denegado por defecto salvo la zona pública, y las respuestas de acceso denegado tienen formato uniforme. Los módulos declaran sus permisos por rol sobre esta base, y el login del personal (C-03) la extiende sin cambiar su comportamiento.

## ADDED Requirements

### Requirement: Denegación por defecto
Toda ruta de la API que no esté declarada como pública SHALL requerir un usuario autenticado. Una petición anónima a una ruta protegida MUST recibir 401 en formato Problem Details, exista o no la ruta.

#### Scenario: Una petición anónima a una ruta protegida recibe 401
- **GIVEN** un cliente sin sesión
- **WHEN** envía `GET /api/turnos`
- **THEN** la respuesta tiene estado 401 y formato Problem Details

#### Scenario: Una ruta protegida inexistente también recibe 401
- **GIVEN** un cliente sin sesión
- **WHEN** envía `GET /api/ruta-que-no-existe`
- **THEN** la respuesta tiene estado 401 y no 404

### Requirement: Zona pública abierta
Las rutas bajo `/api/public/**` SHALL ser accesibles sin autenticación ni sesión, también para métodos de escritura, porque las usa la reserva online del paciente.

#### Scenario: Una ruta pública responde sin autenticación
- **GIVEN** un cliente sin sesión y una ruta pública existente bajo `/api/public/`
- **WHEN** el cliente la invoca con GET
- **THEN** la respuesta no es 401 ni 403

#### Scenario: Una escritura pública no exige token CSRF
- **GIVEN** un cliente sin sesión y sin token CSRF
- **WHEN** envía un POST a una ruta pública existente bajo `/api/public/`
- **THEN** la respuesta no es 401 ni 403

### Requirement: Permisos por rol con formato uniforme
La API SHALL permitir que cada operación declare los roles que la pueden ejecutar. Un usuario autenticado sin el rol requerido MUST recibir 403 en formato Problem Details (base de RN-33).

#### Scenario: Un usuario sin el rol requerido recibe 403 Problem Details
- **GIVEN** un usuario autenticado con rol `ODONTOLOGO` y una operación reservada al rol `RECEPCION`
- **WHEN** el usuario invoca esa operación
- **THEN** la respuesta tiene estado 403 y formato Problem Details

#### Scenario: Un usuario con el rol requerido accede a la operación
- **GIVEN** un usuario autenticado con rol `RECEPCION` y una operación reservada al rol `RECEPCION`
- **WHEN** el usuario invoca esa operación
- **THEN** la operación se ejecuta y la respuesta es 200
