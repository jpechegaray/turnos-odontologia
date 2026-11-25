# Spec Delta

## Purpose

Dejar una base de desarrollo vacía lista para demostrar la aplicación con datos ficticios.

## ADDED Requirements

### Requirement: Activación exclusiva de desarrollo

El seed SHALL ejecutarse solo con el perfil dev y una clave de personal configurada. La clave MUST seguir la política de usuarios y almacenarse como hash BCrypt. Se conserva la inicialización existente del administrador.

#### Scenario: El seed solo se habilita en desarrollo

- **WHEN** se carga el componente de arranque con y sin el perfil dev
- **THEN** solo existe en el contexto dev

#### Scenario: Sin clave de personal no se cargan datos de demostración

- **WHEN** el seed se ejecuta sin una clave configurada
- **THEN** no crea datos

#### Scenario: Una clave de personal inválida no inicia la carga

- **WHEN** hay una clave que no cumple la política de usuarios al cargar una base operativa vacía
- **THEN** se rechaza la carga sin insertar datos

### Requirement: Agenda ficticia e idempotente

En una base operativa vacía, la carga SHALL crear los profesionales vinculados, personal, sillones, prestaciones, horarios, pacientes y turnos ficticios de la base de conocimiento. Los turnos MUST respetar la disponibilidad y los bordes contiguos de la agenda. Una base con datos existentes SHALL quedar intacta.

#### Scenario: La base vacía queda lista para demostrar la agenda

- **WHEN** el seed se ejecuta en una base vacía salvo el administrador de desarrollo
- **THEN** quedan las tres cuentas de personal con su hash y vínculos correctos, dos profesionales, tres sillones, cinco prestaciones, horarios hábiles, tres pacientes ficticios y tres turnos futuros incluyendo un par contiguo

#### Scenario: Repetir la carga en otro día no duplica los datos

- **WHEN** se vuelve a ejecutar el seed con otro reloj y otra clave
- **THEN** no aparecen nuevas filas ni cambian los hashes o los horarios sembrados

#### Scenario: Una base operativa existente se conserva

- **WHEN** ya hay un registro operativo, incluso inactivo
- **THEN** se omite la carga y se conserva el registro original

#### Scenario: Una falla durante la carga revierte los datos

- **WHEN** ocurre una falla después de que empezaron las altas
- **THEN** la carga de demostración no deja datos parciales y el contexto de seguridad anterior se restaura
