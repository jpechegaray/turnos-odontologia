package ar.edu.utn.turnos.pacientes.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PacienteRepositorio {

    Optional<Paciente> porId(Long id);

    /** Fichas cuyos ids están en {@code ids}; los inexistentes se ignoran. */
    List<Paciente> porIds(Collection<Long> ids);

    Optional<Paciente> porEmail(String email);

    /**
     * Fichas ACTIVAS con ese email, sin distinguir mayúsculas ni espacios en los extremos, de la más antigua
     * a la más nueva (RN-28). Un email nulo o en blanco no encuentra ninguna.
     */
    List<Paciente> activosPorEmail(String email);

    /** Indica si otra ficha ACTIVA ya tiene ese DNI; las fichas inactivas no lo reservan. */
    boolean existePorDni(String dni);

    Paciente guardar(Paciente paciente);

    List<Paciente> buscar(String apellido, String nombre, String dni, String email);
}
