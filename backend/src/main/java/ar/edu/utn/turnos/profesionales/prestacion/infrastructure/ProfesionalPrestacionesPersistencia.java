package ar.edu.utn.turnos.profesionales.prestacion.infrastructure;

import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProfesionalPrestacionesPersistencia implements ProfesionalRealizaPrestacion {

    private final ProfesionalPrestacionJpaRepository repositorio;

    ProfesionalPrestacionesPersistencia(ProfesionalPrestacionJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean realiza(Long profesionalId, Long prestacionId) {
        return repositorio.existsById(new ProfesionalPrestacionId(profesionalId, prestacionId));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> profesionalesQueRealizan(Long prestacionId) {
        return Set.copyOf(repositorio.profesionalIdsDePrestacion(prestacionId));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> prestacionesConProfesional(Collection<Long> profesionalIds) {
        if (profesionalIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(repositorio.prestacionIdsDeProfesionales(profesionalIds));
    }

    @Transactional(readOnly = true)
    public Set<Long> prestacionIdsDe(Long profesionalId) {
        return repositorio.findByIdProfesionalId(profesionalId).stream()
                .map(ProfesionalPrestacion::getPrestacionId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void reemplazar(Long profesionalId, Collection<Long> altas, Collection<Long> bajas) {
        if (!bajas.isEmpty()) {
            repositorio.deleteByIdProfesionalIdAndIdPrestacionIdIn(profesionalId, bajas);
        }
        if (!altas.isEmpty()) {
            var nuevas = altas.stream()
                    .map(prestacionId -> new ProfesionalPrestacion(profesionalId, prestacionId))
                    .toList();
            repositorio.saveAllAndFlush(nuevas);
        }
    }
}
