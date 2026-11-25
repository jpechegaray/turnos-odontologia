package ar.edu.utn.turnos.listaespera.oferta.infrastructure;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OfertaHuecoJpaRepository extends JpaRepository<OfertaHuecoJpa, Long> {

    Optional<OfertaHuecoJpa> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OfertaHuecoJpa o where o.id = :id")
    Optional<OfertaHuecoJpa> bloquear(@Param("id") Long id);

    @Modifying
    @Query("""
            update OfertaHuecoJpa o set o.estado = ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta.VENCIDA
            where o.estado = ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta.ENVIADA and o.expiraEn <= :ahora
            """)
    int vencer(@Param("ahora") Instant ahora);
}
