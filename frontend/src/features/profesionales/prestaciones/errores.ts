import { ErrorApi } from "../../../shared/api/errorApi";
import { MENSAJE_GENERICO, MENSAJE_PERMISO } from "../mensajes";
import { MENSAJE_NO_ENCONTRADO } from "../profesionales/errores";

export const MENSAJE_PRESTACION_INEXISTENTE = "Alguna de las prestaciones elegidas ya no existe.";
export const MENSAJE_PRESTACION_INACTIVA = "No se puede asociar una prestación dada de baja.";
export const MENSAJE_CONFLICTO = "Otra persona modificó estas prestaciones. Volvé a intentarlo.";

/** Qué hace el panel después de mostrar el mensaje. */
export type ReaccionError =
  /** Volver a consultar el catálogo y las asociadas, conservando la selección (`422` y `409`). */
  | "recargar"
  /** El profesional ya no existe: la página refresca su lista (`404`). */
  | "refrescarLista"
  | "ninguna";

export type ErrorReemplazo = Readonly<{ mensaje: string; reaccion: ReaccionError }>;

/** Traduce el error del `PUT` por código de negocio; nunca expone el `detalle` del servidor. */
export function errorDeReemplazo(error: unknown): ErrorReemplazo {
  if (error instanceof ErrorApi) {
    switch (error.codigo) {
      case "PRESTACION_INEXISTENTE":
        return { mensaje: MENSAJE_PRESTACION_INEXISTENTE, reaccion: "recargar" };
      case "PRESTACION_INACTIVA":
        return { mensaje: MENSAJE_PRESTACION_INACTIVA, reaccion: "recargar" };
      case "PROFESIONAL_PRESTACIONES_CONFLICTO":
        return { mensaje: MENSAJE_CONFLICTO, reaccion: "recargar" };
      case "PROFESIONAL_NO_ENCONTRADO":
        return { mensaje: MENSAJE_NO_ENCONTRADO, reaccion: "refrescarLista" };
    }
    if (error.estado === 403) return { mensaje: MENSAJE_PERMISO, reaccion: "ninguna" };
  }
  return { mensaje: MENSAJE_GENERICO, reaccion: "ninguna" };
}
