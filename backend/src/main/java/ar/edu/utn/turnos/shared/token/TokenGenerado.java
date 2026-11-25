package ar.edu.utn.turnos.shared.token;

/**
 * Token recién generado: el valor en claro (que se entrega una sola vez al paciente) y su hash
 * SHA-256 (lo único que se persiste, RN-25).
 *
 * <p>{@code claro()} nunca se loguea ni se expone en un DTO. {@link #toString()} oculta ambos
 * valores para que registrar el objeto por accidente no filtre el token. La vigencia y la
 * revocación del token no son de esta clase: las resuelven C-32 (turno) y C-36 (oferta de hueco).
 *
 * @param claro valor en claro, base64url de 43 caracteres
 * @param hash  SHA-256 del valor en claro, 64 caracteres hexadecimales en minúscula
 */
public record TokenGenerado(String claro, String hash) {

    private static final String OCULTO = "***";

    public TokenGenerado {
        if (claro == null || hash == null) {
            throw new IllegalArgumentException("El token generado requiere el valor en claro y su hash");
        }
    }

    @Override
    public String toString() {
        return "TokenGenerado[claro=" + OCULTO + ", hash=" + OCULTO + "]";
    }
}
