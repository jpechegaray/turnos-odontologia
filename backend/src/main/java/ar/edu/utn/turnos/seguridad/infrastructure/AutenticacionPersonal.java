package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.domain.EmailUsuario;
import ar.edu.utn.turnos.seguridad.domain.PoliticaBloqueo;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacionPersonal {

    private final UsuarioJpaRepository usuarios;
    private final PoliticaBloqueo politica;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    private final String hashAuxiliar = bcrypt.encode(UUID.randomUUID().toString());

    AutenticacionPersonal(UsuarioJpaRepository usuarios, Clock reloj) {
        this.usuarios = usuarios;
        this.politica = new PoliticaBloqueo(reloj);
    }

    @Transactional
    public Optional<IdentidadPersonal> autenticar(String email, String clave) {
        var usuario = usuarios.findByEmail(EmailUsuario.normalizar(email)).orElse(null);
        boolean habilitado = usuario != null && usuario.isActivo()
                && !politica.estaBloqueado(usuario.estadoDeBloqueo());
        boolean longitudValida = clave.getBytes(StandardCharsets.UTF_8).length <= 72;
        boolean coincide = bcrypt.matches(longitudValida ? clave : "",
                habilitado ? usuario.getPasswordHash() : hashAuxiliar);

        if (!habilitado) {
            return Optional.empty();
        }
        if (!longitudValida || !coincide) {
            usuario.actualizarBloqueo(politica.registrarFallo(usuario.estadoDeBloqueo()));
            return Optional.empty();
        }
        usuario.actualizarBloqueo(politica.registrarExito());
        return Optional.of(new IdentidadPersonal(usuario.getId(), usuario.getEmail(), usuario.getRol()));
    }
}
