package ar.edu.utn.turnos.listaespera.oferta.infrastructure;

import ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta;
import ar.edu.utn.turnos.listaespera.oferta.domain.OfertaHueco;
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
import org.hibernate.annotations.Check;

/**
 * Fila de {@code oferta_hueco} (KB 04 §OfertaHueco). Las FK a {@code lista_espera} y {@code profesional} van en
 * {@code schema-constraints.sql}, como en el resto de los módulos.
 */
@Entity
@Table(name = "oferta_hueco",
        uniqueConstraints = {
                @UniqueConstraint(name = OfertaHuecoJpa.UK_ENTRADA_HUECO,
                        columnNames = {"lista_espera_id", "profesional_id", "hueco_inicio"}),
                @UniqueConstraint(name = "uk_oferta_hueco_token", columnNames = "token_hash")},
        indexes = @Index(name = "ix_oferta_hueco_estado_expira", columnList = "estado, expira_en"))
@Check(name = "ck_oferta_hueco_intervalo", constraints = "hueco_fin > hueco_inicio")
class OfertaHuecoJpa {
    static final String UK_ENTRADA_HUECO = "uk_oferta_hueco_entrada_hueco";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lista_espera_id", nullable = false)
    private Long listaEsperaId;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "hueco_inicio", nullable = false)
    private Instant huecoInicio;

    @Column(name = "hueco_fin", nullable = false)
    private Instant huecoFin;

    @Column(name = "sillon_sugerido_id")
    private Long sillonSugeridoId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoOferta estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected OfertaHuecoJpa() {
    }

    static OfertaHuecoJpa de(OfertaHueco oferta) {
        var fila = new OfertaHuecoJpa();
        fila.id = oferta.id();
        fila.listaEsperaId = oferta.listaEsperaId();
        fila.profesionalId = oferta.profesionalId();
        fila.huecoInicio = oferta.huecoInicio();
        fila.huecoFin = oferta.huecoFin();
        fila.sillonSugeridoId = oferta.sillonSugeridoId();
        fila.tokenHash = oferta.tokenHash();
        fila.expiraEn = oferta.expiraEn();
        fila.estado = oferta.estado();
        fila.creadoEn = oferta.creadoEn();
        return fila;
    }

    OfertaHueco dominio() {
        return new OfertaHueco(id, listaEsperaId, profesionalId, huecoInicio, huecoFin, sillonSugeridoId, tokenHash,
                expiraEn, estado, creadoEn);
    }
}
