package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** Escenarios agregados por C-13 a la spec control-acceso, con login real y la misma cookie de sesión. */
@Transactional
class SesionVigenteIT extends PruebaIntegracionBase {

    private static final String CLAVE = "clave-fixture-c13";
    private static final String HASH = new BCryptPasswordEncoder().encode(CLAVE);
    private static final Instant CREADO = Instant.parse("2026-11-22T12:00:00Z");

    @Autowired private UsuarioJpaRepository usuarios;

    @Test
    @DisplayName("La sesión de un usuario desactivado deja de ser válida")
    void desactivado() throws Exception {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("c13-sv1@consultorio.test", HASH, "Recep", Rol.RECEPCION, CREADO));
        var sesion = SesionRealPrueba.iniciar(mockMvc, usuario.getEmail(), CLAVE);
        mockMvc.perform(get("/api/auth/me").session(sesion)).andExpect(status().isOk());

        usuario.desactivar();
        usuarios.flush();

        mockMvc.perform(get("/api/auth/me").session(sesion))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        usuario.reactivar();
        usuarios.flush();
        mockMvc.perform(get("/api/auth/me").session(sesion)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La sesión de un usuario al que se le cambió el rol deja de ser válida")
    void cambioDeRol() throws Exception {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("c13-sv2@consultorio.test", HASH, "Recep", Rol.RECEPCION, CREADO));
        var sesion = SesionRealPrueba.iniciar(mockMvc, usuario.getEmail(), CLAVE);

        usuario.modificar(usuario.getEmail(), "Recep", Rol.ADMIN, null);
        usuarios.flush();

        mockMvc.perform(get("/api/auth/me").session(sesion)).andExpect(status().isUnauthorized());
        var nueva = SesionRealPrueba.iniciar(mockMvc, usuario.getEmail(), CLAVE);
        mockMvc.perform(get("/api/auth/me").session(nueva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    @DisplayName("La sesión de un usuario sin cambios sigue vigente")
    void sinCambios() throws Exception {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("c13-sv3@consultorio.test", HASH, "Recep", Rol.RECEPCION, CREADO));
        var sesion = SesionRealPrueba.iniciar(mockMvc, usuario.getEmail(), CLAVE);

        usuario.modificar(usuario.getEmail(), "Recepción Renombrada", Rol.RECEPCION, null);
        usuarios.flush();

        mockMvc.perform(get("/api/auth/me").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("c13-sv3@consultorio.test"));
    }

    @Test
    @DisplayName("Un cambio de email también invalida la sesión abierta")
    void cambioDeEmail() throws Exception {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("c13-sv4@consultorio.test", HASH, "Recep", Rol.RECEPCION, CREADO));
        var sesion = SesionRealPrueba.iniciar(mockMvc, usuario.getEmail(), CLAVE);

        usuario.modificar("c13-sv4-nuevo@consultorio.test", "Recep", Rol.RECEPCION, null);
        usuarios.flush();

        mockMvc.perform(get("/api/auth/me").session(sesion)).andExpect(status().isUnauthorized());
    }
}
