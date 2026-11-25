package ar.edu.utn.turnos.agenda;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Filas ficticias para las pruebas de agenda (regla dura 1: nunca datos reales). Los textos llevan un
 * sufijo único para que las pruebas que confirman transacciones no choquen entre sí.
 */
public final class DatosAgenda {

    private DatosAgenda() {
    }

    public static Long paciente(JdbcTemplate jdbc, boolean activo) {
        return jdbc.queryForObject("""
                insert into pacientes (nombre, apellido, email, telefono, activo, creado_en)
                values ('Ana', 'Ficticia', ?, '1100000000', ?, now()) returning id
                """, Long.class, unico() + "@agenda.test", activo);
    }

    public static Long profesional(JdbcTemplate jdbc, boolean activo) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Luis', 'Ficticio', ?, ?) returning id
                """, Long.class, "MP-" + unico(), activo);
    }

    public static Long sillon(JdbcTemplate jdbc, boolean activo) {
        return jdbc.queryForObject("""
                insert into sillon (nombre, descripcion, activo) values (?, 'Sillón ficticio', ?) returning id
                """, Long.class, "Sillón " + unico(), activo);
    }

    public static Long prestacion(JdbcTemplate jdbc, int duracionDefaultMin, boolean activa) {
        var nombre = "Prestación " + unico();
        return jdbc.queryForObject("""
                insert into prestacion (nombre, nombre_clave, duracion_default_min, activa)
                values (?, lower(?), ?, ?) returning id
                """, Long.class, nombre, nombre, duracionDefaultMin, activa);
    }

    /** El profesional realiza la prestación (RN-11). */
    public static void asignarPrestacion(JdbcTemplate jdbc, Long profesionalId, Long prestacionId) {
        jdbc.update("insert into profesional_prestacion (profesional_id, prestacion_id) values (?, ?)",
                profesionalId, prestacionId);
    }

    /** Tramo de atención en hora local de Buenos Aires; {@code dia} ISO (1 = lunes). */
    public static void horario(JdbcTemplate jdbc, Long profesionalId, int dia, String desde, String hasta) {
        jdbc.update("""
                insert into horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin)
                values (?, ?, cast(? as time), cast(? as time))
                """, profesionalId, dia, desde, hasta);
    }

    /** Inserta un turno directamente, sin pasar por la validación de la aplicación. */
    public static Long turno(JdbcTemplate jdbc, Long paciente, Long profesional, Long sillon, Long prestacion,
            String inicioIso, String finIso, String estado) {
        return jdbc.queryForObject("""
                insert into turno (paciente_id, profesional_id, sillon_id, prestacion_id, inicio, fin, duracion_min,
                                   estado, canal, cancelacion_tardia, reprogramado_tarde, creado_en, version)
                values (?, ?, ?, ?, cast(? as timestamptz), cast(? as timestamptz),
                        cast(extract(epoch from (cast(? as timestamptz) - cast(? as timestamptz))) / 60 as int),
                        ?, 'INTERNO', false, false, now(), 0)
                returning id
                """, Long.class, paciente, profesional, sillon, prestacion, inicioIso, finIso, finIso, inicioIso, estado);
    }

    /** Borra los turnos de esos recursos y luego los recursos (las FK obligan a ese orden). */
    public static void limpiar(JdbcTemplate jdbc, Long paciente, Long[] profesionales, Long[] sillones, Long[] prestaciones) {
        for (Long p : profesionales) {
            jdbc.update("delete from turno where profesional_id = ?", p);
        }
        for (Long s : sillones) {
            jdbc.update("delete from turno where sillon_id = ?", s);
        }
        jdbc.update("delete from turno where paciente_id = ?", paciente);
        for (Long p : profesionales) {
            jdbc.update("delete from profesional_prestacion where profesional_id = ?", p);
            jdbc.update("delete from horario_atencion where profesional_id = ?", p);
            jdbc.update("delete from bloqueo where profesional_id = ?", p);
            jdbc.update("delete from profesional where id = ?", p);
        }
        for (Long s : sillones) {
            jdbc.update("delete from sillon where id = ?", s);
        }
        for (Long p : prestaciones) {
            jdbc.update("delete from prestacion where id = ?", p);
        }
        jdbc.update("delete from pacientes where id = ?", paciente);
    }

    private static String unico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
