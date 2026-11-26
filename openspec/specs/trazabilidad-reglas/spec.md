# trazabilidad-reglas Specification

## Purpose
Deja evidencia verificable de que cada regla de negocio del MVP está cubierta por un escenario y un test, y de que el MVP entregado coincide con el alcance decidido en el Discovery.

## Requirements

### Requirement: Matriz de reglas de negocio a tests

El repositorio SHALL incluir una matriz con cada regla RN-01 a RN-38 de la base de conocimiento, el escenario de spec que la expresa y al menos un test que la verifica, nombrado por su escenario. Ninguna fila MUST quedar sin test.

#### Scenario: Cada regla tiene al menos un test que existe
- **GIVEN** la matriz de reglas a tests
- **WHEN** se compara con las reglas de la base de conocimiento y con los tests del repositorio
- **THEN** aparecen las 38 reglas, cada una con al menos un test, y cada test citado existe con ese nombre de escenario

### Requirement: Checklist de cierre del MVP

El repositorio SHALL incluir un checklist que contraste cada función imprescindible y diferenciadora del MVP del Discovery con la evidencia que la cubre, y que confirme que ninguna función de "Etapas posteriores" entró en el producto.

#### Scenario: El checklist cubre el MVP y excluye el backlog futuro
- **GIVEN** el checklist de cierre
- **WHEN** se lo contrasta con la sección del MVP del informe de Discovery
- **THEN** cada función imprescindible y diferenciadora tiene evidencia, y cada función de "Etapas posteriores" figura como no incorporada

### Requirement: Arranque y demostración documentados

El README SHALL explicar cómo levantar desde cero la base, el backend con el seed ficticio y el frontend, y cómo recorrer una demostración completa con los usuarios del seed, sin incluir contraseñas ni secretos.

#### Scenario: Un integrante nuevo arranca la demo siguiendo el README
- **GIVEN** un clon limpio con Docker instalado y un `.env` copiado de `.env.example` con claves propias
- **WHEN** sigue los pasos del README
- **THEN** llega a la agenda con los datos ficticios del seed y puede recorrer la reserva online, la cancelación y la lista de espera

### Requirement: Sin secretos versionados

El repositorio MUST NOT versionar secretos ni credenciales. El cierre SHALL dejar registrada la búsqueda hecha y su resultado.

#### Scenario: La búsqueda de secretos no encuentra nada versionado
- **WHEN** se buscan en los archivos versionados archivos `.env`, claves privadas, contraseñas o tokens en claro
- **THEN** no hay coincidencias reales, y las de ejemplo o de prueba quedan justificadas en el checklist de cierre
