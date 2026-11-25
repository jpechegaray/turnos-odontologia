import { ErrorApi } from "../../../shared/api/errorApi";
import { MENSAJE_GENERICO, MENSAJE_PERMISO } from "../mensajes";
import { MENSAJE_NO_ENCONTRADO } from "../profesionales/errores";
import type { ErrorTraducido } from "../useListadoPanel";

export const MENSAJE_SUPERPUESTO = "El tramo se superpone con otro del mismo día.";
export const MENSAJE_TRAMO_NO_ENCONTRADO = "El tramo ya no existe.";

/**
 * Traduce el error de una escritura de tramo por código; nunca expone el `detalle` del servidor.
 * `409` y `404` del tramo recargan; `404` del profesional refresca la lista de la página.
 */
export function errorDeTramo(error: unknown): ErrorTraducido {
  if (error instanceof ErrorApi) {
    switch (error.codigo) {
      case "HORARIO_SUPERPUESTO":
        return { mensaje: MENSAJE_SUPERPUESTO, reaccion: "recargar" };
      case "HORARIO_NO_ENCONTRADO":
        return { mensaje: MENSAJE_TRAMO_NO_ENCONTRADO, reaccion: "recargar" };
      case "PROFESIONAL_NO_ENCONTRADO":
        return { mensaje: MENSAJE_NO_ENCONTRADO, reaccion: "refrescarLista" };
    }
    if (error.estado === 403) return { mensaje: MENSAJE_PERMISO, reaccion: "ninguna" };
  }
  return { mensaje: MENSAJE_GENERICO, reaccion: "ninguna" };
}
