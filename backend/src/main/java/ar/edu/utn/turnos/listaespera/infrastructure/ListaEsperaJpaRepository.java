package ar.edu.utn.turnos.listaespera.infrastructure;

import ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ListaEsperaJpaRepository extends JpaRepository<ListaEsperaJpa, Long> {

    boolean existsByPacienteIdAndPrestacionIdAndEstado(Long pacienteId, Long prestacionId, EstadoListaEspera estado);

    /** Filtros opcionales y combinables; orden por antigüedad (SU-14 usa el mismo criterio). */
    @Query("""
            select e from ListaEsperaJpa e
            where (:estado is null or e.estado = :estado)
              and (:pacienteId is null or e.pacienteId = :pacienteId)
              and (:prestacionId is null or e.prestacionId = :prestacionId)
            order by e.creadoEn asc, e.id asc
            """)
    List<ListaEsperaJpa> filtrar(@Param("estado") EstadoListaEspera estado, @Param("pacienteId") Long pacienteId,
            @Param("prestacionId") Long prestacionId);

    @Modifying
    @Query("""
            update ListaEsperaJpa e set e.estado = ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera.VENCIDA
            where e.estado = ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera.ACTIVA and e.fechaHasta < :hoy
            """)
    int vencerAnterioresA(@Param("hoy") LocalDate hoy);
}
