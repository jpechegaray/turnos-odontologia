package ar.edu.utn.turnos.seguridad.domain;

import java.util.Locale;

public final class EmailUsuario {

    private EmailUsuario() {
    }

    public static String normalizar(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
