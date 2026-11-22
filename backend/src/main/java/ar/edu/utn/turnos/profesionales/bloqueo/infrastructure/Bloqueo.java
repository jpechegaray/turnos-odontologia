package ar.edu.utn.turnos.profesionales.bloqueo.infrastructure;

import ar.edu.utn.turnos.profesionales.bloqueo.application.BloqueoDatos;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "bloqueo", indexes = @Index(name = "ix_bloqueo_profesional_inicio", columnList = "profesional_id, inicio"))
@Check(name = "ck_bloqueo_intervalo", constraints = "fin > inicio")
class Bloqueo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Column(nullable = false, length = 255)
    private String motivo;

    protected Bloqueo() {
    }

    Bloqueo(Long profesionalId, Intervalo intervalo, String motivo) {
        this.profesionalId = profesionalId;
        this.inicio = intervalo.inicio();
        this.fin = intervalo.fin();
        this.motivo = motivo;
    }

    BloqueoDatos datos() {
        return new BloqueoDatos(id, profesionalId, inicio, fin, motivo);
    }
}
