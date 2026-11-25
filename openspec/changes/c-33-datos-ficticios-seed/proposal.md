# Proposal

## Why

El arranque de desarrollo permite iniciar sesión, pero requiere cargar a mano los catálogos y la agenda para demostrar los flujos del consultorio. C-33 agrega una carga reproducible de datos ficticios (US-021, RN-35).

## What Changes

- Completar el entorno local con personal, profesionales vinculados, sillones, prestaciones, horarios, pacientes y turnos.
- Activar la carga de demostración en el perfil dev al configurar SEED_STAFF_PASSWORD, con los valores de configuración que ya inicializa la aplicación.
- Dejar intactas las bases que ya tienen datos operativos y conservar el contrato de inicialización del administrador.

## Capabilities

### New Capabilities

- `datos-demostracion`: carga de un consultorio ficticio al iniciar una base de desarrollo vacía.

### Modified Capabilities

Ninguna.

## Impact

Backend de desarrollo y documentación de arranque. Sin endpoints, dependencias ni cambios de esquema.
