package ar.edu.utn.turnos.reservapublica.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** Credencial de turno. La FK entre módulos se declara en schema-constraints.sql. */
@Entity
@Table(name = "token_turno",
        indexes = @Index(name = "ix_token_turno_turno", columnList = "turno_id"),
        uniqueConstraints = @UniqueConstraint(name = "uq_token_turno_hash", columnNames = "token_hash"))
class TokenTurno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turno_id", nullable = false, updatable = false)
    private Long turnoId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    @Column(name = "ultimo_uso_en")
    private Instant ultimoUsoEn;

    protected TokenTurno() {
    }

    TokenTurno(Long turnoId, String tokenHash, Instant expiraEn, Instant creadoEn) {
        this.turnoId = turnoId;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
        this.creadoEn = creadoEn;
    }
}
