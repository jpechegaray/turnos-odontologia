package ar.edu.utn.turnos.prestaciones.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Solo lo usa este módulo; los demás consultan por {@code ConsultaPrestaciones}. */
interface PrestacionJpaRepository extends JpaRepository<Prestacion, Long> {

    boolean existsByNombreClave(String nombreClave);

    boolean existsByNombreClaveAndIdNot(String nombreClave, Long id);

    List<Prestacion> findAllByOrderByNombreClaveAsc();

    List<Prestacion> findByActivaTrueOrderByNombreClaveAsc();
}
