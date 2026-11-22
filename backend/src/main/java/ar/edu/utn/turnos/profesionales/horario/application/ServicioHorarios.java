package ar.edu.utn.turnos.profesionales.horario.application;

import ar.edu.utn.turnos.profesionales.horario.infrastructure.HorariosPersistencia;
import ar.edu.utn.turnos.profesionales.profesional.application.ServicioProfesionales;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioHorarios {
    private static final String PUEDE_GESTIONAR = "hasAnyRole('ADMIN', 'RECEPCION')";
    private static final String PUEDE_CONSULTAR = "hasAnyRole('ADMIN', 'RECEPCION', 'ODONTOLOGO')";

    private final HorariosPersistencia horarios;
    private final ServicioProfesionales catalogo;

    public ServicioHorarios(HorariosPersistencia horarios, ServicioProfesionales catalogo) {
        this.horarios = horarios;
        this.catalogo = catalogo;
    }

    @PreAuthorize(PUEDE_GESTIONAR)
    @Transactional
    public HorarioDatos crear(Long profesionalId, Tramo tramo) {
        bloquearProfesional(profesionalId);
        verificarSinSuperposicion(profesionalId, tramo, Optional.empty());
        return horarios.crear(profesionalId, tramo);
    }

    @PreAuthorize(PUEDE_CONSULTAR)
    @Transactional(readOnly = true)
    public List<HorarioDatos> listar(Long profesionalId) {
        // Aplica la titularidad del odontólogo (403) antes de revelar si el profesional existe.
        catalogo.detalle(profesionalId);
        return horarios.listar(profesionalId);
    }

    @PreAuthorize(PUEDE_GESTIONAR)
    @Transactional
    public HorarioDatos reemplazar(Long profesionalId, Long horarioId, Tramo tramo) {
        bloquearProfesional(profesionalId);
        if (!horarios.pertenece(profesionalId, horarioId)) {
            throw horarioNoEncontrado();
        }
        verificarSinSuperposicion(profesionalId, tramo, Optional.of(horarioId));
        return horarios.reemplazar(profesionalId, horarioId, tramo).orElseThrow(ServicioHorarios::horarioNoEncontrado);
    }

    @PreAuthorize(PUEDE_GESTIONAR)
    @Transactional
    public void eliminar(Long profesionalId, Long horarioId) {
        bloquearProfesional(profesionalId);
        if (!horarios.eliminar(profesionalId, horarioId)) {
            throw horarioNoEncontrado();
        }
    }

    /** Serializa las escrituras de tramos de un mismo profesional (RN-16 ante concurrencia). */
    private void bloquearProfesional(Long profesionalId) {
        if (!horarios.bloquearProfesional(profesionalId)) {
            throw new ReglaNegocioException("PROFESIONAL_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                    "No se encontró el profesional");
        }
    }

    private void verificarSinSuperposicion(Long profesionalId, Tramo tramo, Optional<Long> excluido) {
        if (horarios.existeSuperpuesto(profesionalId, tramo, excluido)) {
            throw new ReglaNegocioException("HORARIO_SUPERPUESTO", EstadoError.CONFLICTO_409,
                    "El tramo se superpone con otro del mismo profesional y día");
        }
    }

    private static ReglaNegocioException horarioNoEncontrado() {
        return new ReglaNegocioException("HORARIO_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                "No se encontró el horario");
    }
}
