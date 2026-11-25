package ar.edu.utn.turnos.agenda.infrastructure;

import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.TurnoOcupado;
import ar.edu.utn.turnos.agenda.domain.ValidadorTurno;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Persistencia de turnos y traducción de la garantía de la base (RN-15) al mismo error que da la validación. */
@Repository
public class TurnosPersistencia {
    static final String SOLAPE_PROFESIONAL = "turno_sin_solape_profesional";
    static final String SOLAPE_SILLON = "turno_sin_solape_sillon";

    private final TurnoJpaRepository turnos;

    TurnosPersistencia(TurnoJpaRepository turnos) {
        this.turnos = turnos;
    }

    /**
     * Guarda y sincroniza enseguida ({@code saveAndFlush}) para que el {@code INSERT} ocurra dentro de este
     * método. Si otra transacción ocupó el hueco entre la validación y el guardado, PostgreSQL rechaza la
     * fila con una restricción {@code EXCLUDE}, y acá se traduce a {@code 409 RN-01} con el recurso.
     */
    public Turno guardar(Turno turno) {
        try {
            return turnos.saveAndFlush(TurnoJpa.de(turno)).dominio();
        } catch (DataIntegrityViolationException ex) {
            if (menciona(ex, SOLAPE_PROFESIONAL)) {
                throw ValidadorTurno.solapado("PROFESIONAL");
            }
            if (menciona(ex, SOLAPE_SILLON)) {
                throw ValidadorTurno.solapado("SILLON");
            }
            throw ex;
        }
    }

    public Optional<Turno> buscar(Long id) {
        return turnos.findById(id).map(TurnoJpa::dominio);
    }

    /** Turnos no cancelados del profesional o del sillón que solapan con el intervalo (RN-01, RN-10). */
    public List<TurnoOcupado> turnosQueSolapan(Long profesionalId, Long sillonId, Intervalo intervalo) {
        return turnos.activosQueSolapan(profesionalId, sillonId, intervalo.inicio(), intervalo.fin()).stream()
                .map(TurnoJpa::dominio)
                .map(t -> new TurnoOcupado(t.id(), t.profesionalId(), t.sillonId(), t.intervalo(), t.estado()))
                .toList();
    }

    private static boolean menciona(Throwable error, String restriccion) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa.getMessage() != null && causa.getMessage().contains(restriccion)) {
                return true;
            }
        }
        return false;
    }
}
