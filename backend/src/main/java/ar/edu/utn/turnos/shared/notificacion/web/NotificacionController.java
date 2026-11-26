package ar.edu.utn.turnos.shared.notificacion.web;

import ar.edu.utn.turnos.shared.notificacion.application.ConsultaNotificaciones;
import ar.edu.utn.turnos.shared.notificacion.application.ConsultaVistaPrevia;
import ar.edu.utn.turnos.shared.notificacion.domain.EstadoNotificacion;
import ar.edu.utn.turnos.shared.paginacion.Pagina;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;

/**
 * Registro de notificaciones (C-25, US-020). Un estado desconocido o una paginación fuera de rango
 * responden 400 en Problem Details mediante el manejo de errores común.
 *
 * <p>Sin {@code @Validated} a propósito: así Spring MVC valida los {@code @RequestParam} y lanza
 * {@code HandlerMethodValidationException} (400). Con {@code @Validated} la validación la hace el
 * proxy AOP, lanza {@code ConstraintViolationException} y termina en el 500 genérico.
 */
@RestController
@RequestMapping("/api/notificaciones")
class NotificacionController {

    static final int TAMANIO_MAXIMO = 100;

    private final ConsultaNotificaciones consulta;
    private final ConsultaVistaPrevia vistaPrevia;

    NotificacionController(ConsultaNotificaciones consulta, ConsultaVistaPrevia vistaPrevia) {
        this.consulta = consulta;
        this.vistaPrevia = vistaPrevia;
    }

    @GetMapping("/{id}/vista-previa")
    ResponseEntity<VistaPrevia> vistaPrevia(@PathVariable Long id) {
        return vistaPrevia.consultar(id)
                .map(mensaje -> ResponseEntity.ok().cacheControl(CacheControl.noStore())
                        .body(new VistaPrevia(mensaje.asunto(), mensaje.cuerpo())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    record VistaPrevia(String asunto, String cuerpo) {}

    @GetMapping
    Pagina<RespuestaNotificacion> listar(
            @RequestParam(required = false) EstadoNotificacion estado,
            @RequestParam(required = false) Long turnoId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(TAMANIO_MAXIMO) int tamanio) {
        return consulta.listar(estado, turnoId, pagina, tamanio).map(RespuestaNotificacion::de);
    }
}
