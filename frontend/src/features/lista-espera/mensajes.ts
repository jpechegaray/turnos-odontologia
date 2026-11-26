import { ErrorApi } from "../../shared/api/errorApi";
import { mensajeDelTurno } from "../agenda/crear/errores";
import type { CampoAlta, ErroresAlta } from "./validacion";

const MENSAJES_ALTA: Readonly<Record<string, string>> = {
  ENTRADA_ACTIVA_DUPLICADA: "El paciente ya está anotado para esa prestación.",
  PACIENTE_SIN_EMAIL: "El paciente necesita un email para recibir el aviso del hueco. Cargalo en su ficha.",
  PACIENTE_INACTIVO: "El paciente está dado de baja. Buscá otro paciente.",
  PACIENTE_INEXISTENTE: "El paciente ya no está disponible. Buscá otro paciente.",
  PRESTACION_INACTIVA: "La prestación está inactiva. Elegí otra.",
  PRESTACION_INEXISTENTE: "La prestación ya no está disponible. Elegí otra.",
  PROFESIONAL_INACTIVO: "El profesional está inactivo. Elegí otro o dejá «Cualquiera».",
  PROFESIONAL_INEXISTENTE: "El profesional ya no está disponible. Elegí otro o dejá «Cualquiera».",
};

/** Campos del 400 y validaciones de clase del backend (`isFechasValidas`, `isFranjaValida`) por campo visible. */
const CAMPOS_SERVIDOR: Readonly<Record<string, CampoAlta>> = {
  pacienteId: "pacienteId",
  prestacionId: "prestacionId",
  fechaDesde: "fechaDesde",
  fechaHasta: "fechaHasta",
  franjaDesde: "franjaHasta",
  franjaHasta: "franjaHasta",
  fechasValidas: "fechaHasta",
  franjaValida: "franjaHasta",
};

export const MENSAJE_GENERICO = "No pudimos completar la operación. Intentá de nuevo.";

function tiene(tabla: Readonly<Record<string, string>>, clave: string | undefined): clave is string {
  return clave !== undefined && Object.prototype.hasOwnProperty.call(tabla, clave);
}

export function rechazoDelAlta(error: unknown): { mensaje: string; campos: ErroresAlta } {
  if (!(error instanceof ErrorApi)) return { mensaje: MENSAJE_GENERICO, campos: {} };
  if (error.tipo === "validacion") {
    const campos: ErroresAlta = {};
    for (const { campo, mensaje } of error.campos) {
      const destino = CAMPOS_SERVIDOR[campo];
      if (destino) campos[destino] = mensaje;
    }
    return { mensaje: "Revisá los datos marcados.", campos };
  }
  if (tiene(MENSAJES_ALTA, error.codigo)) return { mensaje: MENSAJES_ALTA[error.codigo], campos: {} };
  if (error.tipo === "red") return { mensaje: error.detalle, campos: {} };
  return { mensaje: MENSAJE_GENERICO, campos: {} };
}

/** Mensaje del rechazo de la asignación y si hay que volver a consultar las sugerencias (409 y 422). */
export function rechazoDeAsignacion(error: unknown): { mensaje: string; recargar: boolean } {
  if (!(error instanceof ErrorApi)) return { mensaje: MENSAJE_GENERICO, recargar: false };
  const recargar = error.tipo === "conflicto" || error.tipo === "no-procesable";
  if (error.codigo === "ENTRADA_NO_ACTIVA") {
    return { mensaje: "La entrada ya no está activa. Actualizamos las sugerencias.", recargar };
  }
  if (error.codigo === "ENTRADA_INCOMPATIBLE") return { mensaje: "La entrada no es compatible con ese hueco.", recargar };
  if (error.codigo === "SIN_SILLON_DISPONIBLE") return { mensaje: "No hay un sillón libre en ese horario.", recargar };
  if (error.tipo === "conflicto") return { mensaje: `${mensajeDelTurno(error)} Actualizamos las sugerencias.`, recargar };
  return { mensaje: mensajeDelTurno(error), recargar };
}
