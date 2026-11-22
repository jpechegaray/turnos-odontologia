package ar.edu.utn.turnos.profesionales.prestacion.infrastructure;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProfesionalPrestacionJpaRepository extends JpaRepository<ProfesionalPrestacion, ProfesionalPrestacionId> {

    List<ProfesionalPrestacion> findByIdProfesionalId(Long profesionalId);

    void deleteByIdProfesionalIdAndIdPrestacionIdIn(Long profesionalId, Collection<Long> prestacionIds);
}
