# Proposal

## Why

El consultorio necesita ajustar plazos y límites sin redeployar el backend. C-08 centraliza esos valores para que los módulos siguientes los consulten sin duplicar constantes ni modificar decisiones ya registradas (US-006).

## What Changes

- Persistir las siete claves del MVP y sembrar sus valores por defecto al arrancar, sin sobrescribir las claves existentes.
- Agregar `GET /api/configuracion` para `ADMIN` y `RECEPCION`, y `PUT /api/configuracion` solo para `ADMIN`. El `PUT` exige las siete claves y guarda todo en una transacción; un valor inválido deja intacta la configuración.
- Validar los rangos de RN-36 y exponer las lecturas tipadas desde `ConfiguracionServicio`. Mantener 24 horas como valor inicial del recordatorio (SU-06; PA-05 sigue abierta).
- No implementar el formulario de C-20, ni las políticas de cancelación, notificación, reserva o lista de espera que consumirán los valores. No se agrega historial ni se recalculan operaciones pasadas.

## Capabilities

### New Capabilities

- `configuracion-parametros`: valores iniciales, consulta y reemplazo de la configuración, y lectura tipada para módulos internos.

### Modified Capabilities

Ninguna. Se reutilizan `control-acceso` y `errores-api` sin modificar sus requisitos.

## Impact

- Nuevo módulo `backend/src/main/java/ar/edu/utn/turnos/configuracion/`, tabla `configuracion`, endpoints y pruebas backend.
- C-01 es la dependencia del roadmap y ya está en `master`. Las pruebas de rol pueden usar autenticación simulada sobre la cadena existente mientras C-03 no esté integrado; C-08 no agrega ni cambia el login.
- El contrato servirá a C-15, C-20, C-27, C-36, C-37, C-38 y C-41. No se cambian APIs existentes ni se agregan dependencias.
