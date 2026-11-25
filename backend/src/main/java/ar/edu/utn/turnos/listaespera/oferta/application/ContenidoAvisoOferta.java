package ar.edu.utn.turnos.listaespera.oferta.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.shared.notificacion.application.DatosNotificacion;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Arma el contenido del email {@code OFERTA_HUECO} (RN-31, RN-38): saludo, inicio del hueco, prestación,
 * profesional y el enlace {@code <url pública>/oferta/<token>}. Nunca loguea el enlace.
 */
@Component
class ContenidoAvisoOferta {
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalConsulta profesionales;
    private final String urlPublica;

    ContenidoAvisoOferta(ConsultaPrestaciones prestaciones, ProfesionalConsulta profesionales,
            @Value("${app.listaespera.url-publica:http://localhost:5173}") String urlPublica) {
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.urlPublica = urlPublica.endsWith("/") ? urlPublica.substring(0, urlPublica.length() - 1) : urlPublica;
    }

    DatosNotificacion datos(Paciente paciente, Instant inicio, String prestacion, String profesional, String token) {
        return new DatosNotificacion(paciente.email(), (paciente.nombre() + " " + paciente.apellido()).trim(), inicio,
                prestacion, profesional, urlPublica + "/oferta/" + token);
    }

    /** Email {@code CONFIRMACION_RESERVA} del turno tomado (C-42): enlace {@code <url pública>/turno/<token>}. */
    DatosNotificacion confirmacion(Paciente paciente, Instant inicio, String prestacion, String profesional,
            String tokenTurno) {
        return new DatosNotificacion(paciente.email(), (paciente.nombre() + " " + paciente.apellido()).trim(), inicio,
                prestacion, profesional, urlPublica + "/turno/" + tokenTurno);
    }

    String nombreProfesional(Long id) {
        return profesionales.buscarPorId(id).map(p -> (p.nombre() + " " + p.apellido()).trim()).orElse(null);
    }

    String nombrePrestacion(Long id) {
        return id == null ? null : prestaciones.buscarPorId(id).map(PrestacionResumen::nombre).orElse(null);
    }
}
