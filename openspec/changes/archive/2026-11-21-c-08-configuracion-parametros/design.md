# Design

## Context

C-01 ya provee `Clock`, Hibernate, PostgreSQL, la política de seguridad por defecto y el manejador Problem Details. C-08 no tiene implementación previa. El alcance se describe en [proposal.md](proposal.md) y el contrato en [la especificación](specs/configuracion-parametros/spec.md).

## Goals / Non-Goals

**Goals:** Una fuente de verdad para claves, valores iniciales y restricciones; un único límite transaccional para el reemplazo; lecturas internas independientes de HTTP y de roles.

**Non-Goals:** No agregar caché, historial, login, cambios globales de seguridad ni dependencias de otros módulos.

## Decisions

1. **Catálogo cerrado y persistencia clave-valor.** Definir las siete entradas en un único catálogo con su clave, valor inicial, descripción y validación. Mapear `Configuracion` con `clave` como PK de texto, `valor` y `descripcion` no nulos y `actualizado_en` como `Instant`/`timestamptz` obtenido del `Clock` de C-01. Hibernate gestiona la tabla (DD-08). Un inicializador de arranque transaccional inserta solo claves ausentes después de que el esquema esté disponible. No usar YAML como fuente de valores vigentes ni sobrescribir filas al iniciar.
2. **Contrato JSON explícito y validación local.** Usar adaptadores de entrada/salida para el objeto plano, nunca entidades JPA. Verificar localmente que el objeto tiene exactamente las claves esperadas y los tipos JSON requeridos. Detectar claves duplicadas antes de perderlas en la conversión a un mapa; no cambiar Jackson globalmente. No depender de coerciones hacia `Integer` o `String`: validar los números antes de convertirlos, incluidos integralidad y rango representable. Separar los 400 de estructura de `ReglaNegocioException` 422 con `RN-36`; reutilizar el contrato Problem Details de C-01.
3. **Reemplazo en un caso de uso transaccional.** Validar el objeto completo antes de escribir y bloquear las siete filas en orden de clave dentro de la transacción antes de comparar valores. Así dos PUT concurrentes no mezclan cambios calculados contra una misma versión anterior. Persistir solo los valores distintos con un mismo instante. No hacer una transacción por clave ni actualizar metadatos si no hay cambios. La respuesta usa los valores resultantes, no devuelve descripciones ni timestamps.
4. **Puerta de entrada autorizada y lectura interna independiente.** Declarar `GET` para `ADMIN`/`RECEPCION` y `PUT` solo para `ADMIN` con seguridad por método en el límite web/caso de uso administrativo, conservando la cadena global y CSRF de C-01. `ConfiguracionServicio` expone `plazoCancelacionHoras()`, `anticipacionRecordatorioHoras()`, `horizonteReservaOnlineDias()`, `anticipacionMinimaReservaOnlineHoras()`, `maxTurnosActivosPorEmail()`, `maxAvisosPorHueco()` y `nombreConsultorio()`. Consultar persistencia sin caché; llamar al servicio al decidir una operación nueva, no al releer un resultado histórico. No usar los defaults como sustituto silencioso de una fila que falta tras arrancar.

## Risks / Trade-offs

- **PostgreSQL no admite `U+0000` en columnas de texto** → Validar el nombre antes de escribir y responder 422 con `RN-36`, conservando todo el estado anterior.
- **DTOs o tipos JSON coercionados** → Probar explícitamente números en campos de texto, strings en campos numéricos, duplicados y fracciones sin afectar otros endpoints.
- **Claves editadas sobrescritas al reiniciar** → Probar la reinicialización sobre PostgreSQL real, preservando valor y fecha existentes.
- **Dos administradores editan un formulario desactualizado** → El MVP no agrega versionado de edición: prevalece el último reemplazo confirmado. El bloqueo ordenado evita mezclar valores de dos escrituras concurrentes.
- **C-03 aún no está en `master`** → Probar permisos con autenticación simulada, CSRF de prueba y la cadena real de la base correspondiente; no modificar el login para habilitar C-08.

## Migration Plan

En dev y tests, Hibernate crea la tabla y el inicializador completa las siete claves sin tocar las existentes. Si se despliega con `ddl-auto=validate`, provisionar la tabla antes del arranque, conforme a DD-08. Un rollback de la aplicación no requiere borrar la tabla ni los valores guardados.

## Open Questions

- PA-05 sigue pendiente para C-37. C-08 entrega el valor inicial configurable de 24 horas y no implementa el envío.
