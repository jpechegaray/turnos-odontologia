package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.seguridad.domain.Rol;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Profile("dev")
@Component
class AdminDesarrollo implements ApplicationRunner {

    private static final String EMAIL = "admin@consultorio.test";

    private final UsuarioJpaRepository usuarios;
    private final Clock reloj;
    private final String clave;

    AdminDesarrollo(UsuarioJpaRepository usuarios, Clock reloj, @Value("${SEED_ADMIN_PASSWORD:}") String clave) {
        this.usuarios = usuarios;
        this.reloj = reloj;
        this.clave = clave;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (clave.isBlank() || usuarios.existsByEmail(EMAIL)) {
            return;
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("SEED_ADMIN_PASSWORD no puede superar los 72 bytes UTF-8");
        }
        usuarios.saveAndFlush(new Usuario(EMAIL, new BCryptPasswordEncoder().encode(clave),
                "Administrador de desarrollo", Rol.ADMIN, reloj.instant()));
    }
}
