package ar.edu.utn.turnos.profesionales.prestacion.infrastructure;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "profesional_prestacion",
        indexes = @Index(name = "ix_profesional_prestacion_prestacion", columnList = "prestacion_id"))
class ProfesionalPrestacion {

    @EmbeddedId
    private ProfesionalPrestacionId id;

    protected ProfesionalPrestacion() {
    }

    ProfesionalPrestacion(Long profesionalId, Long prestacionId) {
        this.id = new ProfesionalPrestacionId(profesionalId, prestacionId);
    }

    Long getPrestacionId() {
        return id.getPrestacionId();
    }
}
