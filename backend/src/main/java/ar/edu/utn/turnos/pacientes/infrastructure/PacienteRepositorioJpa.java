package ar.edu.utn.turnos.pacientes.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PacienteJpa}.
 */
public interface PacienteRepositorioJpa extends JpaRepository<PacienteJpa, Long> {

    Optional<PacienteJpa> findByEmailIgnoreCase(String email);

    boolean existsByDni(String dni);

    @Query("""
        SELECT p FROM PacienteJpa p
        WHERE p.activo = true
          AND (CAST(:apellido AS String) IS NULL OR LOWER(p.apellido) LIKE CAST(:apellido AS String))
          AND (CAST(:nombre AS String) IS NULL OR LOWER(p.nombre) LIKE CAST(:nombre AS String))
          AND (CAST(:dni AS String) IS NULL OR p.dni = CAST(:dni AS String))
          AND (CAST(:email AS String) IS NULL OR LOWER(p.email) = CAST(:email AS String))
        ORDER BY p.apellido, p.nombre
    """)
    List<PacienteJpa> buscar(@Param("apellido") String apellido,
                             @Param("nombre") String nombre,
                             @Param("dni") String dni,
                             @Param("email") String email);
}
