package ar.edu.utn.turnos.listaespera.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CompatibilidadListaEsperaTest {
    private static final java.time.ZoneId BA = java.time.ZoneId.of("America/Argentina/Buenos_Aires");
    private static final long PROFESIONAL = 10L;
    private static final long OTRO_PROFESIONAL = 20L;
    private static final PrestacionParaHueco CONSULTA = new PrestacionParaHueco(30, true);
    private static final PrestacionParaHueco CONDUCTO = new PrestacionParaHueco(90, true);
    private static final Instant CREADA = Instant.parse("2030-06-01T12:00:00Z");

    /** Hueco que empieza a la hora de pared indicada en Buenos Aires. */
    private static Hueco hueco(long profesional, int anio, int mes, int dia, int hora, int minuto, int minutos) {
        Instant inicio = ZonedDateTime.of(anio, mes, dia, hora, minuto, 0, 0, BA).toInstant();
        return new Hueco(profesional, inicio, inicio.plus(Duration.ofMinutes(minutos)));
    }

    private static Hueco huecoDeTreinta() {
        return hueco(PROFESIONAL, 2030, 6, 12, 10, 0, 30);
    }

    private static EntradaListaEspera entrada(Long preferido, LocalDate desde, LocalDate hasta,
            LocalTime franjaDesde, LocalTime franjaHasta) {
        return EntradaListaEspera.nueva(1L, 5L, preferido, desde, hasta, franjaDesde, franjaHasta, CREADA);
    }

    private static EntradaListaEspera entradaSimple() {
        return entrada(null, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 30), null, null);
    }

    @Test
    @DisplayName("Una prestación de 90 minutos no cabe en un hueco de 30")
    void noventaEnTreinta() {
        var hueco = huecoDeTreinta();

        assertThat(CompatibilidadListaEspera.esCompatible(entradaSimple(), hueco, CONDUCTO, true)).isFalse();
        assertThat(CompatibilidadListaEspera.esCompatible(entradaSimple(), hueco, CONSULTA, true)).isTrue();
    }

    @Test
    @DisplayName("Una prestación que dura exactamente el hueco es compatible")
    void duracionExacta() {
        assertThat(CompatibilidadListaEspera.esCompatible(entradaSimple(), huecoDeTreinta(), CONSULTA, true))
                .isTrue();
        // Un minuto de más ya no cabe.
        assertThat(CompatibilidadListaEspera.esCompatible(entradaSimple(), huecoDeTreinta(),
                new PrestacionParaHueco(31, true), true)).isFalse();
    }

    @Test
    @DisplayName("El profesional del hueco debe realizar la prestación")
    void debeRealizar() {
        assertThat(CompatibilidadListaEspera.esCompatible(entradaSimple(), huecoDeTreinta(), CONSULTA, false))
                .isFalse();
    }

    @Test
    @DisplayName("Una prestación inactiva no es compatible")
    void prestacionInactiva() {
        assertThat(CompatibilidadListaEspera.esCompatible(entradaSimple(), huecoDeTreinta(),
                new PrestacionParaHueco(30, false), true)).isFalse();
    }

    @Test
    @DisplayName("Se respeta el profesional preferido")
    void profesionalPreferido() {
        var conPreferido = entrada(PROFESIONAL, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 30), null, null);

        assertThat(CompatibilidadListaEspera.esCompatible(conPreferido, huecoDeTreinta(), CONSULTA, true)).isTrue();
        assertThat(CompatibilidadListaEspera.esCompatible(conPreferido,
                hueco(OTRO_PROFESIONAL, 2030, 6, 12, 10, 0, 30), CONSULTA, true)).isFalse();
    }

    @Test
    @DisplayName("Una entrada sin profesional preferido acepta a cualquiera que realice la prestación")
    void sinPreferido() {
        assertThat(CompatibilidadListaEspera.esCompatible(entradaSimple(),
                hueco(OTRO_PROFESIONAL, 2030, 6, 12, 10, 0, 30), CONSULTA, true)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoListaEspera.class, names = {"ASIGNADA", "CANCELADA", "VENCIDA"})
    @DisplayName("Una entrada que no está activa no es compatible")
    void noActiva(EstadoListaEspera estado) {
        var activa = entradaSimple();
        var otra = switch (estado) {
            case ASIGNADA -> activa.asignar(99L);
            case CANCELADA -> activa.cancelar();
            default -> activa.vencer();
        };

        assertThat(CompatibilidadListaEspera.esCompatible(otra, huecoDeTreinta(), CONSULTA, true)).isFalse();
    }

    @Test
    @DisplayName("La fecha del hueco debe estar dentro del rango de la entrada")
    void rangoDeFechas() {
        var entrada = entrada(null, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 20), null, null);

        assertThat(CompatibilidadListaEspera.esCompatible(entrada, hueco(PROFESIONAL, 2030, 6, 9, 10, 0, 30),
                CONSULTA, true)).isFalse();
        assertThat(CompatibilidadListaEspera.esCompatible(entrada, hueco(PROFESIONAL, 2030, 6, 10, 10, 0, 30),
                CONSULTA, true)).isTrue();
        assertThat(CompatibilidadListaEspera.esCompatible(entrada, hueco(PROFESIONAL, 2030, 6, 20, 10, 0, 30),
                CONSULTA, true)).isTrue();
        assertThat(CompatibilidadListaEspera.esCompatible(entrada, hueco(PROFESIONAL, 2030, 6, 21, 10, 0, 30),
                CONSULTA, true)).isFalse();
    }

    @Test
    @DisplayName("La fecha se toma en hora de Buenos Aires")
    void fechaEnBuenosAires() {
        var entrada = entrada(null, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 12), null, null);
        var hueco = hueco(PROFESIONAL, 2030, 6, 12, 22, 0, 30);
        // En UTC ya es el 13: si se tomara la fecha UTC, quedaría fuera del rango.
        assertThat(hueco.inicio()).isEqualTo(Instant.parse("2030-06-13T01:00:00Z"));

        assertThat(CompatibilidadListaEspera.esCompatible(entrada, hueco, CONSULTA, true)).isTrue();
    }

    @Test
    @DisplayName("El turno resultante debe caer dentro de la franja")
    void franja() {
        var entrada = entrada(null, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 30),
                LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThat(CompatibilidadListaEspera.esCompatible(entrada, hueco(PROFESIONAL, 2030, 6, 12, 8, 30, 60),
                CONSULTA, true)).isFalse();
        assertThat(CompatibilidadListaEspera.esCompatible(entrada, hueco(PROFESIONAL, 2030, 6, 12, 9, 0, 60),
                CONSULTA, true)).isTrue();
        assertThat(CompatibilidadListaEspera.esCompatible(entrada, hueco(PROFESIONAL, 2030, 6, 12, 11, 30, 60),
                CONSULTA, true)).isTrue();
        assertThat(CompatibilidadListaEspera.esCompatible(entrada, hueco(PROFESIONAL, 2030, 6, 12, 11, 45, 60),
                CONSULTA, true)).isFalse();
    }

    @Test
    @DisplayName("Una entrada sin franja acepta cualquier hora")
    void sinFranja() {
        assertThat(CompatibilidadListaEspera.esCompatible(entradaSimple(), hueco(PROFESIONAL, 2030, 6, 12, 19, 0, 30),
                CONSULTA, true)).isTrue();
    }

    @Test
    @DisplayName("Un turno que cruza la medianoche no cae en ninguna franja")
    void cruzaMedianoche() {
        var entrada = entrada(null, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 30),
                LocalTime.of(20, 0), LocalTime.of(23, 59));

        assertThat(CompatibilidadListaEspera.esCompatible(entrada, hueco(PROFESIONAL, 2030, 6, 12, 23, 45, 60),
                CONSULTA, true)).isFalse();
    }
}
