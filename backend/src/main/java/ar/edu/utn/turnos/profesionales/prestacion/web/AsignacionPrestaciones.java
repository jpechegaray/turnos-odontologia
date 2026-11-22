package ar.edu.utn.turnos.profesionales.prestacion.web;

import jakarta.validation.constraints.NotNull;
import java.util.List;

record AsignacionPrestaciones(@NotNull List<@NotNull Long> prestacionIds) {
}
