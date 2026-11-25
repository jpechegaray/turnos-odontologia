import { ErrorApi } from "../../../shared/api/errorApi";
import { MENSAJE_GENERICO, MENSAJE_PERMISO } from "../mensajes";
import { MENSAJE_NO_ENCONTRADO } from "../profesionales/errores";
import type { ErrorTraducido } from "../useListadoPanel";

export const MENSAJE_BLOQUEO_NO_ENCONTRADO = "El bloqueo ya no existe.";
export const MENSAJE_INTERVALO_RECHAZADO = "No se puede crear el bloqueo en ese intervalo.";

/**
 * Traduce el error de una escritura de bloqueo; nunca expone el `detalle` del servidor. Un `409` o
 * `422` (p. ej. turnos superpuestos, RN-17, cuando lo valide el back) conserva lo ingresado.
 */
export function errorDeBloqueo(error: unknown): ErrorTraducido {
  if (error instanceof ErrorApi) {
    if (error.codigo === "BLOQUEO_NO_ENCONTRADO") return { mensaje: MENSAJE_BLOQUEO_NO_ENCONTRADO, reaccion: "recargar" };
    if (error.codigo === "PROFESIONAL_NO_ENCONTRADO") return { mensaje: MENSAJE_NO_ENCONTRADO, reaccion: "refrescarLista" };
    if (error.estado === 409 || error.estado === 422) return { mensaje: MENSAJE_INTERVALO_RECHAZADO, reaccion: "ninguna" };
    if (error.estado === 403) return { mensaje: MENSAJE_PERMISO, reaccion: "ninguna" };
  }
  return { mensaje: MENSAJE_GENERICO, reaccion: "ninguna" };
}
