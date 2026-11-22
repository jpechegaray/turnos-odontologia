package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.seguridad.application.UsuarioResumen;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/** Reglas del servicio de usuarios contra PostgreSQL real. Datos ficticios, revertidos al terminar cada test. */
@Transactional
@WithMockUser(roles = "ADMIN")
class GestionUsuariosIT extends PruebaIntegracionBase {

    private static final String CLAVE = "clave-fixture-c13";

    @Autowired private GestionUsuarios gestion;
    @Autowired private UsuarioJpaRepository usuarios;
    @Autowired private JdbcTemplate jdbc;

    /** Otras clases de test pueden dejar administradores confirmados: se desactivan solo dentro de esta transacción. */
    @BeforeEach
    void sinOtrosAdministradores() {
        jdbc.update("update usuario set activo = false where rol = 'ADMIN'");
    }

    @Test
    @DisplayName("El servicio da de alta con email normalizado, hash BCrypt y sin vínculo")
    void alta() {
        UsuarioResumen creado = gestion.crear(" C13-Recepcion@Consultorio.Test ", "Recepción Ficticia", Rol.RECEPCION, CLAVE, null);

        assertThat(creado.email()).isEqualTo("c13-recepcion@consultorio.test");
        assertThat(creado.activo()).isTrue();
        assertThat(creado.profesionalId()).isNull();
        String hash = usuarios.findById(creado.id()).orElseThrow().getPasswordHash();
        assertThat(hash).isNotEqualTo(CLAVE);
        assertThat(new BCryptPasswordEncoder().matches(CLAVE, hash)).isTrue();
    }

    @Test
    @DisplayName("El servicio rechaza un email repetido con 409 USUARIO_EMAIL_DUPLICADO")
    void emailRepetido() {
        gestion.crear("c13-odonto1@consultorio.test", "A", Rol.RECEPCION, CLAVE, null);
        UsuarioResumen otro = gestion.crear("c13-otro@consultorio.test", "B", Rol.RECEPCION, CLAVE, null);

        assertRegla(() -> gestion.crear("C13-ODONTO1@consultorio.test", "C", Rol.RECEPCION, CLAVE, null),
                "USUARIO_EMAIL_DUPLICADO", EstadoError.CONFLICTO_409);
        assertRegla(() -> gestion.modificar(otro.id(), "c13-odonto1@consultorio.test", "B", Rol.RECEPCION, null, true),
                "USUARIO_EMAIL_DUPLICADO", EstadoError.CONFLICTO_409);
    }

    @Test
    @DisplayName("El servicio valida el vínculo del odontólogo contra el catálogo de profesionales")
    void vinculo() {
        long activo = profesional("C13-MP-1", true);
        long inactivo = profesional("C13-MP-2", false);

        UsuarioResumen odonto = gestion.crear("c13-odonto@consultorio.test", "Odonto", Rol.ODONTOLOGO, CLAVE, activo);
        assertThat(odonto.profesionalId()).isEqualTo(activo);

        assertRegla(() -> gestion.crear("c13-x@consultorio.test", "X", Rol.ODONTOLOGO, CLAVE, null),
                "USUARIO_VINCULO_INVALIDO", EstadoError.NO_PROCESABLE_422);
        assertRegla(() -> gestion.crear("c13-x@consultorio.test", "X", Rol.RECEPCION, CLAVE, activo),
                "USUARIO_VINCULO_INVALIDO", EstadoError.NO_PROCESABLE_422);
        assertRegla(() -> gestion.crear("c13-x@consultorio.test", "X", Rol.ODONTOLOGO, CLAVE, inactivo),
                "USUARIO_VINCULO_INVALIDO", EstadoError.NO_PROCESABLE_422);
        assertRegla(() -> gestion.crear("c13-x@consultorio.test", "X", Rol.ODONTOLOGO, CLAVE, 999_999L),
                "USUARIO_VINCULO_INVALIDO", EstadoError.NO_PROCESABLE_422);
        assertRegla(() -> gestion.crear("c13-x@consultorio.test", "X", Rol.ODONTOLOGO, CLAVE, activo),
                "USUARIO_PROFESIONAL_YA_VINCULADO", EstadoError.CONFLICTO_409);
    }

    @Test
    @DisplayName("Modificar sin cambiar de profesional no exige que siga activo")
    void conservaVinculoAunqueElProfesionalSeDesactive() {
        long id = profesional("C13-MP-3", true);
        UsuarioResumen odonto = gestion.crear("c13-odonto3@consultorio.test", "Odonto", Rol.ODONTOLOGO, CLAVE, id);
        jdbc.update("update profesional set activo = false where id = ?", id);

        UsuarioResumen modificado = gestion.modificar(odonto.id(), odonto.email(), "Odonto Renombrado", Rol.ODONTOLOGO, id, true);

        assertThat(modificado.nombre()).isEqualTo("Odonto Renombrado");
    }

    @Test
    @DisplayName("El servicio responde 404 USUARIO_NO_ENCONTRADO ante un id inexistente")
    void inexistente() {
        assertRegla(() -> gestion.obtener(999_999L), "USUARIO_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404);
        assertRegla(() -> gestion.modificar(999_999L, "c13-n@consultorio.test", "N", Rol.RECEPCION, null, true),
                "USUARIO_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404);
        assertRegla(() -> gestion.desactivar(999_999L), "USUARIO_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404);
    }

    @Test
    @DisplayName("La desactivación es lógica, idempotente y reversible")
    void desactivacion() {
        UsuarioResumen u = gestion.crear("c13-baja@consultorio.test", "Baja", Rol.RECEPCION, CLAVE, null);

        gestion.desactivar(u.id());
        gestion.desactivar(u.id());
        assertThat(gestion.obtener(u.id()).activo()).isFalse();

        gestion.modificar(u.id(), u.email(), u.nombre(), Rol.RECEPCION, null, true);
        assertThat(gestion.obtener(u.id()).activo()).isTrue();
    }

    @Test
    @DisplayName("No se puede desactivar al último administrador activo")
    void ultimoAdministrador() {
        UsuarioResumen admin = gestion.crear("c13-admin@consultorio.test", "Admin", Rol.ADMIN, CLAVE, null);

        assertRegla(() -> gestion.desactivar(admin.id()), "USUARIO_ULTIMO_ADMIN", EstadoError.CONFLICTO_409);
        assertRegla(() -> gestion.modificar(admin.id(), admin.email(), "Admin", Rol.RECEPCION, null, true),
                "USUARIO_ULTIMO_ADMIN", EstadoError.CONFLICTO_409);
        assertRegla(() -> gestion.modificar(admin.id(), admin.email(), "Admin", Rol.ADMIN, null, false),
                "USUARIO_ULTIMO_ADMIN", EstadoError.CONFLICTO_409);
        assertThat(gestion.obtener(admin.id())).extracting(UsuarioResumen::rol, UsuarioResumen::activo)
                .containsExactly(Rol.ADMIN, true);

        gestion.crear("c13-admin2@consultorio.test", "Admin 2", Rol.ADMIN, CLAVE, null);
        gestion.desactivar(admin.id());
        assertThat(gestion.obtener(admin.id()).activo()).isFalse();
    }

    @Test
    @DisplayName("El listado se ordena por email")
    void listadoOrdenado() {
        gestion.crear("c13-zz@consultorio.test", "Z", Rol.RECEPCION, CLAVE, null);
        gestion.crear("c13-aa@consultorio.test", "A", Rol.RECEPCION, CLAVE, null);

        assertThat(gestion.listar()).extracting(UsuarioResumen::email)
                .containsSubsequence("c13-aa@consultorio.test", "c13-zz@consultorio.test");
    }

    @Test
    @WithMockUser(roles = "RECEPCION")
    @DisplayName("Solo ADMIN usa el servicio de usuarios, incluso para leer")
    void soloAdmin() {
        assertThatThrownBy(() -> gestion.listar()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> gestion.crear("c13-r@consultorio.test", "R", Rol.RECEPCION, CLAVE, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private long profesional(String matricula, boolean activo) {
        return jdbc.queryForObject("insert into profesional (nombre, apellido, matricula, activo) values (?, ?, ?, ?) returning id",
                Long.class, "Profesional", "Ficticio", matricula, activo);
    }

    private static void assertRegla(ThrowingCallable accion, String codigo, EstadoError estado) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
            assertThat(ex.codigo()).isEqualTo(codigo);
            assertThat(ex.estado()).isEqualTo(estado);
        });
    }
}
