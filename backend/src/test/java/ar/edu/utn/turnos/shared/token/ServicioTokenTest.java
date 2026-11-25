package ar.edu.utn.turnos.shared.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ServicioTokenTest {

    private static final String HASH_DE_63 = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef012345678";
    private static final String HASH_DE_65 = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef01234567890";

    private final ServicioToken servicio = new ServicioToken();

    @Test
    @DisplayName("Un token generado tiene 43 caracteres del alfabeto base64url")
    void tokenGeneradoTiene43CaracteresBase64Url() {
        TokenGenerado token = servicio.generar();

        assertThat(token.claro()).matches("^[A-Za-z0-9_-]{43}$");
    }

    @Test
    @DisplayName("Un token generado decodifica a 32 bytes")
    void tokenGeneradoDecodificaA32Bytes() {
        TokenGenerado token = servicio.generar();

        assertThat(Base64.getUrlDecoder().decode(token.claro())).hasSize(32);
    }

    @Test
    @DisplayName("Diez mil tokens generados son todos distintos")
    void diezMilTokensSonDistintos() {
        Set<String> claros = new HashSet<>();
        Set<String> hashes = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            TokenGenerado token = servicio.generar();
            claros.add(token.claro());
            hashes.add(token.hash());
        }

        assertThat(claros).hasSize(10_000);
        assertThat(hashes).hasSize(10_000);
    }

    @Test
    @DisplayName("El hash coincide con el vector conocido de SHA-256")
    void hashCoincideConVectorConocido() {
        assertThat(servicio.hashear("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("El hash de un token es siempre el mismo")
    void hashEsDeterminista() {
        String token = servicio.generar().claro();

        String primero = servicio.hashear(token);
        String segundo = servicio.hashear(token);

        assertThat(primero).isEqualTo(segundo).matches("^[0-9a-f]{64}$");
    }

    @Test
    @DisplayName("Tokens distintos producen hashes distintos")
    void tokensDistintosHashesDistintos() {
        assertThat(servicio.hashear("token-uno")).isNotEqualTo(servicio.hashear("token-dos"));
    }

    @Test
    @DisplayName("El token generado trae el hash de su valor en claro")
    void tokenGeneradoTraeSuHash() {
        TokenGenerado token = servicio.generar();

        assertThat(token.hash()).isEqualTo(servicio.hashear(token.claro()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Hashear un valor nulo o en blanco se rechaza")
    void hashearNuloOEnBlancoSeRechaza(String invalido) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> servicio.hashear(invalido))
                .withMessage("El token no puede estar vacío");
    }

    @Test
    @DisplayName("Un token coincide con su propio hash")
    void tokenCoincideConSuHash() {
        TokenGenerado token = servicio.generar();

        assertThat(servicio.coincide(token.claro(), token.hash())).isTrue();
    }

    @Test
    @DisplayName("Un token no coincide con el hash de otro token")
    void tokenNoCoincideConHashAjeno() {
        TokenGenerado uno = servicio.generar();
        TokenGenerado otro = servicio.generar();

        assertThat(servicio.coincide(uno.claro(), otro.hash())).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", HASH_DE_63, HASH_DE_65})
    @DisplayName("Un hash nulo o de otra longitud no coincide")
    void hashNuloOdeOtraLongitudNoCoincide(String hashInvalido) {
        TokenGenerado token = servicio.generar();

        assertThat(servicio.coincide(token.claro(), hashInvalido)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Un token nulo o en blanco no coincide")
    void tokenNuloOEnBlancoNoCoincide(String tokenInvalido) {
        String hashValido = servicio.generar().hash();

        assertThat(servicio.coincide(tokenInvalido, hashValido)).isFalse();
    }

    @Test
    @DisplayName("La comparación usa una primitiva de tiempo constante")
    void comparacionUsaPrimitivaDeTiempoConstante() {
        JavaClass clase = new ClassFileImporter().importClasses(ServicioToken.class).get(ServicioToken.class);

        boolean llamaAIsEqual = clase.getMethodCallsFromSelf().stream()
                .anyMatch(llamada -> esLlamadaA(llamada, "java.security.MessageDigest", "isEqual", "[B", "[B"));
        boolean llamaAStringEquals = clase.getMethodCallsFromSelf().stream()
                .anyMatch(llamada -> esLlamadaA(llamada, "java.lang.String", "equals", "java.lang.Object"));

        assertThat(llamaAIsEqual).as("llama a MessageDigest.isEqual(byte[], byte[])").isTrue();
        assertThat(llamaAStringEquals).as("llama a String.equals(Object)").isFalse();
    }

    private static boolean esLlamadaA(JavaCall<?> llamada, String propietario, String metodo, String... parametros) {
        return llamada.getTargetOwner().getFullName().equals(propietario)
                && llamada.getName().equals(metodo)
                && llamada.getTarget().getRawParameterTypes().stream()
                        .map(JavaClass::getName).toList().equals(java.util.List.of(parametros));
    }
}
