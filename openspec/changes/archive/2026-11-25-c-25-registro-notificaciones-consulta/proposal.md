# Proposal

## Why

Desde C-15, cada email del consultorio queda registrado en `NotificacionEnviada` con su estado, la cantidad de intentos y el último error. Pero esa información solo se ve consultando la base. Recepción y administración necesitan ver qué emails se enviaron y cuáles fallaron para detectar problemas de entrega, como un SMTP caído o un email mal cargado (US-020, RN-30). Además, el front de C-34 necesita un endpoint de donde leer ese registro.

## What Changes

- Se agrega `GET /api/notificaciones`, que devuelve el registro de notificaciones paginado y ordenado de la más reciente a la más antigua.
- Filtros opcionales y combinables: `estado` (`PENDIENTE`, `ENVIADA` o `FALLIDA`) y `turnoId`. Un estado desconocido responde `400` y un turno sin notificaciones devuelve una página vacía.
- Paginación con `pagina` (empieza en 0; por defecto 0) y `tamanio` (por defecto 20, entre 1 y 100). Un valor fuera de rango responde `400`. La respuesta trae `contenido`, `pagina`, `tamanio`, `totalElementos` y `totalPaginas`. Es el primer endpoint paginado del proyecto, así que deja definido el formato que van a seguir los próximos.
- Cada elemento muestra tipo, estado, canal, destinatario, ids de turno, oferta de hueco y paciente, inicio de referencia, intentos, programación, fecha de envío y `ultimoError`. Las `FALLIDA` traen el último error; las demás lo devuelven vacío.
- Solo `ADMIN` y `RECEPCION` pueden consultarlo. `ODONTOLOGO` recibe `403` y un pedido sin sesión, `401`.
- Se agregan tests de integración con PostgreSQL real (Testcontainers) para filtros, paginación, orden, validación y permisos. Cada test lleva en `@DisplayName` el nombre exacto de su escenario.

## Capabilities

### New Capabilities
- `registro-notificaciones`: consulta paginada y filtrable del registro de notificaciones enviadas por el personal autorizado.

### Modified Capabilities
- Ninguna. `notificaciones-email` (C-15) ya define cómo se registra cada envío; este change solo agrega su consulta y no cambia cómo se registra, se reintenta ni se planifica.

## Impact

- **Backend**:
  - Nuevos `shared/notificacion/web/` (controller y DTO de respuesta) y un caso de uso de consulta en `shared/notificacion/application/`.
  - El puerto `NotificacionRepositorio` y su adaptador JPA suman una búsqueda paginada con filtros.
  - Se agrega un tipo genérico de página en `shared/`, que van a reutilizar los próximos listados paginados.
  - `SecurityConfig` suma la regla de `GET /api/notificaciones` para `ADMIN` y `RECEPCION`.
- **API**: un endpoint nuevo de solo lectura. No cambia ningún contrato existente.
- **Base de datos**: sin cambios de esquema. El filtro por estado aprovecha el índice `(estado, programada_para)` y el filtro por turno, la restricción única que empieza por `turno_id`.
- **Frontend**: sin cambios; la pantalla la hace C-34.
- **Dependencias**: C-15, ya mergeado y archivado.
- **Fuera de alcance**: reenviar manualmente una notificación fallida, filtrar por paciente, tipo o fechas, y la pantalla del front.
