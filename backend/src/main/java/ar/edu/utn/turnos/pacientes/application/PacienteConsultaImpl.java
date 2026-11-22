package ar.edu.utn.turnos.pacientes.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.domain.PacienteRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Optional;

/**
 * Implementation of {@link PacienteConsulta}.
 * Provides read‑only access to patient data for other modules.
 */
@Component
public class PacienteConsultaImpl implements PacienteConsulta {

    private final PacienteRepositorio repositorio;

    @Autowired
    public PacienteConsultaImpl(PacienteRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Paciente> porId(Long id) {
        return repositorio.porId(id);
    }

    @Override
    public Optional<Paciente> porEmail(String email) {
        return repositorio.porEmail(email);
    }
}
