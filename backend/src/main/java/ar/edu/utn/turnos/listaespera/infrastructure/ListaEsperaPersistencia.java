package ar.edu.utn.turnos.listaespera.infrastructure;

import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class ListaEsperaPersistencia {
    static final String INDICE_ACTIVAS = "ux_lista_espera_activa";

    private final ListaEsperaJpaRepository entradas;

    ListaEsperaPersistencia(ListaEsperaJpaRepository entradas) {
        this.entradas = entradas;
    }

    /**
     * Guarda y sincroniza enseguida para que una violación del índice parcial de activas (RN-21) se
     * traduzca acá a {@code ENTRADA_ACTIVA_DUPLICADA} y no al confirmar la transacción.
     */
    public EntradaListaEspera guardar(EntradaListaEspera entrada) {
        try {
            return entradas.saveAndFlush(ListaEsperaJpa.de(entrada)).dominio();
        } catch (DataIntegrityViolationException ex) {
            if (mencionaIndiceDeActivas(ex)) {
                throw duplicada();
            }
            throw ex;
        }
    }

    public boolean existeActiva(Long pacienteId, Long prestacionId) {
        return entradas.existsByPacienteIdAndPrestacionIdAndEstado(pacienteId, prestacionId, EstadoListaEspera.ACTIVA);
    }

    public Optional<EntradaListaEspera> buscar(Long id) {
        return entradas.findById(id).map(ListaEsperaJpa::dominio);
    }

    public List<EntradaListaEspera> filtrar(EstadoListaEspera estado, Long pacienteId, Long prestacionId) {
        return entradas.filtrar(estado, pacienteId, prestacionId).stream().map(ListaEsperaJpa::dominio).toList();
    }

    public int vencerAnterioresA(LocalDate hoy) {
        return entradas.vencerAnterioresA(hoy);
    }

    public static ReglaNegocioException duplicada() {
        return new ReglaNegocioException("ENTRADA_ACTIVA_DUPLICADA", EstadoError.CONFLICTO_409,
                "El paciente ya tiene una entrada activa para esa prestación");
    }

    private static boolean mencionaIndiceDeActivas(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa.getMessage() != null && causa.getMessage().contains(INDICE_ACTIVAS)) {
                return true;
            }
        }
        return false;
    }
}
