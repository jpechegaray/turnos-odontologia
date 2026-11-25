import type { UsuarioModificacion, UsuarioRespuesta } from "./tipos";

/** Cuerpo del `PUT`: reemplazo completo con los datos pedidos y el `activo` indicado, sin contraseña. */
export function cuerpoModificacion(
  datos: Pick<UsuarioRespuesta, "email" | "nombre" | "rol" | "profesionalId">,
  activo: boolean,
): UsuarioModificacion {
  return { email: datos.email, nombre: datos.nombre, rol: datos.rol, profesionalId: datos.profesionalId, activo };
}
