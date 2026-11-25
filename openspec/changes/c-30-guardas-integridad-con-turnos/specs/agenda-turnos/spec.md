# Spec Delta

## ADDED Requirements

### Requirement: Consulta interna de turnos activos en conflicto para bloqueos y bajas de catálogo

La agenda SHALL ofrecer una consulta interna de turnos `PROGRAMADO` o `CONFIRMADO` para RN-17 y RN-37: por intervalo `[inicio, fin)` de un profesional devuelve los que cumplan `inicioTurno < fin` y `finTurno > inicio` (RN-02), y por baja de profesional, sillón o prestación los que tengan `fin` posterior al reloj actual (RN-14), ordenados por `inicio` e `id` ascendentes con `id`, `inicio`, `fin` y `estado` sin datos del paciente (RN-35).

#### Scenario: La consulta por intervalo incluye solo turnos programados o confirmados que se solapan
- **GIVEN** un profesional con turnos `PROGRAMADO` y `CONFIRMADO` dentro del intervalo, un turno contiguo y otros turnos `CANCELADO`, `ATENDIDO` y `AUSENTE` en el mismo horario
- **WHEN** se consultan los turnos activos del profesional en ese intervalo `[inicio, fin)`
- **THEN** la consulta devuelve únicamente los turnos `PROGRAMADO` y `CONFIRMADO` solapados, ordenados por `inicio` e `id`, y excluye el contiguo y los de estado final

#### Scenario: La consulta de turnos futuros activos filtra por fin posterior al reloj actual
- **GIVEN** un recurso con un turno activo cuyo `fin` ya pasó o coincide con el instante actual y otro turno activo cuyo `fin` es posterior al instante actual
- **WHEN** se consultan los turnos futuros activos del recurso
- **THEN** la consulta devuelve únicamente el turno activo con `fin` posterior al instante actual
