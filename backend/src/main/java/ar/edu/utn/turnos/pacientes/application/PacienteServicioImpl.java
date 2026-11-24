package ar.edu.utn.turnos.pacientes.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.domain.PacienteRepositorio;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de pacientes. El DNI es único solo entre fichas activas (índice parcial uk_paciente_dni_activo);
 * la traducción de {@link DataIntegrityViolationException} a 409 asume que ese índice es la única
 * restricción única de la tabla. Si otro change agrega una, hay que revisar esa traducción.
 */
@Service
public class PacienteServicioImpl implements PacienteServicio {

    private final PacienteRepositorio repositorio;
    private final Clock clock;

    @Autowired
    public PacienteServicioImpl(PacienteRepositorio repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Paciente crear(Paciente paciente) {
        String dni = paciente.dni();
        if (dni != null && repositorio.existePorDni(dni)) {
            throw new ReglaNegocioException("RN-32", EstadoError.CONFLICTO_409,
                    "El DNI ya está registrado");
        }
        Paciente conCreacion = (paciente.creadoEn() == null)
                ? paciente.conCreacion(Instant.now(clock).truncatedTo(ChronoUnit.MICROS))
                : paciente;
        try {
            return repositorio.guardar(conCreacion);
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("RN-32", EstadoError.CONFLICTO_409,
                    "El DNI ya está registrado");
        }
    }

    @Override
    @Transactional
    public Paciente actualizar(Long id, Paciente nuevosDatos) {
        Paciente existente = repositorio.porId(id)
                .orElseThrow(() -> new ReglaNegocioException("PACIENTE_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                        "Paciente no encontrado"));

        if (!existente.activo()) {
            throw new ReglaNegocioException("RN-32", EstadoError.NO_PROCESABLE_422,
                    "No se pueden actualizar fichas inactivas");
        }

        String nuevoDni = nuevosDatos.dni();
        if (nuevoDni != null && !nuevoDni.equals(existente.dni()) && repositorio.existePorDni(nuevoDni)) {
            throw new ReglaNegocioException("RN-32", EstadoError.CONFLICTO_409,
                    "El DNI ya está registrado");
        }

        Paciente actualizado = existente.conDatos(
                nuevosDatos.nombre(),
                nuevosDatos.apellido(),
                nuevoDni,
                nuevosDatos.email(),
                nuevosDatos.telefono(),
                nuevosDatos.obraSocial(),
                nuevosDatos.nroAfiliado(),
                nuevosDatos.notas());

        try {
            return repositorio.guardar(actualizado);
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("RN-32", EstadoError.CONFLICTO_409,
                    "El DNI ya está registrado");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Paciente obtener(Long id) {
        return repositorio.porId(id)
                .orElseThrow(() -> new ReglaNegocioException("PACIENTE_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                        "Paciente no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Paciente> buscar(String apellido, String nombre, String dni, String email) {
        return repositorio.buscar(apellido, nombre, dni, email);
    }

    @Override
    @Transactional
    public void darDeBaja(Long id) {
        Paciente existente = repositorio.porId(id)
                .orElseThrow(() -> new ReglaNegocioException("PACIENTE_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                        "Paciente no encontrado"));
        if (!existente.activo()) {
            return;
        }
        Paciente inactiva = existente.dadoDeBaja();
        repositorio.guardar(inactiva);
    }
}
