package ar.edu.utn.turnos.listaespera.infrastructure;

import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

@Entity
@Table(name = "lista_espera", indexes = {
        @Index(name = "ix_lista_espera_estado_prestacion", columnList = "estado, prestacion_id"),
        @Index(name = "ix_lista_espera_paciente", columnList = "paciente_id")})
class ListaEsperaJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "prestacion_id", nullable = false)
    private Long prestacionId;

    @Column(name = "profesional_id")
    private Long profesionalId;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    @Check(name = "ck_lista_espera_fechas", constraints = "fecha_hasta >= fecha_desde")
    private LocalDate fechaHasta;

    // hibernate.jdbc.time_zone=UTC desplazaría la hora de pared; LocalTimeJdbcType la envía sin convertir.
    @Column(name = "franja_desde")
    @JdbcType(LocalTimeJdbcType.class)
    private LocalTime franjaDesde;

    @Column(name = "franja_hasta")
    @JdbcType(LocalTimeJdbcType.class)
    @Check(name = "ck_lista_espera_franja", constraints = """
            (franja_desde is null and franja_hasta is null)
            or (franja_desde is not null and franja_hasta is not null and franja_hasta > franja_desde)""")
    private LocalTime franjaHasta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoListaEspera estado;

    @Column(name = "turno_asignado_id")
    private Long turnoAsignadoId;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected ListaEsperaJpa() {
    }

    static ListaEsperaJpa de(EntradaListaEspera entrada) {
        var fila = new ListaEsperaJpa();
        fila.id = entrada.id();
        fila.pacienteId = entrada.pacienteId();
        fila.prestacionId = entrada.prestacionId();
        fila.profesionalId = entrada.profesionalId();
        fila.fechaDesde = entrada.fechaDesde();
        fila.fechaHasta = entrada.fechaHasta();
        fila.franjaDesde = entrada.franjaDesde();
        fila.franjaHasta = entrada.franjaHasta();
        fila.estado = entrada.estado();
        fila.turnoAsignadoId = entrada.turnoAsignadoId();
        fila.creadoEn = entrada.creadoEn();
        return fila;
    }

    EntradaListaEspera dominio() {
        return new EntradaListaEspera(id, pacienteId, prestacionId, profesionalId, fechaDesde, fechaHasta,
                franjaDesde, franjaHasta, estado, turnoAsignadoId, creadoEn);
    }
}
