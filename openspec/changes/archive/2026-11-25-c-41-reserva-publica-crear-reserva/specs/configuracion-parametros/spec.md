# Spec Delta

## MODIFIED Requirements

### Requirement: Valores iniciales de la configuración

Al arrancar, el sistema SHALL completar las claves faltantes de la tabla siguiente sin alterar el valor ni la fecha de actualización de las claves que ya existen.

| Clave | Valor por defecto | Valores admitidos |
|---|---|---|
| `cancelacion.plazo_horas` | `24` | Entero de 0 a 168 inclusive |
| `recordatorio.anticipacion_horas` | `24` | Entero de 1 a 168 inclusive |
| `reserva_online.horizonte_dias` | `60` | Entero positivo |
| `reserva_online.anticipacion_minima_horas` | `2` | Entero positivo |
| `reserva_online.max_turnos_activos_por_email` | `3` | Entero positivo |
| `lista_espera.max_avisos_por_hueco` | `10` | Entero positivo |
| `consultorio.nombre` | `"Consultorio Ficticio"` | Texto no blanco y sin el carácter nulo `U+0000` |
| `consultorio.domicilio` | `"Av. Ficticia 1234, Ciudad Autónoma de Buenos Aires"` | Texto no blanco y sin el carácter nulo `U+0000` |
| `consultorio.email_contacto` | `"contacto@consultorio-ficticio.example"` | Texto no blanco y sin el carácter nulo `U+0000` |
| `consultorio.retencion_anios` | `2` | Entero de 1 a 20 inclusive |

#### Scenario: El primer arranque inicializa los siete valores por defecto
- **GIVEN** una base de datos sin parámetros
- **WHEN** la aplicación termina de arrancar
- **THEN** existen exactamente las diez claves con los valores por defecto de la tabla

#### Scenario: Los parámetros nuevos del consultorio tienen su valor por defecto
- **GIVEN** una base de datos que ya tenía las siete claves anteriores con valores editados
- **WHEN** la aplicación termina de arrancar
- **THEN** `consultorio.domicilio`, `consultorio.email_contacto` y `consultorio.retencion_anios` existen con sus valores por defecto, y las siete claves anteriores conservan su valor y su fecha

#### Scenario: La inicialización repone solo claves faltantes
- **GIVEN** una clave editada y otra ausente
- **WHEN** se vuelve a ejecutar el inicializador de arranque
- **THEN** se restaura la clave ausente con su valor por defecto, sin duplicar claves ni cambiar el valor o fecha de la clave editada

### Requirement: Consulta de configuración por personal autorizado

`GET /api/configuracion` SHALL devolver 200 a `ADMIN` y `RECEPCION`, con un objeto JSON plano cuyas propiedades son exactamente las diez claves de la tabla (los puntos son parte del nombre de cada propiedad). Los siete valores numéricos SHALL ser números y los tres textos del consultorio SHALL ser texto. `ODONTOLOGO` MUST recibir 403 y un cliente anónimo MUST recibir 401, ambos con Problem Details.

#### Scenario: Admin y Recepción consultan los parámetros vigentes
- **GIVEN** un usuario autenticado con rol `ADMIN` o `RECEPCION`
- **WHEN** consulta `GET /api/configuracion`
- **THEN** recibe 200 con las diez claves vigentes y sus tipos JSON correspondientes

#### Scenario: Odontólogo no puede consultar configuración
- **WHEN** un usuario con rol `ODONTOLOGO` consulta `GET /api/configuracion`
- **THEN** recibe 403 Problem Details

#### Scenario: Un cliente anónimo no puede consultar configuración
- **WHEN** un cliente sin autenticación consulta `GET /api/configuracion`
- **THEN** recibe 401 Problem Details

### Requirement: Reemplazo completo y atómico por Admin

`PUT /api/configuracion` SHALL exigir un objeto JSON con las diez claves y ninguna adicional, en el mismo formato que `GET`. Solo `ADMIN` con CSRF válido SHALL poder ejecutarlo. Una escritura válida MUST guardar la configuración completa en una transacción, devolver 200 con el objeto resultante y actualizar `actualizado_en` para los valores que cambien. Si se repite un contenido idéntico, los valores y sus fechas MUST permanecer intactos.

#### Scenario: Admin reemplaza toda la configuración con límites válidos
- **GIVEN** un `ADMIN` con CSRF válido y las diez claves, incluidos valores en los extremos permitidos de cancelación, recordatorio y retención
- **WHEN** envía `PUT /api/configuracion`
- **THEN** recibe 200 con la configuración aplicada, un GET posterior devuelve esos valores y se actualiza la fecha de las claves modificadas

#### Scenario: Repetir el mismo reemplazo no cambia las fechas
- **GIVEN** una configuración vigente
- **WHEN** un `ADMIN` con CSRF válido envía las diez claves con los mismos valores
- **THEN** recibe 200 sin modificar valores ni fechas

#### Scenario: Dos reemplazos concurrentes no mezclan valores
- **GIVEN** dos reemplazos válidos y distintos de la configuración
- **WHEN** se ejecutan en paralelo y ambos finalizan con éxito
- **THEN** la configuración final coincide con un reemplazo completo, no con una mezcla de ambos

#### Scenario: Recepción y Odontólogo no pueden editar configuración
- **WHEN** un usuario `RECEPCION` u `ODONTOLOGO` envía un PUT válido con CSRF válido
- **THEN** recibe 403 Problem Details y la configuración no se modifica

#### Scenario: Admin no puede editar configuración sin CSRF válido
- **WHEN** un `ADMIN` envía un PUT sin token CSRF válido
- **THEN** recibe 403 Problem Details y la configuración no se modifica

### Requirement: Validación sin escrituras parciales

Un JSON mal formado, una clave faltante, extra o repetida, un `null` o un tipo JSON incorrecto SHALL devolver 400 Problem Details. Si el objeto tiene la forma esperada pero se incumple una regla de la tabla —incluidos números no enteros, números fuera del rango de un entero con signo de 32 bits o un texto del consultorio en blanco o con `U+0000`—, el sistema MUST devolver 422 Problem Details con `codigo: "RN-36"`. Ningún PUT rechazado SHALL cambiar valores o fechas.

#### Scenario: Un cuerpo que no coincide con el contrato devuelve 400
- **WHEN** un `ADMIN` con CSRF válido envía un cuerpo mal formado, incompleto, con claves extra o repetidas, con `null` o con un tipo incorrecto, como `"24"` en una clave numérica
- **THEN** recibe 400 Problem Details y no cambia ningún valor ni fecha

#### Scenario: Un valor inválido devuelve RN-36 sin guardar los demás
- **GIVEN** un `ADMIN` con CSRF válido y un cuerpo completo que combina al menos un cambio válido con un valor inválido
- **WHEN** envía el PUT, por ejemplo con `cancelacion.plazo_horas` en 169, `consultorio.retencion_anios` en 0 o 21, un valor menor al mínimo aplicable, un número no entero o un texto del consultorio en blanco o con `U+0000` escapado en JSON
- **THEN** recibe 422 Problem Details con `codigo: "RN-36"` y todos los valores y fechas anteriores se conservan

### Requirement: Configuración vigente sin efecto retroactivo

El sistema SHALL ofrecer a los módulos consumidores lecturas tipadas de los diez valores vigentes. Tras un reemplazo exitoso, las lecturas posteriores MUST usar los valores nuevos; el cambio MUST NOT recalcular ni modificar operaciones ya realizadas.

#### Scenario: El consumidor lee valores nuevos sin alterar su resultado anterior
- **GIVEN** un consumidor que ya tomó una decisión con los valores anteriores
- **WHEN** se cambia la configuración y el consumidor vuelve a consultarla para una operación posterior
- **THEN** obtiene los nuevos valores con sus tipos, y la decisión anterior no cambia
