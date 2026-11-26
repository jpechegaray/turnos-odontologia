package ar.edu.utn.turnos.listaespera.application;

import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera;
import ar.edu.utn.turnos.listaespera.domain.Hueco;
import ar.edu.utn.turnos.listaespera.infrastructure.ListaEsperaPersistencia;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.notificacion.application.EventosAvisoAlta;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Alta, consulta, baja y vencimiento de la lista de espera (RN-21). */
@Service
public class ServicioListaEspera {
    private static final Logger LOG = LoggerFactory.getLogger(ServicioListaEspera.class);
    private static final String PUEDE_GESTIONAR = "hasAnyRole('ADMIN', 'RECEPCION')";
    private static final String PUEDE_CONSULTAR = "hasAnyRole('ADMIN', 'RECEPCION', 'ODONTOLOGO')";

    private final ListaEsperaPersistencia entradas;
    private final PacienteConsulta pacientes;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalConsulta profesionales;
    private final BuscadorCompatibles buscador;
    private final Clock reloj;
    private final ApplicationEventPublisher eventos;

    public ServicioListaEspera(ListaEsperaPersistencia entradas, PacienteConsulta pacientes,
            ConsultaPrestaciones prestaciones, ProfesionalConsulta profesionales, BuscadorCompatibles buscador,
            Clock reloj, ApplicationEventPublisher eventos) {
        this.entradas = entradas;
        this.pacientes = pacientes;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.buscador = buscador;
        this.reloj = reloj;
        this.eventos = eventos;
    }

    @PreAuthorize(PUEDE_GESTIONAR)
    @Transactional
    public EntradaListaEspera crear(DatosAlta datos) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate desde = datos.fechaDesde() != null ? datos.fechaDesde() : hoy;
        // Depende del reloj, por eso no puede validarse solo en el DTO.
        if (datos.fechaHasta().isBefore(hoy) || datos.fechaHasta().isBefore(desde)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de fin no puede ser anterior a la de inicio ni a hoy");
        }
        verificarPaciente(datos.pacienteId());
        verificarPrestacion(datos.prestacionId());
        if (datos.profesionalId() != null) {
            verificarProfesional(datos.profesionalId());
        }
        if (entradas.existeActiva(datos.pacienteId(), datos.prestacionId())) {
            throw ListaEsperaPersistencia.duplicada();
        }
        // Si otra alta concurrente ganó entre la consulta y el guardado, el índice parcial la detecta.
        var entrada = entradas.guardar(EntradaListaEspera.nueva(datos.pacienteId(), datos.prestacionId(),
                datos.profesionalId(), desde, datos.fechaHasta(), datos.franjaDesde(), datos.franjaHasta(),
                Instant.now(reloj)));
        eventos.publishEvent(new EventosAvisoAlta.EntradaRegistrada(entrada.id()));
        return entrada;
    }

    @PreAuthorize(PUEDE_CONSULTAR)
    @Transactional(readOnly = true)
    public List<EntradaListaEspera> listar(EstadoListaEspera estado, Long pacienteId, Long prestacionId) {
        return entradas.filtrar(estado, pacienteId, prestacionId);
    }

    @PreAuthorize(PUEDE_CONSULTAR)
    @Transactional(readOnly = true)
    public EntradaListaEspera detalle(Long id) {
        return entradas.buscar(id).orElseThrow(ServicioListaEspera::noEncontrada);
    }

    @PreAuthorize(PUEDE_GESTIONAR)
    @Transactional
    public void cancelar(Long id) {
        var entrada = entradas.buscar(id).orElseThrow(ServicioListaEspera::noEncontrada);
        entradas.guardar(entrada.cancelar());
    }

    /**
     * Sugerencias para recepción (RN-22, SU-15): todas las entradas compatibles con el hueco, por antigüedad,
     * sin tope ni deduplicación. Un hueco que ya empezó no tiene sugerencias.
     */
    @PreAuthorize(PUEDE_GESTIONAR)
    @Transactional(readOnly = true)
    public List<EntradaListaEspera> compatibles(Long profesionalId, Instant inicio, Instant fin) {
        if (!fin.isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El fin del hueco debe ser posterior al inicio");
        }
        verificarProfesional(profesionalId);
        if (!Instant.now(reloj).isBefore(inicio)) {
            return List.of();
        }
        return buscador.compatibles(new Hueco(profesionalId, inicio, fin));
    }

    /**
     * Marca VENCIDA toda entrada ACTIVA con fecha de fin anterior a hoy en Buenos Aires (RN-14, RN-21).
     * Lo invoca el planificador, sin usuario autenticado; no registra datos personales.
     */
    @Transactional
    public int vencerEntradas() {
        int vencidas = entradas.vencerAnterioresA(LocalDate.now(reloj));
        LOG.info("Lista de espera: {} entradas vencidas", vencidas);
        return vencidas;
    }

    private void verificarPaciente(Long id) {
        var paciente = pacientes.porId(id).orElseThrow(() -> invalido("PACIENTE_INEXISTENTE",
                "No se encontró el paciente"));
        if (!paciente.activo()) {
            throw invalido("PACIENTE_INACTIVO", "El paciente está dado de baja");
        }
        if (paciente.email() == null || paciente.email().isBlank()) {
            throw invalido("PACIENTE_SIN_EMAIL",
                    "El paciente necesita email para recibir el aviso de un hueco (RN-21)");
        }
    }

    private void verificarPrestacion(Long id) {
        var prestacion = prestaciones.buscarPorId(id).orElseThrow(() -> invalido("PRESTACION_INEXISTENTE",
                "No se encontró la prestación"));
        if (!prestacion.activa()) {
            throw invalido("PRESTACION_INACTIVA", "La prestación está inactiva");
        }
    }

    private void verificarProfesional(Long id) {
        var profesional = profesionales.buscarPorId(id).orElseThrow(() -> invalido("PROFESIONAL_INEXISTENTE",
                "No se encontró el profesional"));
        if (!profesional.activo()) {
            throw invalido("PROFESIONAL_INACTIVO", "El profesional está inactivo");
        }
    }

    private static ReglaNegocioException invalido(String codigo, String mensaje) {
        return new ReglaNegocioException(codigo, EstadoError.NO_PROCESABLE_422, mensaje);
    }

    private static ReglaNegocioException noEncontrada() {
        return new ReglaNegocioException("ENTRADA_NO_ENCONTRADA", EstadoError.NO_ENCONTRADO_404,
                "No se encontró la entrada de la lista de espera");
    }
}
