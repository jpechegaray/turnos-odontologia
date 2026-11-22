package ar.edu.utn.turnos.profesionales.horario.infrastructure;

import ar.edu.utn.turnos.profesionales.horario.application.HorarioConsulta;
import ar.edu.utn.turnos.profesionales.horario.application.HorarioDatos;
import ar.edu.utn.turnos.profesionales.horario.application.Tramo;
import ar.edu.utn.turnos.profesionales.horario.domain.FranjaLocal;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class HorariosPersistencia implements HorarioConsulta {
    private static final Long NINGUNO = -1L;

    private final HorarioJpaRepository horarios;

    HorariosPersistencia(HorarioJpaRepository horarios) {
        this.horarios = horarios;
    }

    /** Debe llamarse dentro de una transacción de escritura; devuelve falso si el profesional no existe. */
    public boolean bloquearProfesional(Long profesionalId) {
        return horarios.bloquearProfesional(profesionalId).isPresent();
    }

    public boolean existeSuperpuesto(Long profesionalId, Tramo tramo, Optional<Long> excluido) {
        return horarios.existeSuperpuesto(profesionalId, tramo.diaSemana(), texto(tramo.horaInicio()),
                texto(tramo.horaFin()), excluido.orElse(NINGUNO));
    }

    public HorarioDatos crear(Long profesionalId, Tramo tramo) {
        return horarios.save(new HorarioAtencion(profesionalId, tramo)).datos();
    }

    public List<HorarioDatos> listar(Long profesionalId) {
        return horarios.findByProfesionalIdOrderByDiaSemanaAscHoraInicioAscIdAsc(profesionalId).stream()
                .map(HorarioAtencion::datos).toList();
    }

    public boolean pertenece(Long profesionalId, Long id) {
        return horarios.existsByIdAndProfesionalId(id, profesionalId);
    }

    public Optional<HorarioDatos> reemplazar(Long profesionalId, Long id, Tramo tramo) {
        return horarios.findByIdAndProfesionalId(id, profesionalId).map(horario -> {
            horario.reemplazar(tramo);
            return horarios.saveAndFlush(horario).datos();
        });
    }

    public boolean eliminar(Long profesionalId, Long id) {
        return horarios.eliminar(profesionalId, id) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean contiene(Long profesionalId, Intervalo intervalo) {
        return FranjaLocal.de(intervalo)
                .map(franja -> horarios.contiene(profesionalId, franja.diaSemana(), texto(franja.inicio()),
                        texto(franja.fin())))
                .orElse(false);
    }

    /** ISO {@code HH:mm[:ss[.nnn]]}, que PostgreSQL castea a {@code time} sin zona. */
    private static String texto(LocalTime hora) {
        return hora.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }
}
