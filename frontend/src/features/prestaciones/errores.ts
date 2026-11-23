import { ErrorApi } from "../../shared/api/errorApi";
import { MENSAJE_DURACION_RN09 } from "./validacion";

export const MENSAJE_DUPLICADA = "Ya existe una prestación con ese nombre.";
export const MENSAJE_NO_ENCONTRADA = "La prestación ya no existe.";
export const MENSAJE_PERMISO = "No tenés permiso para realizar esta acción.";
export const MENSAJE_GENERICO = "No pudimos completar la operación. Intentá de nuevo.";

/** Traduce `ErrorApi` por código de negocio; nunca expone el `detalle` del servidor. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof ErrorApi) {
    if (error.codigo === "RN-09") return MENSAJE_DURACION_RN09;
    if (error.codigo === "PRESTACION-DUPLICADA") return MENSAJE_DUPLICADA;
    if (error.codigo === "PRESTACION-NO-ENCONTRADA") return MENSAJE_NO_ENCONTRADA;
    if (error.estado === 403) return MENSAJE_PERMISO;
  }
  return MENSAJE_GENERICO;
}
