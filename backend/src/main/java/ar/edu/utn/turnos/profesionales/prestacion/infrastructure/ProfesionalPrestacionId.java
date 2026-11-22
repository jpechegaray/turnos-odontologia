package ar.edu.utn.turnos.profesionales.prestacion.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
class ProfesionalPrestacionId implements Serializable {

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "prestacion_id", nullable = false)
    private Long prestacionId;

    protected ProfesionalPrestacionId() {
    }

    ProfesionalPrestacionId(Long profesionalId, Long prestacionId) {
        this.profesionalId = profesionalId;
        this.prestacionId = prestacionId;
    }

    Long getPrestacionId() {
        return prestacionId;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ProfesionalPrestacionId id)) {
            return false;
        }
        return Objects.equals(profesionalId, id.profesionalId)
                && Objects.equals(prestacionId, id.prestacionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profesionalId, prestacionId);
    }
}
