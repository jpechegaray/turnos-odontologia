package ar.edu.utn.turnos.reservapublica.reserva.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Consentimiento aceptado en una reserva online que reutilizó una ficha existente (PA-08 §5): queda ligado al
 * turno y no modifica la ficha (RN-28). Uno por turno. Referencia al turno solo por id, como el resto de los
 * módulos (KB 08).
 */
@Entity
@Table(name = "consentimiento_turno",
        uniqueConstraints = @UniqueConstraint(name = "uq_consentimiento_turno_turno", columnNames = "turno_id"))
public class ConsentimientoTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turno_id", nullable = false, updatable = false)
    private Long turnoId;

    @Column(name = "aceptado_en", nullable = false, updatable = false)
    private Instant aceptadoEn;

    @Column(name = "version", nullable = false, updatable = false, length = 20)
    private String version;

    protected ConsentimientoTurno() {
    }

    public ConsentimientoTurno(Long turnoId, Instant aceptadoEn, String version) {
        this.turnoId = turnoId;
        this.aceptadoEn = aceptadoEn;
        this.version = version;
    }

    public Long getTurnoId() {
        return turnoId;
    }

    public Instant getAceptadoEn() {
        return aceptadoEn;
    }

    public String getVersion() {
        return version;
    }
}
