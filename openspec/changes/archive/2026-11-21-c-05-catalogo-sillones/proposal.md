# Proposal

## Why

El consultorio necesita identificar sus lugares físicos de atención y retirar o volver a habilitar sillones sin perder la referencia histórica. La agenda necesita consultar el mismo catálogo para ofrecer únicamente sillones activos.

## What Changes

- Incorporar el catálogo `Sillon` en `profesionales.sillon`, con nombre único, descripción opcional y estado activo.
- Exponer listado y detalle de ambos estados en `/api/sillones` a `ADMIN`, `RECEPCION` y `ODONTOLOGO`. Solo `ADMIN` puede crear, editar, desactivar y reactivar. Las transiciones se hacen con `PUT`, sin borrado físico.
- Exponer `SillonConsulta` para que agenda liste solo activos y distinga por ID un sillón inactivo de uno inexistente.
- Cubrir la baja lógica de RN-37 y US-004. La guarda por turnos futuros activos queda en C-30; C-05 no agrega bloqueos por intervalo (PA-12), reglas de solapamiento (RN-01), pantallas ni rutas públicas.

## Capabilities

### New Capabilities

- `catalogo-sillones`: administración del catálogo, permisos y consultas internas para agenda.

### Modified Capabilities

## Impact

- Backend: módulo `profesionales.sillon`, tabla `sillon` administrada por Hibernate y API para la futura pantalla C-21. `SillonConsulta` es la interfaz pública del módulo para agenda.
- C-01 sigue siendo la única dependencia declarada. Se reutilizan sus contratos de acceso y errores; la integración con sesión y CSRF usa el contrato compartido de C-03 cuando esté disponible, sin modificar la seguridad global ni las specs base.
