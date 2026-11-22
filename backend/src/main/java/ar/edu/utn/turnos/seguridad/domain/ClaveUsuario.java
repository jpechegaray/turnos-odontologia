package ar.edu.utn.turnos.seguridad.domain;

import java.nio.charset.StandardCharsets;

/** Largo admitido para una contraseña nueva: BCrypt solo considera los primeros 72 bytes (RN-34). */
public final class ClaveUsuario {

    public static final int MINIMO_CARACTERES = 8;
    public static final int MAXIMO_BYTES = 72;

    private ClaveUsuario() {
    }

    public static boolean esValida(String clave) {
        return clave != null
                && clave.codePointCount(0, clave.length()) >= MINIMO_CARACTERES
                && clave.getBytes(StandardCharsets.UTF_8).length <= MAXIMO_BYTES;
    }
}
