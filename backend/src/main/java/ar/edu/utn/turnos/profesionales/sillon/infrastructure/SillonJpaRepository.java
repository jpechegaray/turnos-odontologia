package ar.edu.utn.turnos.profesionales.sillon.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SillonJpaRepository extends JpaRepository<Sillon, Long> {

    List<Sillon> findByActivoTrueOrderById();
}
