# reserva-publica-frontend Specification

## Purpose

Permitir que un paciente sin cuenta reserve un turno desde el celular, acepte el tratamiento de sus datos y reciba el enlace para gestionarlo.

## Requirements

### Requirement: Selección pública de prestación profesional y horario

La SPA SHALL permitir abrir `/reservar` sin sesión y elegir una prestación, un profesional que la realice y un horario devuelto por disponibilidad pública. Los horarios SHALL mostrarse en la zona de Buenos Aires.

#### Scenario: Una persona sin sesión puede elegir un horario desde el celular
- **WHEN** una persona abre `/reservar` sin sesión y elige prestación, profesional y horario
- **THEN** la pantalla permite avanzar a sus datos y muestra el horario en Buenos Aires

#### Scenario: Cambiar prestación o profesional limpia las selecciones dependientes
- **WHEN** la persona vuelve atrás y cambia la prestación o el profesional
- **THEN** la SPA limpia las selecciones que dependían de ese valor y muestra únicamente los resultados de la consulta actual

#### Scenario: Un rango sin horarios permite elegir otras fechas
- **WHEN** la disponibilidad pública devuelve una lista vacía
- **THEN** la pantalla informa que no hay horarios en el rango consultado y permite elegir otro rango

### Requirement: Datos personales y consentimiento de la reserva

La SPA SHALL pedir nombre, apellido, email, teléfono y obra social y mostrar el aviso de PA-08 con los datos del consultorio. El formulario MUST NOT pedir DNI. La casilla SHALL comenzar desmarcada. La reserva solo SHALL enviarse con la aceptación del texto y su versión.

#### Scenario: El formulario exige aceptar el aviso antes de reservar
- **WHEN** los campos están completos pero la casilla de consentimiento está desmarcada
- **THEN** la SPA indica que falta aceptar y no envía la reserva

#### Scenario: Una versión de consentimiento vencida exige volver a aceptar
- **WHEN** el servidor rechaza la versión de consentimiento
- **THEN** la SPA conserva los campos, actualiza los datos del aviso y vuelve a pedir la aceptación antes de reservar

#### Scenario: La pantalla bloquea el envío si falta el texto vigente
- **WHEN** la versión que informa el consultorio no corresponde a un texto disponible en la SPA
- **THEN** la pantalla informa que el consentimiento no está disponible y no envía la reserva

#### Scenario: Los errores de validación se muestran junto al campo correspondiente
- **WHEN** la API responde con errores de campos
- **THEN** la SPA identifica los campos, mantiene los valores ingresados y permite corregirlos

### Requirement: Recuperación ante un horario ocupado

Ante un 409 por horario ocupado, la SPA SHALL conservar el formulario y ofrecer las alternativas del servidor o una nueva búsqueda.

#### Scenario: Un horario ocupado muestra alternativas y conserva el formulario
- **WHEN** el servidor responde 409 con alternativas y la persona selecciona otra
- **THEN** puede confirmar el nuevo horario con los datos que ya ingresó

#### Scenario: Un conflicto sin alternativas permite volver a buscar
- **WHEN** el servidor responde 409 sin alternativas disponibles
- **THEN** la pantalla permite buscar en otra fecha y conserva los datos del formulario

### Requirement: Confirmación y enlace del turno

La SPA SHALL enviar como máximo un pedido por confirmación en curso. Si recibe un 201, SHALL mostrar la prestación, el profesional, el horario y el enlace relativo `/turno/{token}`. Los datos del formulario y el token MUST permanecer fuera del almacenamiento persistente de la SPA.

#### Scenario: Una reserva confirmada muestra el resumen y el enlace
- **WHEN** la API confirma la reserva
- **THEN** la pantalla muestra el horario en Buenos Aires y el enlace del turno

#### Scenario: Un segundo envío queda bloqueado mientras se confirma la reserva
- **WHEN** la persona intenta confirmar otra vez mientras el primer pedido está en curso
- **THEN** la SPA hace un solo POST

#### Scenario: El límite de solicitudes conserva el formulario y permite reintentar
- **WHEN** la API devuelve 429 al intentar confirmar
- **THEN** la SPA muestra un aviso, conserva el formulario y permite un nuevo intento manual
