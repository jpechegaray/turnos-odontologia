package ar.edu.utn.turnos.listaespera.application;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/** El reloj fijo hace que hoy sea 2026-12-01 en Buenos Aires. */
@Import(RelojFijoConfig.class)
class VencimientoListaEsperaIT extends PruebaIntegracionBase {
    @Autowired
    private ServicioListaEspera servicio;

    @Autowired
    private PlanificadorVencimiento planificador;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @Transactional
    @DisplayName("Una entrada con fecha de fin pasada vence en el siguiente ciclo")
    void venceEnElSiguienteCiclo() {
        jdbc.update("update lista_espera set estado = 'CANCELADA' where estado = 'ACTIVA'");
        Long paciente = DatosFicticios.paciente(jdbc, "vence@correo.test");
        Long ayer = guardar(paciente, DatosFicticios.prestacion(jdbc, "Vence ayer"), "ACTIVA", "2026-11-30");
        Long hoy = guardar(paciente, DatosFicticios.prestacion(jdbc, "Vence hoy"), "ACTIVA", "2026-12-01");
        Long cancelada = guardar(paciente, DatosFicticios.prestacion(jdbc, "Ya cancelada"), "CANCELADA", "2026-11-20");

        planificador.ejecutar();

        assertThat(estadoDe(ayer)).isEqualTo("VENCIDA");
        assertThat(estadoDe(hoy)).isEqualTo("ACTIVA");
        assertThat(estadoDe(cancelada)).isEqualTo("CANCELADA");
        // Un segundo ciclo el mismo día no encuentra nada más para vencer.
        assertThat(servicio.vencerEntradas()).isZero();
    }

    @Test
    @DisplayName("El planificador corre una vez por día a las 00:05 de Buenos Aires")
    void cronDelPlanificador() throws Exception {
        Method ejecutar = PlanificadorVencimiento.class.getMethod("ejecutar");
        Scheduled programacion = ejecutar.getAnnotation(Scheduled.class);
        assertThat(programacion.cron()).isEqualTo("${lista-espera.vencimiento.cron:0 5 0 * * *}");
        assertThat(programacion.zone()).isEqualTo("America/Argentina/Buenos_Aires");
    }

    private Long guardar(Long paciente, Long prestacion, String estado, String hasta) {
        return jdbc.queryForObject("""
                insert into lista_espera (paciente_id, prestacion_id, fecha_desde, fecha_hasta, estado, creado_en)
                values (?, ?, date '2026-11-01', cast(? as date), ?, now()) returning id
                """, Long.class, paciente, prestacion, hasta, estado);
    }

    private String estadoDe(Long id) {
        return jdbc.queryForObject("select estado from lista_espera where id = ?", String.class, id);
    }
}
