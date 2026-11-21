package ar.edu.utn.turnos.seguridad.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface UsuarioJpaRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findByEmail(String email);
}
