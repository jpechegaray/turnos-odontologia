package ar.edu.utn.turnos.shared.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Genera, hashea y compara los tokens secretos de acceso sin login (RN-25, RN-38, DD-06).
 * Dominio puro: sin Spring ni JPA. Es seguro entre hilos (un {@code MessageDigest} por llamada).
 *
 * <p>El valor en claro ({@link TokenGenerado#claro()}) nunca se loguea ni se expone como DTO. La
 * vigencia y la revocación del token son responsabilidad de C-32 y C-36.
 */
public final class ServicioToken {

    private static final int BYTES_TOKEN = 32;
    private static final int LONGITUD_HASH = 64;

    private final SecureRandom aleatorio = new SecureRandom();

    /** Genera un token de 256 bits en base64url sin relleno (43 caracteres) junto con su hash. */
    public TokenGenerado generar() {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        String claro = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new TokenGenerado(claro, hashear(claro));
    }

    /**
     * SHA-256 del token sobre UTF-8, en 64 caracteres hexadecimales en minúscula.
     *
     * @throws IllegalArgumentException si el valor es nulo o está en blanco (el mensaje no incluye el valor)
     */
    public String hashear(String claro) {
        if (claro == null || claro.isBlank()) {
            throw new IllegalArgumentException("El token no puede estar vacío");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(claro.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("El JDK no provee SHA-256", e);
        }
    }

    /**
     * Indica si el token en claro corresponde al hash guardado, comparando en tiempo constante.
     * Nunca lanza: cualquier entrada inválida da {@code false}.
     */
    public boolean coincide(String claro, String hashGuardado) {
        if (claro == null || claro.isBlank() || hashGuardado == null || hashGuardado.length() != LONGITUD_HASH) {
            return false;
        }
        return MessageDigest.isEqual(
                hashear(claro).getBytes(StandardCharsets.US_ASCII),
                hashGuardado.getBytes(StandardCharsets.US_ASCII));
    }
}
