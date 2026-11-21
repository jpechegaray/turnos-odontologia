package ar.edu.utn.turnos.profesionales.profesional.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProfesionalJpaRepository extends JpaRepository<Profesional, Long> {

    List<Profesional> findByActivoTrueOrderById();
}
