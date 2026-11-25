package ar.edu.utn.turnos.agenda.domain;

/**
 * Quién cancela un turno (RN-19): un usuario del personal o el paciente con su token. Se guarda en
 * {@code turno.cancelado_por}; cuando es {@link #USUARIO}, el id va en {@code cancelado_por_usuario_id}.
 */
public enum ActorCancelacion {
    USUARIO,
    PACIENTE_TOKEN
}
