package ar.edu.utn.turnos.listaespera.oferta.infrastructure;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
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

    @Modifying
    @Query("""
            update OfertaHuecoJpa o set o.estado = ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta.TOMADA
            where o.estado = ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta.ENVIADA
              and o.profesionalId = :profesionalId and o.huecoInicio = :inicio and o.huecoFin = :fin
            """)
    int tomarDelHueco(@Param("profesionalId") Long profesionalId, @Param("inicio") Instant inicio,
            @Param("fin") Instant fin);

    @Modifying
    @Query("""
            update OfertaHuecoJpa o set o.estado = ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta.TOMADA
            where o.estado = ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta.ENVIADA
              and o.profesionalId = :profesionalId and o.huecoInicio = :inicio
            """)
    int tomarDelHuecoPorInicio(@Param("profesionalId") Long profesionalId, @Param("inicio") Instant inicio);

    @Query("""
            select o from OfertaHuecoJpa o
            where o.profesionalId = :profesionalId and o.huecoInicio = :inicio
            order by case when o.listaEsperaId = :entradaId then 0 else 1 end, o.id
            """)
    List<OfertaHuecoJpa> delHuecoPrefiriendo(@Param("entradaId") Long entradaId,
            @Param("profesionalId") Long profesionalId, @Param("inicio") Instant inicio);

    @Modifying
    @Query("""
            update OfertaHuecoJpa o set o.estado = ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta.VENCIDA
            where o.estado = ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta.ENVIADA and o.id = :id
            """)
    int vencerOferta(@Param("id") Long id);
}
