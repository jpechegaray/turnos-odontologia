import { ErrorApi } from "../../shared/api/errorApi";
import { MENSAJE_RN36 } from "./validacion";

export const MENSAJE_OPERACION = "No pudimos guardar la configuración. Revisá los datos e intentá de nuevo.";
export const MENSAJE_PERMISO = "No tenés permiso para realizar esta acción.";
export const MENSAJE_GENERICO = "No pudimos completar la operación. Intentá de nuevo.";

/** Traduce `ErrorApi` por `estado`/`codigo`; nunca expone el `detalle` del servidor. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof ErrorApi) {
    if (error.estado === 422 && error.codigo === "RN-36") return MENSAJE_RN36;
    if (error.estado === 400) return MENSAJE_OPERACION;
    if (error.estado === 403) return MENSAJE_PERMISO;
  }
  return MENSAJE_GENERICO;
}
