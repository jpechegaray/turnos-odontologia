package ar.edu.utn.turnos.listaespera.oferta.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Aviso de un hueco liberado a una entrada de la lista de espera (RN-23, RN-38). Guarda solo el hash del token;
 * el valor en claro nunca llega a esta clase. Vence al inicio del hueco. Inmutable.
 */
public record OfertaHueco(
        Long id,
        Long listaEsperaId,
        Long profesionalId,
        Instant huecoInicio,
        Instant huecoFin,
        Long sillonSugeridoId,
        String tokenHash,
        Instant expiraEn,
        EstadoOferta estado,
        Instant creadoEn) {

    public OfertaHueco {
        Objects.requireNonNull(listaEsperaId, "listaEsperaId");
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(huecoInicio, "huecoInicio");
        Objects.requireNonNull(huecoFin, "huecoFin");
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(expiraEn, "expiraEn");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(creadoEn, "creadoEn");
        if (!huecoFin.isAfter(huecoInicio)) {
            throw new IllegalArgumentException("El fin del hueco debe ser posterior al inicio");
        }
    }

    /** Oferta recién creada: {@code ENVIADA} y vigente hasta el inicio del hueco. */
    public static OfertaHueco nueva(Long listaEsperaId, Long profesionalId, Instant huecoInicio, Instant huecoFin,
            Long sillonSugeridoId, String tokenHash, Instant ahora) {
        return new OfertaHueco(null, listaEsperaId, profesionalId, huecoInicio, huecoFin, sillonSugeridoId, tokenHash,
                huecoInicio, EstadoOferta.ENVIADA, ahora);
    }

    /** Vigente: enviada y antes del inicio del hueco. */
    public boolean vigente(Instant ahora) {
        return estado == EstadoOferta.ENVIADA && ahora.isBefore(expiraEn);
    }

    /** La misma oferta con otro token (reintento del aviso, design D5). */
    public OfertaHueco conToken(String nuevoHash) {
        return new OfertaHueco(id, listaEsperaId, profesionalId, huecoInicio, huecoFin, sillonSugeridoId, nuevoHash,
                expiraEn, estado, creadoEn);
    }

    @Override
    public String toString() {
        return "OfertaHueco[id=" + id + ", listaEsperaId=" + listaEsperaId + ", estado=" + estado + "]";
    }
}
