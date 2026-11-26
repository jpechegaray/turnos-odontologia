# reglas-arquitectura Specification

## Purpose
Fija las reglas de dependencia del backend que permiten trabajar por módulos sin acoplarlos, y exige que la build las verifique automáticamente en cada cambio.

## Requirements

### Requirement: Dominio sin frameworks

El código de la capa de dominio de cada módulo MUST NOT depender del framework web ni de la capa de persistencia (Spring, JPA, Hibernate). La build SHALL fallar si aparece una dependencia así.

#### Scenario: Una dependencia del dominio hacia el framework rompe la build
- **GIVEN** una clase de dominio que usa una clase de Spring, JPA o Hibernate
- **WHEN** se ejecutan las pruebas unitarias del backend
- **THEN** la prueba de arquitectura falla e indica la clase infractora

### Requirement: Módulos sin acceso a la persistencia ajena

Un módulo MUST NOT usar las entidades persistentes ni los repositorios de otro módulo; se comunica por la interfaz de aplicación del otro módulo o por eventos. El paquete compartido no cuenta como módulo ajeno. La build SHALL fallar ante una violación.

#### Scenario: Usar el repositorio de otro módulo rompe la build
- **GIVEN** una clase del módulo de lista de espera que usa un repositorio o una entidad del módulo de agenda
- **WHEN** se ejecutan las pruebas unitarias del backend
- **THEN** la prueba de arquitectura falla e indica la clase infractora y el módulo ajeno

#### Scenario: El código actual cumple las reglas de dependencia
- **WHEN** se ejecutan las pruebas de arquitectura sobre el backend actual
- **THEN** pasan sin excepciones ocultas y sin aceptar un conjunto vacío de clases
