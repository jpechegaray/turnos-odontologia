import { ErrorApi } from "../../../shared/api/errorApi";
import { MENSAJE_GENERICO, MENSAJE_PERMISO } from "../mensajes";

export const MENSAJE_SILLON_DUPLICADO = "Ya existe un sillón con ese nombre.";
export const MENSAJE_SILLON_NO_ENCONTRADO = "El sillón ya no existe.";

/** Traduce `ErrorApi` por código de negocio; nunca expone el `detalle` del servidor. */
export function mensajeDeErrorSillon(error: unknown): string {
  if (error instanceof ErrorApi) {
    if (error.codigo === "SILLON_NOMBRE_DUPLICADO") return MENSAJE_SILLON_DUPLICADO;
    if (error.codigo === "SILLON_NO_ENCONTRADO") return MENSAJE_SILLON_NO_ENCONTRADO;
    if (error.estado === 403) return MENSAJE_PERMISO;
  }
  return MENSAJE_GENERICO;
}
