package ar.edu.utn.turnos.agenda.infrastructure;

import ar.edu.utn.turnos.agenda.domain.ActorCancelacion;
import ar.edu.utn.turnos.agenda.domain.CanalTurno;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.Check;

/**
 * Tabla {@code turno} (KB 04 §Turno). Hibernate genera columnas, índices y CHECK; las FK y las dos
 * restricciones {@code EXCLUDE} (RN-01, RN-15) van en {@code schema-constraints.sql} (DD-08). Los otros
 * módulos se referencian solo por id (KB 08). {@code actualizado_en} lo completarán los cambios de estado
 * (C-27, C-29, C-35).
 */
@Entity
@Table(name = "turno", indexes = {
        @Index(name = "ix_turno_profesional_inicio", columnList = "profesional_id, inicio"),
        @Index(name = "ix_turno_sillon_inicio", columnList = "sillon_id, inicio"),
        @Index(name = "ix_turno_paciente_inicio", columnList = "paciente_id, inicio"),
        @Index(name = "ix_turno_estado_inicio", columnList = "estado, inicio")})
@Check(name = "ck_turno_intervalo", constraints = "fin > inicio")
@Check(name = "ck_turno_duracion", constraints = "duracion_min > 0")
class TurnoJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "sillon_id", nullable = false)
    private Long sillonId;

    @Column(name = "prestacion_id", nullable = false)
    private Long prestacionId;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Column(name = "duracion_min", nullable = false)
    private int duracionMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoTurno estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CanalTurno canal;

    @Column(name = "cancelacion_tardia", nullable = false)
    private boolean cancelacionTardia;

    @Column(name = "reprogramado_tarde", nullable = false)
    private boolean reprogramadoTarde;

    @Column(name = "cancelado_en")
    private Instant canceladoEn;

    @Column(name = "cancelado_por", length = 16)
    private String canceladoPor;

    @Column(name = "cancelado_por_usuario_id")
    private Long canceladoPorUsuarioId;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    @Column(columnDefinition = "text")
    private String notas;

    @Column(name = "creado_por_usuario_id")
    private Long creadoPorUsuarioId;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    @Version
    @Column(nullable = false)
    private Long version;

    protected TurnoJpa() {
    }

    static TurnoJpa de(Turno turno) {
        var fila = new TurnoJpa();
        fila.id = turno.id();
        fila.pacienteId = turno.pacienteId();
        fila.profesionalId = turno.profesionalId();
        fila.sillonId = turno.sillonId();
        fila.prestacionId = turno.prestacionId();
        fila.inicio = turno.intervalo().inicio();
        fila.fin = turno.intervalo().fin();
        fila.duracionMin = turno.duracionMin();
        fila.estado = turno.estado();
        fila.canal = turno.canal();
        fila.cancelacionTardia = turno.cancelacionTardia();
        fila.reprogramadoTarde = turno.reprogramadoTarde();
        fila.canceladoEn = turno.canceladoEn();
        fila.canceladoPor = turno.canceladoPor() == null ? null : turno.canceladoPor().name();
        fila.canceladoPorUsuarioId = turno.canceladoPorUsuarioId();
        fila.motivoCancelacion = turno.motivoCancelacion();
        fila.notas = turno.notas();
        fila.creadoPorUsuarioId = turno.creadoPorUsuarioId();
        fila.creadoEn = turno.creadoEn();
        fila.version = turno.version();
        return fila;
    }

    Turno dominio() {
        return new Turno(id, pacienteId, profesionalId, sillonId, prestacionId, new Intervalo(inicio, fin), duracionMin,
                estado, canal, notas, cancelacionTardia, reprogramadoTarde, canceladoEn,
                canceladoPor == null ? null : ActorCancelacion.valueOf(canceladoPor), canceladoPorUsuarioId,
                motivoCancelacion, creadoPorUsuarioId, creadoEn, version);
    }
}
