import { ErrorApi } from "../../../shared/api/errorApi";
import { MENSAJE_GENERICO, MENSAJE_PERMISO } from "../mensajes";

export const MENSAJE_MATRICULA_DUPLICADA = "Ya existe un profesional con esa matrícula.";
export const MENSAJE_NO_ENCONTRADO = "El profesional ya no existe.";

/** Traduce `ErrorApi` por código de negocio; nunca expone el `detalle` del servidor. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof ErrorApi) {
    if (error.codigo === "PROFESIONAL_MATRICULA_DUPLICADA") return MENSAJE_MATRICULA_DUPLICADA;
    if (error.codigo === "PROFESIONAL_NO_ENCONTRADO") return MENSAJE_NO_ENCONTRADO;
    if (error.estado === 403) return MENSAJE_PERMISO;
  }
  return MENSAJE_GENERICO;
}
