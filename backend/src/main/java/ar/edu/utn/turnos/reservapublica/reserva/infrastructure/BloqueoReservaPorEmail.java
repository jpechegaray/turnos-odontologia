package ar.edu.utn.turnos.reservapublica.reserva.infrastructure;

import jakarta.persistence.EntityManager;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serializa las reservas del mismo email (RN-27 y RN-28): sin este lock, dos reservas simultáneas podrían
 * superar el tope o crear dos fichas. Es un advisory lock de transacción, en su propio espacio de claves, que la
 * reserva toma antes que los de la agenda (19_001 y 19_002), siempre en ese orden, así que no hay ciclos.
 */
@Component
public class BloqueoReservaPorEmail {

    static final int LOCK_EMAIL = 41_001;

    private final EntityManager em;

    BloqueoReservaPorEmail(EntityManager em) {
        this.em = em;
    }

    /** Una colisión de hash solo serializa reservas de más; nunca deja pasar dos del mismo email. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void bloquear(String email) {
        int clave = email.strip().toLowerCase(Locale.ROOT).hashCode();
        em.createNativeQuery("select cast(pg_advisory_xact_lock(?1, ?2) as text)")
                .setParameter(1, LOCK_EMAIL)
                .setParameter(2, clave)
                .getSingleResult();
    }
}
