import { ErrorApi } from "../../../shared/api/errorApi";

const MENSAJE_NO_PROCESABLE: Readonly<Record<string, string>> = {
  "RN-09": "Usá una duración de 5 a 480 minutos, en múltiplos de 5, y elegí un horario sugerido.",
  "RN-11": "El profesional no realiza la prestación. Elegí otra prestación u otro profesional.",
  "RN-13": "El horario ya pasó. Elegí un horario futuro.",
  PACIENTE_INACTIVO: "El paciente ya no está activo. Buscá otro paciente.",
  PACIENTE_INEXISTENTE: "El paciente ya no está disponible. Buscá otro paciente.",
  PROFESIONAL_INACTIVO: "El profesional ya no está activo. Elegí otro profesional.",
  PROFESIONAL_INEXISTENTE: "El profesional ya no está disponible. Elegí otro profesional.",
  PRESTACION_INACTIVA: "La prestación ya no está activa. Elegí otra prestación.",
  PRESTACION_INEXISTENTE: "La prestación ya no está disponible. Elegí otra prestación.",
  SILLON_INACTIVO: "El sillón ya no está activo. Elegí otro sillón.",
  SILLON_INEXISTENTE: "El sillón ya no está disponible. Elegí otro sillón.",
  RANGO_DISPONIBILIDAD_INVALIDO: "La fecha no se puede consultar. Elegí otra fecha.",
};

export function mensajeDelTurno(error: unknown): string {
  if (!(error instanceof ErrorApi)) {
    return "No pudimos completar la operación. Intentá de nuevo.";
  }
  if (error.tipo === "conflicto") {
    const recurso = error.propiedades.recurso;
    if (recurso === "PROFESIONAL") return "El profesional está ocupado en ese horario. Elegí otro horario.";
    if (recurso === "SILLON") return "El sillón está ocupado en ese horario. Elegí otro sillón o un nuevo horario.";
    return "El horario ya no está disponible. Elegí otra sugerencia.";
  }
  if (error.tipo === "no-procesable") {
    if (error.codigo === "RN-04") {
      return error.propiedades.motivo === "BLOQUEO"
        ? "El profesional tiene un bloqueo en ese momento. Elegí otro horario."
        : "El turno cae fuera del horario de atención. Elegí otro horario.";
    }
    if (error.codigo && Object.prototype.hasOwnProperty.call(MENSAJE_NO_PROCESABLE, error.codigo)) {
      return MENSAJE_NO_PROCESABLE[error.codigo];
    }
    return "Revisá los datos del turno y volvé a intentar.";
  }
  if (error.tipo === "red") return error.detalle;
  if (error.tipo === "validacion") return "Revisá los datos del turno antes de volver a intentar.";
  if (error.tipo === "prohibido") return "Tu usuario no tiene permiso para realizar esta operación.";
  return "No pudimos completar la operación. Intentá de nuevo.";
}
