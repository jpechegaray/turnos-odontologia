package ar.edu.utn.turnos.agenda.web;

import jakarta.validation.constraints.Size;

/** Cuerpo opcional de la cancelación (RN-19): el motivo es optativo; 255 es el largo de la columna. */
record SolicitudCancelarTurno(@Size(max = 255) String motivo) {
}
