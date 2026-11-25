import type { Rol } from "../../../app/rutas/tipos";

export const MENSAJE_EMAIL_INVALIDO = "Ingresá un email válido.";
export const MENSAJE_EMAIL_LARGO = "El email no puede superar los 254 caracteres.";
export const MENSAJE_NOMBRE_OBLIGATORIO = "El nombre es obligatorio.";
export const MENSAJE_NOMBRE_LARGO = "El nombre no puede superar los 255 caracteres.";

export const MENSAJE_PASSWORD_LARGO = "La contraseña debe tener entre 8 caracteres y 72 bytes.";
export const MENSAJE_PROFESIONAL_OBLIGATORIO = "Elegí el profesional del odontólogo.";

export const MAXIMO_EMAIL = 254;
/** Igual que `ClaveUsuario` del backend: BCrypt solo considera los primeros 72 bytes (RN-34). */
export const MINIMO_CARACTERES_PASSWORD = 8;
export const MAXIMO_BYTES_PASSWORD = 72;
export const MAXIMO_NOMBRE = 255;

/** Algo antes y después de una única `@`, con un punto en el dominio. El backend sigue siendo la autoridad. */
const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export type ErroresUsuario = Readonly<{
  email?: string;
  nombre?: string;
  password?: string;
  profesionalId?: string;
}>;

export type DatosUsuario = Readonly<{
  email: string;
  nombre: string;
  rol: Rol;
  profesionalId: number | null;
  /** Solo en el alta; la edición no cambia la contraseña. */
  password?: string;
}>;

function validarEmail(email: string): string | undefined {
  const recortado = email.trim();
  if (!FORMATO_EMAIL.test(recortado)) return MENSAJE_EMAIL_INVALIDO;
  if (recortado.length > MAXIMO_EMAIL) return MENSAJE_EMAIL_LARGO;
  return undefined;
}

function validarNombre(nombre: string): string | undefined {
  const recortado = nombre.trim();
  if (recortado === "") return MENSAJE_NOMBRE_OBLIGATORIO;
  if (recortado.length > MAXIMO_NOMBRE) return MENSAJE_NOMBRE_LARGO;
  return undefined;
}

/** Cuenta *code points* (no unidades UTF-16) y mide bytes en UTF-8, como el backend. */
export function passwordValida(password: string): boolean {
  return (
    [...password].length >= MINIMO_CARACTERES_PASSWORD &&
    new TextEncoder().encode(password).length <= MAXIMO_BYTES_PASSWORD
  );
}

/** Espejo de `UsuarioAlta`/`UsuarioModificacion`: devuelve un mensaje por campo inválido. */
export function validarUsuario(datos: DatosUsuario): ErroresUsuario {
  return {
    email: validarEmail(datos.email),
    nombre: validarNombre(datos.nombre),
    password: datos.password !== undefined && !passwordValida(datos.password) ? MENSAJE_PASSWORD_LARGO : undefined,
    profesionalId:
      datos.rol === "ODONTOLOGO" && datos.profesionalId === null ? MENSAJE_PROFESIONAL_OBLIGATORIO : undefined,
  };
}

export function tieneErrores(errores: ErroresUsuario): boolean {
  return Object.values(errores).some((mensaje) => mensaje !== undefined);
}
