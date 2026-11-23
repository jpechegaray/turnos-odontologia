# Spec Delta

## Purpose

Define cómo el personal consulta y reemplaza los parámetros operativos del consultorio desde la SPA, con validación local de los rangos de RN-36 y edición limitada al rol `ADMIN` (US-006).

## ADDED Requirements

### Requirement: Carga de la configuración vigente

La pantalla `/configuracion` SHALL consultar `GET /api/configuracion` y precargar exactamente las siete claves vigentes con sus valores y tipos. MUST mostrar un error recuperable con reintento si la consulta falla. MUST NOT enviar escrituras al montar.

#### Scenario: La configuración se precarga con las siete claves vigentes

- **GIVEN** una sesión activa con rol `ADMIN` o `RECEPCION`
- **WHEN** la persona abre `/configuracion`
- **THEN** la SPA consulta `GET /api/configuracion` y muestra los siete campos con sus valores vigentes: `cancelacion.plazo_horas`, `recordatorio.anticipacion_horas`, `reserva_online.horizonte_dias`, `reserva_online.anticipacion_minima_horas`, `reserva_online.max_turnos_activos_por_email`, `lista_espera.max_avisos_por_hueco` y `consultorio.nombre`

#### Scenario: Un fallo al consultar la configuración permite reintentar

- **GIVEN** una API que falla al devolver la configuración
- **WHEN** la persona abre `/configuracion`
- **THEN** la pantalla muestra una alerta de error y un botón para reintentar, y al usarlo se vuelve a consultar la configuración

#### Scenario: La pantalla no envía escrituras al cargar

- **WHEN** la persona abre `/configuracion` con una API que responde la configuración vigente
- **THEN** la SPA solo emite la consulta `GET` y ninguna escritura

### Requirement: Validación RN-36 por campo

El formulario SHALL validar antes de enviar que los seis valores numéricos sean enteros dentro de su rango y que `consultorio.nombre` no esté en blanco. Un valor inválido MUST impedir el envío, mostrar el mensaje de RN-36 en el campo y MUST NOT llamar a la API.

#### Scenario: Un plazo de cancelación fuera de rango bloquea el guardado sin llamar a la API

- **WHEN** el administrador ingresa `169` en `cancelacion.plazo_horas` y envía el formulario
- **THEN** el formulario muestra el mensaje de RN-36 en ese campo y no se emite ninguna escritura a la API

#### Scenario: Una anticipación de recordatorio fuera de rango bloquea el guardado sin llamar a la API

- **WHEN** el administrador ingresa `0` en `recordatorio.anticipacion_horas` y envía el formulario
- **THEN** el formulario muestra el mensaje de RN-36 en ese campo y no se emite ninguna escritura a la API

#### Scenario: Un límite positivo en cero bloquea el guardado sin llamar a la API

- **WHEN** el administrador ingresa `0` en `reserva_online.horizonte_dias` y envía el formulario
- **THEN** el formulario muestra el mensaje de RN-36 en ese campo y no se emite ninguna escritura a la API

#### Scenario: Un valor no entero bloquea el guardado sin llamar a la API

- **WHEN** el administrador ingresa `2,5` en `reserva_online.anticipacion_minima_horas` y envía el formulario
- **THEN** el formulario muestra el mensaje de RN-36 en ese campo y no se emite ninguna escritura a la API

#### Scenario: Un nombre de consultorio en blanco bloquea el guardado sin llamar a la API

- **WHEN** el administrador deja `consultorio.nombre` en blanco y envía el formulario
- **THEN** el formulario muestra el mensaje de RN-36 en ese campo y no se emite ninguna escritura a la API

#### Scenario: Los valores límite permitidos se aceptan

- **WHEN** el administrador guarda `cancelacion.plazo_horas` en `0`, `recordatorio.anticipacion_horas` en `168` y el resto dentro de rango
- **THEN** la SPA envía el `PUT` completo con esos valores límite y muestra la confirmación del backend

### Requirement: Guardado completo de la configuración

El rol `ADMIN` SHALL enviar `PUT /api/configuracion` con exactamente las siete claves. Con un `200` MUST mostrar confirmación y conservar los valores devueltos. Un `422` con código `RN-36` SHALL mostrarse como alerta sin perder lo ingresado. Un `400` SHALL mostrarse como error de operación.

#### Scenario: El administrador guarda las siete claves y ve la confirmación

- **WHEN** el administrador cambia `cancelacion.plazo_horas` y envía el formulario con las siete claves válidas
- **THEN** la SPA emite el `PUT` con exactamente las siete claves y muestra la confirmación del backend

#### Scenario: Un 422 RN-36 del backend se muestra como alerta sin perder los valores ingresados

- **WHEN** la API responde `422` con código `RN-36`
- **THEN** la pantalla muestra una alerta de valor inválido y conserva los valores ingresados en el formulario

#### Scenario: Un 400 del backend se muestra como error de operación

- **WHEN** la API responde `400` a un guardado con forma válida para la SPA
- **THEN** la pantalla muestra una alerta de operación no completada, distinta del mensaje de valor inválido

### Requirement: Permisos de consulta y edición

El rol `ADMIN` SHALL editar y guardar la configuración. `RECEPCION` SHALL consultar los siete valores en solo lectura, sin botón de guardado ni campos editables. `ODONTOLOGO` MUST NOT acceder a `/configuracion` y SHALL ver el aviso de permiso al intentarlo directamente. La API conserva la autorización efectiva.

#### Scenario: El administrador puede editar y guardar

- **GIVEN** una sesión activa con rol `ADMIN`
- **WHEN** abre `/configuracion`
- **THEN** los campos están habilitados y el botón de guardado está visible

#### Scenario: Recepción consulta la configuración en solo lectura y no envía PUT

- **GIVEN** una sesión activa con rol `RECEPCION`
- **WHEN** abre `/configuracion`
- **THEN** los siete campos están en solo lectura, no hay botón de guardado y no se emite ninguna escritura a la API

#### Scenario: El odontólogo no puede abrir la configuración

- **GIVEN** una sesión activa con rol `ODONTOLOGO`
- **WHEN** intenta abrir `/configuracion` directamente
- **THEN** la SPA muestra el aviso de permiso y no consulta la configuración

#### Scenario: La pantalla de configuración aparece en el menú solo para ADMIN y RECEPCION

- **GIVEN** una sesión activa con rol `ADMIN`, `RECEPCION` u `ODONTOLOGO`
- **WHEN** se muestra el menú principal
- **THEN** la entrada de configuración aparece para `ADMIN` y `RECEPCION`, y no aparece para `ODONTOLOGO`
