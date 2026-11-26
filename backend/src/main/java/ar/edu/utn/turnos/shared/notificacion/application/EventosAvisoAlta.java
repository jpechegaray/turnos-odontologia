package ar.edu.utn.turnos.shared.notificacion.application;

public final class EventosAvisoAlta {
    private EventosAvisoAlta() {}

    public record EntradaRegistrada(Long entradaId) {}

    public record TurnoAgendado(Long turnoId, boolean desdeListaDeEspera) {}
}
