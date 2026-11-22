package ar.edu.utn.turnos.profesionales.horario.infrastructure;

import ar.edu.utn.turnos.profesionales.horario.application.HorarioDatos;
import ar.edu.utn.turnos.profesionales.horario.application.Tramo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalTime;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

/** Tramo semanal de atención. Las horas son de pared (sin zona): la zona se aplica al comparar con un turno. */
@Entity
@Table(name = "horario_atencion",
        indexes = @Index(name = "ix_horario_atencion_profesional_dia", columnList = "profesional_id, dia_semana"))
@Check(name = "ck_horario_atencion_tramo", constraints = "hora_fin > hora_inicio")
class HorarioAtencion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "dia_semana", nullable = false)
    @Check(name = "ck_horario_atencion_dia", constraints = "dia_semana between 1 and 7")
    private int diaSemana;

    // hibernate.jdbc.time_zone=UTC desplazaría la hora de pared; LocalTimeJdbcType la envía sin convertir.
    @Column(name = "hora_inicio", nullable = false)
    @JdbcType(LocalTimeJdbcType.class)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    @JdbcType(LocalTimeJdbcType.class)
    private LocalTime horaFin;

    protected HorarioAtencion() {
    }

    HorarioAtencion(Long profesionalId, Tramo tramo) {
        this.profesionalId = profesionalId;
        reemplazar(tramo);
    }

    void reemplazar(Tramo tramo) {
        this.diaSemana = tramo.diaSemana();
        this.horaInicio = tramo.horaInicio();
        this.horaFin = tramo.horaFin();
    }

    HorarioDatos datos() {
        return new HorarioDatos(id, profesionalId, diaSemana, horaInicio, horaFin);
    }
}
