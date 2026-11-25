package ar.edu.utn.turnos.listaespera.oferta.infrastructure;

import ar.edu.utn.turnos.listaespera.oferta.domain.OfertaHueco;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class OfertasHuecoPersistencia {
    private final OfertaHuecoJpaRepository ofertas;

    OfertasHuecoPersistencia(OfertaHuecoJpaRepository ofertas) {
        this.ofertas = ofertas;
    }

    /**
     * Inserta o actualiza y sincroniza enseguida, así una violación de unicidad salta acá. Quien inserta ofertas
     * nuevas atrapa la violación fuera de su transacción y la clasifica con {@link #esOfertaRepetida}.
     */
    public OfertaHueco guardar(OfertaHueco oferta) {
        return ofertas.saveAndFlush(OfertaHuecoJpa.de(oferta)).dominio();
    }

    public Optional<OfertaHueco> porTokenHash(String tokenHash) {
        return ofertas.findByTokenHash(tokenHash).map(OfertaHuecoJpa::dominio);
    }

    /** Lee la oferta con lock de fila (reintento del aviso y, en C-42, la toma). */
    public Optional<OfertaHueco> bloquear(Long id) {
        return ofertas.bloquear(id).map(OfertaHuecoJpa::dominio);
    }

    public int vencer(Instant ahora) {
        return ofertas.vencer(ahora);
    }

    /** La entrada ya tiene oferta para ese hueco: una sola tanda (SU-14 b). */
    public static boolean esOfertaRepetida(DataIntegrityViolationException ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            var mensaje = causa.getMessage();
            if (mensaje != null && mensaje.toLowerCase(Locale.ROOT).contains(OfertaHuecoJpa.UK_ENTRADA_HUECO)) {
                return true;
            }
        }
        return false;
    }
}
