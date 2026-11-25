package ar.edu.utn.turnos.pacientes.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.domain.PacienteRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Override
    public Map<Long, Paciente> porIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return repositorio.porIds(ids).stream().collect(Collectors.toUnmodifiableMap(Paciente::id, p -> p));
    }
}
