package ar.edu.utn.turnos.reservapublica.gestiontoken;

import ar.edu.utn.turnos.agenda.application.CancelarTurnoService;
import ar.edu.utn.turnos.agenda.application.TurnosParaGestionPublica;
import ar.edu.utn.turnos.agenda.domain.ActorCancelacion;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.PoliticaCancelacion;
import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GestionTurnoPorToken {
    private final ServicioTokenTurno tokens;
    private final TurnosParaGestionPublica agenda;
    private final CancelarTurnoService cancelaciones;
    private final ConfiguracionServicio configuracion;
    private final Clock reloj;

    GestionTurnoPorToken(ServicioTokenTurno tokens, TurnosParaGestionPublica agenda,
            CancelarTurnoService cancelaciones, ConfiguracionServicio configuracion, Clock reloj) {
        this.tokens = tokens;
        this.agenda = agenda;
        this.cancelaciones = cancelaciones;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    @Transactional
    public Detalle consultar(String token) {
        return detalle(agenda.consultar(tokens.validar(token)));
    }

    @Transactional
    public Detalle confirmar(String token) {
        Long id = tokens.validar(token);
        return agenda.confirmar(id, reloj.instant()).map(this::detalle)
                .orElseThrow(ServicioTokenTurno::tokenNoEncontrado);
    }

    @Transactional
    public void cancelar(String token) {
        Long id = tokens.validar(token);
        try {
            cancelaciones.cancelar(id, ActorCancelacion.PACIENTE_TOKEN, null, null);
        } catch (ReglaNegocioException ex) {
            // Si el inicio se cruzó desde la validación, la regla interna RN-05 se traduce a credencial vencida.
            if ("RN-05".equals(ex.codigo())) {
                throw ServicioTokenTurno.tokenNoEncontrado();
            }
            throw ex;
        }
    }

    private Detalle detalle(TurnosParaGestionPublica.Datos datos) {
        boolean tardia = PoliticaCancelacion.esTardia(datos.inicio(), reloj.instant(),
                Duration.ofHours(configuracion.plazoCancelacionHoras()));
        return new Detalle(datos.inicio(), datos.fin(), datos.estado(), datos.profesional(), datos.prestacion(), tardia);
    }

    record Detalle(Instant inicio, Instant fin, EstadoTurno estado, String profesional, String prestacion,
            boolean cancelacionSeriaTardia) {
    }
}
