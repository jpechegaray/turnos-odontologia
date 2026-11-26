package ar.edu.utn.turnos.pacientes.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.domain.PacienteRepositorio;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class AltaPacienteReservaOnlineImpl implements AltaPacienteReservaOnline {

    private final PacienteRepositorio repositorio;
    private final Clock reloj;

    AltaPacienteReservaOnlineImpl(PacienteRepositorio repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Paciente registrar(DatosFicha datos, String versionConsentimiento) {
        // Microsegundos: la precisión de timestamptz, para que lo leído sea igual a lo guardado.
        var ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        return repositorio.guardar(Paciente.nuevoDeReservaOnline(datos.nombre(), datos.apellido(), datos.email(),
                datos.telefono(), datos.obraSocial(), ahora, versionConsentimiento));
    }
}
