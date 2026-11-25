package ar.edu.utn.turnos.listaespera.infrastructure;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Filas ficticias para las pruebas de lista de espera (regla dura 1: nunca datos reales). Nombres y
 * emails llevan un sufijo único, así las pruebas que confirman transacciones no chocan entre sí.
 */
public final class DatosFicticios {

    private DatosFicticios() {
    }

    /** Paciente activo; con {@code email} nulo solo tiene teléfono. */
    public static Long paciente(JdbcTemplate jdbc, String email) {
        return paciente(jdbc, email, true);
    }

    public static Long paciente(JdbcTemplate jdbc, String email, boolean activo) {
        return jdbc.queryForObject("""
                insert into pacientes (nombre, apellido, email, telefono, activo, creado_en)
                values ('Ana', 'Ficticia', ?, '1100000000', ?, now()) returning id
                """, Long.class, email == null ? null : unico() + "-" + email, activo);
    }

    public static Long prestacion(JdbcTemplate jdbc, String nombre) {
        return prestacion(jdbc, nombre, true);
    }

    public static Long prestacion(JdbcTemplate jdbc, String nombre, boolean activa) {
        var nombreUnico = nombre + " " + unico();
        return jdbc.queryForObject("""
                insert into prestacion (nombre, nombre_clave, duracion_default_min, activa)
                values (?, lower(?), 30, ?) returning id
                """, Long.class, nombreUnico, nombreUnico, activa);
    }

    public static Long profesional(JdbcTemplate jdbc, String matricula, boolean activo) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Luis', 'Ficticio', ?, ?) returning id
                """, Long.class, matricula + "-" + unico(), activo);
    }

    private static String unico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
