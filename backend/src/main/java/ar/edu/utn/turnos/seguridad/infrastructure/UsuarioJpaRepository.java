package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.seguridad.domain.Rol;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UsuarioJpaRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    /** Bloqueo pesimista: lo usa el login para contar intentos fallidos sin perder incrementos (C-03). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByProfesionalId(Long profesionalId);

    boolean existsByProfesionalIdAndIdNot(Long profesionalId, Long id);

    List<Usuario> findAllByOrderByEmailAsc();

    /** Serializa las operaciones que pueden dejar al consultorio sin administrador activo (C-13). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.rol = :rol and u.activo = true")
    List<Usuario> bloquearActivosConRol(@Param("rol") Rol rol);
}
