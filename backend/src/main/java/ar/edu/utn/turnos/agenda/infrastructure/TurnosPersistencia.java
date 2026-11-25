package ar.edu.utn.turnos.agenda.infrastructure;

import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.TurnoOcupado;
import ar.edu.utn.turnos.agenda.domain.ValidadorTurno;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

/** Persistencia de turnos y traducción de la garantía de la base (RN-15) al mismo error que da la validación. */
@Repository
public class TurnosPersistencia {
    static final String SOLAPE_PROFESIONAL = "turno_sin_solape_profesional";
    static final String SOLAPE_SILLON = "turno_sin_solape_sillon";
    /** Espacios de claves de los locks de agenda, para no chocar con otros usos de advisory locks. */
    static final int LOCK_PROFESIONAL = 19_001;
    static final int LOCK_SILLON = 19_002;

    private final TurnoJpaRepository turnos;
    private final EntityManager em;

    TurnosPersistencia(TurnoJpaRepository turnos, EntityManager em) {
        this.turnos = turnos;
        this.em = em;
    }

    /**
     * Serializa, hasta el fin de la transacción, las altas que tocan este profesional o este sillón (RN-15).
     * Sin esto, dos {@code INSERT} simultáneos se bloquean en cruz entre las dos restricciones {@code EXCLUDE}
     * y PostgreSQL aborta uno por deadlock (500). Se toma siempre primero el profesional y después el sillón:
     * con un orden fijo no hay ciclo posible. Debe llamarse dentro de una transacción y antes de validar,
     * para que el segundo pedido valide contra el turno que el primero ya confirmó.
     */
    public void bloquearRecursos(Long profesionalId, Long sillonId) {
        bloquear(LOCK_PROFESIONAL, profesionalId);
        bloquear(LOCK_SILLON, sillonId);
    }

    private void bloquear(int espacio, Long id) {
        // La clave de 32 bits se pliega desde el id; una colisión solo serializa de más, nunca permite un solape.
        int clave = (int) (id ^ (id >>> 32));
        em.createNativeQuery("select cast(pg_advisory_xact_lock(?1, ?2) as text)")
                .setParameter(1, espacio)
                .setParameter(2, clave)
                .getSingleResult();
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
        } catch (ObjectOptimisticLockingFailureException ex) {
            // Otra transacción modificó el turno desde que se leyó (@Version): conflicto, no error interno.
            throw new ReglaNegocioException("TURNO_MODIFICADO", EstadoError.CONFLICTO_409,
                    "El turno cambió mientras se modificaba; volvé a consultarlo");
        }
    }

    /**
     * Lee el turno bloqueando su fila hasta el fin de la transacción (exige una transacción activa). Una
     * segunda modificación simultánea espera y lee el estado ya actualizado (C-27; C-35 también lo usa).
     */
    public Optional<Turno> buscarParaActualizar(Long id) {
        return turnos.paraActualizar(id).map(TurnoJpa::dominio);
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
