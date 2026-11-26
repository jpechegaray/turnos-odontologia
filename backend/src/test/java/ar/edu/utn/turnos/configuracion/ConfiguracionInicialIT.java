package ar.edu.utn.turnos.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.configuracion.infrastructure.InicializadorConfiguracion;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class ConfiguracionInicialIT extends PruebaIntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InicializadorConfiguracion inicializador;

    @Autowired
    private EntityManager entidades;

    @Test
    @DisplayName("El primer arranque inicializa los siete valores por defecto")
    void inicializaValoresPorDefecto() {
        var valores = jdbc.query("select clave, valor from configuracion",
                (rs, fila) -> Map.entry(rs.getString("clave"), rs.getString("valor")));

        assertThat(valores.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "cancelacion.plazo_horas", "24",
                        "recordatorio.anticipacion_horas", "24",
                        "reserva_online.horizonte_dias", "60",
                        "reserva_online.anticipacion_minima_horas", "2",
                        "reserva_online.max_turnos_activos_por_email", "3",
                        "lista_espera.max_avisos_por_hueco", "10",
                        "consultorio.nombre", "Consultorio Ficticio",
                        "consultorio.domicilio", "Av. Ficticia 1234, Ciudad Autónoma de Buenos Aires",
                        "consultorio.email_contacto", "contacto@consultorio-ficticio.example",
                        "consultorio.retencion_anios", "2"));
    }

    @Test
    @Transactional
    @DisplayName("Los parámetros nuevos del consultorio tienen su valor por defecto")
    void reponeParametrosNuevosDelConsultorio() {
        var fecha = OffsetDateTime.parse("2030-06-01T12:00:00Z");
        jdbc.update("delete from configuracion where clave in (?, ?, ?)", "consultorio.domicilio",
                "consultorio.email_contacto", "consultorio.retencion_anios");
        jdbc.update("update configuracion set valor = case when clave = ? then ? else (valor::int + 1)::text end, "
                + "actualizado_en = ?", "consultorio.nombre", "Consultorio Editado", fecha);
        var anteriores = jdbc.queryForList("select clave, valor, actualizado_en from configuracion order by clave");

        inicializador.inicializar();
        entidades.flush();

        assertThat(jdbc.queryForList("select clave, valor, actualizado_en from configuracion where clave not in "
                + "(?, ?, ?) order by clave", "consultorio.domicilio", "consultorio.email_contacto",
                "consultorio.retencion_anios")).isEqualTo(anteriores);
        assertThat(anteriores).hasSize(7);
        var nuevos = jdbc.query("select clave, valor from configuracion where clave in (?, ?, ?)",
                (rs, fila) -> Map.entry(rs.getString("clave"), rs.getString("valor")), "consultorio.domicilio",
                "consultorio.email_contacto", "consultorio.retencion_anios");
        assertThat(nuevos.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "consultorio.domicilio", "Av. Ficticia 1234, Ciudad Autónoma de Buenos Aires",
                        "consultorio.email_contacto", "contacto@consultorio-ficticio.example",
                        "consultorio.retencion_anios", "2"));
    }

    @Test
    @Transactional
    @DisplayName("La inicialización repone solo claves faltantes")
    void reponeSoloClavesFaltantes() {
        var fecha = OffsetDateTime.parse("2030-06-01T12:00:00Z");
        var cancelacion = "cancelacion.plazo_horas";
        var recordatorio = "recordatorio.anticipacion_horas";
        jdbc.update("update configuracion set valor = ?, actualizado_en = ? where clave = ?",
                "48", fecha, cancelacion);
        jdbc.update("delete from configuracion where clave = ?", recordatorio);

        inicializador.inicializar();

        assertThat(jdbc.queryForObject("select count(*) from configuracion", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("select valor from configuracion where clave = ?", String.class, recordatorio))
                .isEqualTo("24");
        assertThat(jdbc.queryForObject("select valor from configuracion where clave = ?", String.class, cancelacion))
                .isEqualTo("48");
        OffsetDateTime fechaPersistida = jdbc.queryForObject("select actualizado_en from configuracion where clave = ?",
                (rs, fila) -> rs.getObject(1, OffsetDateTime.class), cancelacion);
        assertThat(fechaPersistida).isEqualTo(fecha);
    }
}
