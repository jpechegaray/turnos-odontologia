package ar.edu.utn.turnos.reservapublica.reserva.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentimientoTurnoRepositorio extends JpaRepository<ConsentimientoTurno, Long> {

    Optional<ConsentimientoTurno> findByTurnoId(Long turnoId);
}
