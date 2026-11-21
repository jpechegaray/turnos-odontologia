package ar.edu.utn.turnos.seguridad.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class PoliticaBloqueo {

    private static final int MAX_FALLOS = 5;
    private static final Duration DURACION = Duration.ofMinutes(15);
    private final Clock reloj;

    public PoliticaBloqueo(Clock reloj) {
        this.reloj = reloj;
    }

    public boolean estaBloqueado(Estado estado) {
        return estaBloqueado(estado, reloj.instant());
    }

    public Estado registrarFallo(Estado estado) {
        Instant ahora = reloj.instant();
        if (estaBloqueado(estado, ahora)) {
            return estado;
        }
        int fallos = estado.bloqueadoHasta() == null ? estado.intentosFallidos() + 1 : 1;
        return new Estado(fallos, fallos >= MAX_FALLOS ? ahora.plus(DURACION) : null);
    }

    public Estado registrarExito() {
        return new Estado(0, null);
    }

    private boolean estaBloqueado(Estado estado, Instant ahora) {
        return estado.bloqueadoHasta() != null && ahora.isBefore(estado.bloqueadoHasta());
    }

    public record Estado(int intentosFallidos, Instant bloqueadoHasta) {
    }
}
