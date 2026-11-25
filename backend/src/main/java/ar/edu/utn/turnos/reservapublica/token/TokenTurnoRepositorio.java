package ar.edu.utn.turnos.reservapublica.token;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TokenTurnoRepositorio extends JpaRepository<TokenTurno, Long> {
    @Query("select t.turnoId from TokenTurno t where t.tokenHash = :hash")
    Optional<Long> buscarTurno(@Param("hash") String hash);

    @Query("""
            select t.tokenHash as tokenHash, t.expiraEn as expiraEn, t.revocadoEn as revocadoEn
            from TokenTurno t where t.tokenHash = :hash
            """)
    Optional<Vigencia> buscarVigencia(@Param("hash") String hash);

    @Modifying
    @Query("update TokenTurno t set t.ultimoUsoEn = :ahora where t.tokenHash = :hash")
    void registrarUso(@Param("hash") String hash, @Param("ahora") Instant ahora);

    @Modifying
    @Query("update TokenTurno t set t.revocadoEn = :instante where t.turnoId = :turnoId and t.revocadoEn is null")
    void revocar(@Param("turnoId") Long turnoId, @Param("instante") Instant instante);

    @Modifying
    @Query("update TokenTurno t set t.expiraEn = :inicio where t.turnoId = :turnoId and t.revocadoEn is null")
    void recalcularExpiracion(@Param("turnoId") Long turnoId, @Param("inicio") Instant inicio);

    interface Vigencia {
        String getTokenHash();

        Instant getExpiraEn();

        Instant getRevocadoEn();
    }
}
