package ar.edu.utn.turnos.profesionales.prestacion.infrastructure;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProfesionalPrestacionJpaRepository extends JpaRepository<ProfesionalPrestacion, ProfesionalPrestacionId> {

    List<ProfesionalPrestacion> findByIdProfesionalId(Long profesionalId);

    void deleteByIdProfesionalIdAndIdPrestacionIdIn(Long profesionalId, Collection<Long> prestacionIds);

    @Query("select pp.id.profesionalId from ProfesionalPrestacion pp where pp.id.prestacionId = :prestacionId")
    List<Long> profesionalIdsDePrestacion(@Param("prestacionId") Long prestacionId);

    @Query("""
            select distinct pp.id.prestacionId from ProfesionalPrestacion pp
            where pp.id.profesionalId in :profesionalIds
            """)
    List<Long> prestacionIdsDeProfesionales(@Param("profesionalIds") Collection<Long> profesionalIds);
}
