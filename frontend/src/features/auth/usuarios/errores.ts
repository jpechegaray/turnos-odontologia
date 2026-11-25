import { ErrorApi } from "../../../shared/api/errorApi";
import { MENSAJE_GENERICO, MENSAJE_PERMISO } from "./mensajes";
import type { ErroresUsuario } from "./validacion";

export const MENSAJE_EMAIL_DUPLICADO = "Ya hay un usuario con ese email.";
export const MENSAJE_PROFESIONAL_YA_VINCULADO = "Ese profesional ya está vinculado a otro usuario.";
export const MENSAJE_VINCULO_INVALIDO = "El profesional elegido no es válido para ese rol.";
export const MENSAJE_ULTIMO_ADMIN = "Tiene que quedar al menos un administrador activo.";
export const MENSAJE_USUARIO_NO_ENCONTRADO = "El usuario ya no existe.";
export const MENSAJE_DATOS_INVALIDOS = "Revisá los datos marcados.";
export const MENSAJE_PASSWORD_RECHAZADA = "La contraseña no cumple el largo permitido.";
export const MENSAJE_CAMPO_RECHAZADO = "Revisá este dato.";

/** Códigos tras los cuales la pantalla vuelve a consultar usuarios y profesionales (D5). */
const CODIGOS_CON_RECARGA = new Set([
  "USUARIO_PROFESIONAL_YA_VINCULADO",
  "USUARIO_ULTIMO_ADMIN",
  "USUARIO_NO_ENCONTRADO",
]);

const MENSAJES_POR_CODIGO: Readonly<Record<string, string>> = {
  USUARIO_EMAIL_DUPLICADO: MENSAJE_EMAIL_DUPLICADO,
  USUARIO_PROFESIONAL_YA_VINCULADO: MENSAJE_PROFESIONAL_YA_VINCULADO,
  USUARIO_VINCULO_INVALIDO: MENSAJE_VINCULO_INVALIDO,
  USUARIO_ULTIMO_ADMIN: MENSAJE_ULTIMO_ADMIN,
  USUARIO_NO_ENCONTRADO: MENSAJE_USUARIO_NO_ENCONTRADO,
};

/** Campos del `400` que el formulario sabe marcar; el resto queda en la alerta general. */
const CAMPOS_FORMULARIO: Readonly<Record<string, keyof ErroresUsuario>> = {
  email: "email",
  nombre: "nombre",
  password: "password",
  profesionalId: "profesionalId",
};

export type ErrorUsuarioTraducido = Readonly<{
  mensaje: string;
  /** Errores por campo de un `400`, con mensajes propios: nunca el del servidor ni el valor ingresado. */
  campos: ErroresUsuario;
}>;

/** Traduce `ErrorApi` por código de negocio (D5); nunca expone el `detalle` del servidor. */
export function traducirErrorUsuario(error: unknown): ErrorUsuarioTraducido {
  if (!(error instanceof ErrorApi)) return { mensaje: MENSAJE_GENERICO, campos: {} };
  const porCodigo = error.codigo ? MENSAJES_POR_CODIGO[error.codigo] : undefined;
  if (porCodigo) return { mensaje: porCodigo, campos: {} };
  if (error.estado === 403) return { mensaje: MENSAJE_PERMISO, campos: {} };
  if (error.estado === 400) {
    const campos: Partial<Record<keyof ErroresUsuario, string>> = {};
    for (const { campo } of error.campos) {
      const clave = CAMPOS_FORMULARIO[campo];
      if (clave) campos[clave] = clave === "password" ? MENSAJE_PASSWORD_RECHAZADA : MENSAJE_CAMPO_RECHAZADO;
    }
    return { mensaje: MENSAJE_DATOS_INVALIDOS, campos };
  }
  return { mensaje: MENSAJE_GENERICO, campos: {} };
}

/** Indica si, después de este error, hay que volver a consultar usuarios y profesionales. */
export function requiereRecarga(error: unknown): boolean {
  return error instanceof ErrorApi && error.codigo !== undefined && CODIGOS_CON_RECARGA.has(error.codigo);
}
