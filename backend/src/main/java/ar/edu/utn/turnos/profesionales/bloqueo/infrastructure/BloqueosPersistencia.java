package ar.edu.utn.turnos.profesionales.bloqueo.infrastructure;

import ar.edu.utn.turnos.profesionales.bloqueo.application.BloqueoConsulta;
import ar.edu.utn.turnos.profesionales.bloqueo.application.BloqueoDatos;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BloqueosPersistencia implements BloqueoConsulta {
    private final BloqueoJpaRepository bloqueos;

    BloqueosPersistencia(BloqueoJpaRepository bloqueos) {
        this.bloqueos = bloqueos;
    }

    public BloqueoDatos crear(Long profesionalId, Intervalo intervalo, String motivo) {
        return bloqueos.save(new Bloqueo(profesionalId, intervalo, motivo)).datos();
    }

    public List<BloqueoDatos> listar(Long profesionalId) {
        return bloqueos.findByProfesionalIdOrderByInicioAscIdAsc(profesionalId).stream().map(Bloqueo::datos).toList();
    }

    public boolean eliminar(Long profesionalId, Long id) {
        return bloqueos.eliminar(profesionalId, id) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean solapa(Long profesionalId, Intervalo intervalo) {
        return bloqueos.solapa(profesionalId, intervalo.inicio(), intervalo.fin());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueoDatos> queSolapan(Long profesionalId, Intervalo intervalo) {
        return bloqueos.queSolapan(profesionalId, intervalo.inicio(), intervalo.fin()).stream()
                .map(Bloqueo::datos).toList();
    }
}
