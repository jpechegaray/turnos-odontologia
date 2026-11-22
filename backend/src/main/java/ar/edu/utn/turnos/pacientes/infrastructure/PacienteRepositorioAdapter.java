package ar.edu.utn.turnos.pacientes.infrastructure;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.domain.PacienteRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Adapter that implements the domain {@link PacienteRepositorio} using JPA.
 * All persistence rules (unique DNI, case‑insensitive searches, active filter) are applied here.
 */
@Component
public class PacienteRepositorioAdapter implements PacienteRepositorio {

    private final PacienteRepositorioJpa jpaRepository;

    @Autowired
    public PacienteRepositorioAdapter(PacienteRepositorioJpa jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Paciente> porId(Long id) {
        return jpaRepository.findById(id).map(PacienteJpa::aDominio);
    }

    @Override
    public Optional<Paciente> porEmail(String email) {
        if (email == null) return Optional.empty();
        return jpaRepository.findByEmailIgnoreCase(email).map(PacienteJpa::aDominio);
    }

    @Override
    public boolean existePorDni(String dni) {
        if (dni == null) return false;
        return jpaRepository.existsByDni(dni);
    }

    @Override
    public Paciente guardar(Paciente paciente) {
        Paciente toSave = (paciente.creadoEn() == null) ? paciente.conCreacion(Instant.now()) : paciente;
        try {
            PacienteJpa entity = PacienteJpa.desdeDominio(toSave);
            PacienteJpa saved = jpaRepository.saveAndFlush(entity);
            return saved.aDominio();
        } catch (DataIntegrityViolationException e) {
            throw e;
        }
    }

    @Override
    public List<Paciente> buscar(String apellido, String nombre, String dni, String email) {
        String apPattern = (apellido != null && !apellido.isBlank()) ? "%" + apellido.trim().toLowerCase() + "%" : null;
        String nomPattern = (nombre != null && !nombre.isBlank()) ? "%" + nombre.trim().toLowerCase() + "%" : null;
        String dniVal = (dni != null && !dni.isBlank()) ? dni.trim() : null;
        String emailVal = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : null;

        return jpaRepository.buscar(apPattern, nomPattern, dniVal, emailVal)
                .stream()
                .map(PacienteJpa::aDominio)
                .collect(Collectors.toList());
    }
}
