# Spec Delta

## Purpose

Protege los endpoints públicos sin sesión (`/api/public/**`) y las operaciones públicas sensibles frente a ráfagas de solicitudes y abuso mediante límites de tasa en memoria por IP y por clave arbitraria, controlados con el reloj del sistema (`Clock`) y con respuestas `429 Too Many Requests` en formato Problem Details con `Retry-After`.

## ADDED Requirements

### Requirement: Límite de tasa por IP en rutas públicas
El sistema SHALL limitar las solicitudes por IP de origen en `/api/public/**` dentro de una ventana temporal configurable. Mientras una IP no supere el umbral vigente, la solicitud SHALL procesarse normalmente; al superarlo, MUST responder `429` Problem Details (`codigo: "RN-27"`) con cabecera `Retry-After` en segundos enteros restantes. Cada IP MUST tener su cupo independiente y las rutas fuera de `/api/public/**` MUST NOT ser limitadas por este filtro.

#### Scenario: Las solicitudes públicas dentro del umbral se permiten
- **GIVEN** un umbral configurado de solicitudes por ventana para `/api/public/**`
- **WHEN** una misma IP envía solicitudes a una ruta bajo `/api/public/` sin superar el umbral dentro de la ventana vigente
- **THEN** todas las solicitudes son procesadas normalmente y ninguna recibe estado 429

#### Scenario: Superar el umbral por IP en una ruta pública devuelve 429 con Retry-After
- **GIVEN** una IP que ya alcanzó el máximo de solicitudes permitidas en la ventana actual sobre `/api/public/**`
- **WHEN** la misma IP envía una solicitud adicional a cualquier ruta bajo `/api/public/` antes de que venza la ventana
- **THEN** la respuesta tiene estado 429, tipo `application/problem+json`, propiedad `codigo` igual a `RN-27` y cabecera `Retry-After` con los segundos enteros positivos restantes hasta el reinicio de la ventana
- **AND** la operación del endpoint público no se ejecuta

#### Scenario: El bloqueo de una IP no afecta a otra IP distinta
- **GIVEN** una IP que superó el umbral de solicitudes en `/api/public/**` y recibe 429
- **WHEN** otra IP distinta envía una solicitud a `/api/public/` en el mismo instante
- **THEN** la solicitud de la segunda IP es permitida y no recibe 429

#### Scenario: Las rutas fuera de la zona pública no son limitadas por el filtro público
- **GIVEN** una IP que superó el umbral de solicitudes en `/api/public/**`
- **WHEN** la misma IP invoca una ruta fuera de `/api/public/**`
- **THEN** el filtro de tasa público no bloquea la solicitud con 429

### Requirement: Reinicio de ventana temporal y limpieza de contadores vencidos
El cálculo de las ventanas de tasa y del tiempo restante (`Retry-After`) SHALL usar exclusivamente el `Clock` inyectable del sistema, nunca el reloj estático del entorno. Cuando el instante actual alcance o supere el fin de la ventana en la que se agotó el cupo, el sistema SHALL abrir una nueva ventana y permitir nuevamente solicitudes para esa clave o IP. El sistema MUST descartar los contadores cuyas ventanas hayan vencido para evitar acumulación indefinida en memoria.

#### Scenario: Al reiniciar la ventana la misma IP vuelve a recibir respuestas normales
- **GIVEN** una IP que alcanzó el límite y recibe 429 en el instante T con una ventana de duración D
- **WHEN** el reloj del sistema avanza hasta T + D y la misma IP envía una nueva solicitud a `/api/public/`
- **THEN** la solicitud es permitida y la ventana se reinicia con un nuevo cupo

#### Scenario: Los contadores de ventanas vencidas se limpian de la memoria
- **GIVEN** múltiples claves o direcciones IP registradas en ventanas anteriores que ya vencieron según el reloj del sistema
- **WHEN** se procesan nuevas evaluaciones de tasa tras el vencimiento de esas ventanas
- **THEN** las entradas vencidas son eliminadas del almacenamiento en memoria y no permanecen retenidas indefinidamente

### Requirement: Limitador reutilizable por clave de negocio
El sistema SHALL proveer un mecanismo reutilizable en memoria para verificar y consumir cupo por una clave arbitraria (como un email normalizado en operaciones de reserva pública) con umbral y ventana configurables y evaluación atómica ante solicitudes concurrentes. La respuesta o resultado de rechazo MUST indicar los segundos de espera hasta el reinicio de la ventana y MUST NOT exponer la clave ni datos personales de pacientes en el cuerpo del error ni en los logs (RN-35).

#### Scenario: El limitador por clave bloquea al superar el tope para una misma clave normalizada
- **GIVEN** un tope por clave configurado y una clave como un email que alcanza el límite de intentos dentro de la ventana
- **WHEN** se intenta una operación adicional con la misma clave (incluso con distinta capitalización o espacios extremos) antes del fin de la ventana
- **THEN** el intento es rechazado indicando los segundos restantes de espera sin exponer el email ni datos personales en el mensaje de error

#### Scenario: Dos claves distintas tienen cupos independientes y se reinician al vencer su ventana
- **GIVEN** una clave que agotó su cupo en la ventana actual
- **WHEN** se evalúa una clave diferente en el mismo instante, o la misma clave una vez transcurrida la duración de la ventana en el reloj
- **THEN** la evaluación permite la operación
