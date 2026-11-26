## ADDED Requirements

### Requirement: Vista previa de un correo del registro

La API SHALL ofrecer a administración y recepción una vista previa de un aviso por identificador. La vista MUST usar su tipo y sus datos asociados. El correo de vista previa MUST omitir cualquier token vigente de turno u oferta.

#### Scenario: El detalle presenta la vista previa del correo
- **WHEN** el personal abre un aviso desde Notificaciones
- **THEN** se muestra su asunto y texto de vista previa bajo la información del envío
