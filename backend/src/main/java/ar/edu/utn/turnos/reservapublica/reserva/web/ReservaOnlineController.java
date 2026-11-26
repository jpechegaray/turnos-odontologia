package ar.edu.utn.turnos.reservapublica.reserva.web;

import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.reservapublica.reserva.application.ConsentimientoDatos;
import ar.edu.utn.turnos.reservapublica.reserva.application.ReservaOnline;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reserva online anónima (US-015) y datos públicos del consultorio. Sin sesión ni CSRF, con límite de tasa. */
@RestController
@RequestMapping("/api/public")
public class ReservaOnlineController {

    private final ReservaOnline reservas;
    private final ConfiguracionServicio configuracion;

    public ReservaOnlineController(ReservaOnline reservas, ConfiguracionServicio configuracion) {
        this.reservas = reservas;
        this.configuracion = configuracion;
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaCreadaDto reservar(@Valid @RequestBody SolicitudReservaPublica solicitud) {
        return ReservaCreadaDto.de(reservas.reservar(solicitud.aSolicitud()));
    }

    @GetMapping("/consultorio")
    public ConsultorioPublicoDto consultorio() {
        return new ConsultorioPublicoDto(configuracion.nombreConsultorio(), configuracion.domicilioConsultorio(),
                configuracion.emailContactoConsultorio(), configuracion.retencionAnios(),
                ConsentimientoDatos.VERSION_VIGENTE);
    }
}
