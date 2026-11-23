package ar.edu.utn.turnos.pacientes.domain;

import java.util.List;
import java.util.Optional;

public interface PacienteRepositorio {

    Optional<Paciente> porId(Long id);

    Optional<Paciente> porEmail(String email);

    boolean existePorDni(String dni);

    Paciente guardar(Paciente paciente);

    List<Paciente> buscar(String apellido, String nombre, String dni, String email);
}
