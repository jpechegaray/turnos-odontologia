package ar.edu.utn.turnos.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.configuracion.infrastructure.InicializadorConfiguracion;
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
                        "consultorio.nombre", "Consultorio Ficticio"));
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

        assertThat(jdbc.queryForObject("select count(*) from configuracion", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("select valor from configuracion where clave = ?", String.class, recordatorio))
                .isEqualTo("24");
        assertThat(jdbc.queryForObject("select valor from configuracion where clave = ?", String.class, cancelacion))
                .isEqualTo("48");
        OffsetDateTime fechaPersistida = jdbc.queryForObject("select actualizado_en from configuracion where clave = ?",
                (rs, fila) -> rs.getObject(1, OffsetDateTime.class), cancelacion);
        assertThat(fechaPersistida).isEqualTo(fecha);
    }
}
