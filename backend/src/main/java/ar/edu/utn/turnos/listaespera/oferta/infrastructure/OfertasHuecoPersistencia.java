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

    /**
     * Pasa a {@code TOMADA} todas las ofertas {@code ENVIADA} del hueco (mismo profesional e intervalo), incluida la
     * que se tomó (RN-24, C-42). Las demás quedan informativas: "hueco ya tomado".
     */
    public int tomarOfertasDelHueco(Long profesionalId, Instant huecoInicio, Instant huecoFin) {
        return ofertas.tomarDelHueco(profesionalId, huecoInicio, huecoFin);
    }

    /**
     * Pasa a {@code TOMADA} todas las ofertas {@code ENVIADA} del profesional con ese inicio de hueco, sin importar
     * su fin (asignación manual, C-43 design D6). Idempotente.
     */
    public int tomarDelHueco(Long profesionalId, Instant huecoInicio) {
        return ofertas.tomarDelHuecoPorInicio(profesionalId, huecoInicio);
    }

    /**
     * Una oferta del hueco para leer su sillón sugerido (C-43 design D4): la de la entrada si existe, si no la
     * primera del mismo profesional e inicio.
     */
    public Optional<OfertaHueco> sugeridaPara(Long entradaId, Long profesionalId, Instant huecoInicio) {
        return ofertas.delHuecoPrefiriendo(entradaId, profesionalId, huecoInicio).stream().findFirst()
                .map(OfertaHuecoJpa::dominio);
    }

    /** Pasa a {@code VENCIDA} una oferta {@code ENVIADA} cuyo hueco ya empezó (C-42). Idempotente. */
    public int vencerOferta(Long id) {
        return ofertas.vencerOferta(id);
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
