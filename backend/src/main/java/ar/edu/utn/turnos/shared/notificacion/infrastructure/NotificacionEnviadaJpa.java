package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import ar.edu.utn.turnos.shared.notificacion.domain.EstadoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Entidad JPA de {@link NotificacionEnviada} (KB 04). Las dos restricciones únicas y el índice del
 * planificador se declaran acá para que {@code create-drop} de tests y una base nueva de dev los
 * obtengan solas; en dev con {@code ddl-auto=update} los agrega {@code schema-constraints.sql}.
 *
 * <p>No hay FKs a {@code turno} ni a {@code oferta_hueco}: esas tablas todavía no existen (las crean
 * C-19 y C-36). Las columnas quedan como {@code bigint} nullable y la FK se agregará en el change que
 * cree cada tabla.
 */
@Entity
@Table(
        name = "notificacion_enviada",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notificacion_turno_tipo_inicio",
                        columnNames = {"turno_id", "tipo", "inicio_referencia"}),
                @UniqueConstraint(
                        name = "uk_notificacion_oferta_tipo",
                        columnNames = {"oferta_hueco_id", "tipo"})
        },
        indexes = @Index(name = "ix_notificacion_estado_programada", columnList = "estado, programada_para"))
public class NotificacionEnviadaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 20)
    private String canal;

    @Column(nullable = false)
    private String destinatario;

    @Column(name = "turno_id")
    private Long turnoId;

    @Column(name = "oferta_hueco_id")
    private Long ofertaHuecoId;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "inicio_referencia")
    private Instant inicioReferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoNotificacion estado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ultimo_error", columnDefinition = "text")
    private String ultimoError;

    @Column(name = "programada_para")
    private Instant programadaPara;

    @Column(name = "enviada_en")
    private Instant enviadaEn;

    protected NotificacionEnviadaJpa() {
        // Constructor por defecto de JPA.
    }

    private NotificacionEnviadaJpa(NotificacionEnviada dominio) {
        this.id = dominio.id();
        this.tipo = dominio.tipo();
        this.canal = dominio.canal();
        this.destinatario = dominio.destinatario();
        this.turnoId = dominio.turnoId();
        this.ofertaHuecoId = dominio.ofertaHuecoId();
        this.pacienteId = dominio.pacienteId();
        this.inicioReferencia = dominio.inicioReferencia();
        this.estado = dominio.estado();
        this.intentos = dominio.intentos();
        this.ultimoError = dominio.ultimoError();
        this.programadaPara = dominio.programadaPara();
        this.enviadaEn = dominio.enviadaEn();
    }

    public static NotificacionEnviadaJpa desdeDominio(NotificacionEnviada dominio) {
        return new NotificacionEnviadaJpa(dominio);
    }

    public NotificacionEnviada aDominio() {
        return new NotificacionEnviada(
                this.id,
                this.tipo,
                this.canal,
                this.destinatario,
                this.turnoId,
                this.ofertaHuecoId,
                this.pacienteId,
                this.inicioReferencia,
                this.estado,
                this.intentos,
                this.ultimoError,
                this.programadaPara,
                this.enviadaEn);
    }
}
