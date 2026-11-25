import type { CampoPaciente, PacienteSolicitud } from "./tipos";

export const MENSAJE_NOMBRE_OBLIGATORIO = "El nombre es obligatorio.";
export const MENSAJE_APELLIDO_OBLIGATORIO = "El apellido es obligatorio.";
export const MENSAJE_CONTACTO_OBLIGATORIO = "Ingresá al menos un medio de contacto: email o teléfono.";

/** Lo que escribe la persona en el formulario: siempre texto, sin recortar. */
export type ValoresPaciente = Readonly<Record<CampoPaciente, string>>;

export type ErroresPaciente = Readonly<{
  nombre?: string;
  apellido?: string;
  contacto?: string;
}>;

/** RN-32: nombre y apellido no vacíos y al menos un medio de contacto (email o teléfono). */
export function validarPaciente(valores: ValoresPaciente): ErroresPaciente {
  return {
    nombre: valores.nombre.trim() === "" ? MENSAJE_NOMBRE_OBLIGATORIO : undefined,
    apellido: valores.apellido.trim() === "" ? MENSAJE_APELLIDO_OBLIGATORIO : undefined,
    contacto:
      valores.email.trim() === "" && valores.telefono.trim() === "" ? MENSAJE_CONTACTO_OBLIGATORIO : undefined,
  };
}

function textoOpcional(valor: string): string | null {
  const recortado = valor.trim();
  return recortado === "" ? null : recortado;
}

/** Recorta los ocho campos y convierte los opcionales vacíos en `null`, como el backend. */
export function normalizarPaciente(valores: ValoresPaciente): PacienteSolicitud {
  return {
    nombre: valores.nombre.trim(),
    apellido: valores.apellido.trim(),
    dni: textoOpcional(valores.dni),
    email: textoOpcional(valores.email),
    telefono: textoOpcional(valores.telefono),
    obraSocial: textoOpcional(valores.obraSocial),
    nroAfiliado: textoOpcional(valores.nroAfiliado),
    notas: textoOpcional(valores.notas),
  };
}
