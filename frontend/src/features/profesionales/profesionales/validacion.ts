import type { ProfesionalRespuesta, ProfesionalSolicitud } from "./tipos";

export const MAXIMO_CARACTERES = 255;
export const MENSAJE_NOMBRE_OBLIGATORIO = "El nombre es obligatorio.";
export const MENSAJE_APELLIDO_OBLIGATORIO = "El apellido es obligatorio.";
export const MENSAJE_MATRICULA_OBLIGATORIA = "La matrícula es obligatoria.";
export const MENSAJE_MAXIMO = `Máximo ${MAXIMO_CARACTERES} caracteres.`;

export type ClaveCampoProfesional = "nombre" | "apellido" | "matricula" | "especialidad";
/** Los valores del formulario se guardan como texto, tal cual los escribe la persona. */
export type ValoresProfesional = Readonly<Record<ClaveCampoProfesional, string>>;
export type ErroresProfesional = Readonly<Partial<Record<ClaveCampoProfesional, string>>>;

export const VALORES_VACIOS: ValoresProfesional = { nombre: "", apellido: "", matricula: "", especialidad: "" };

function obligatorio(texto: string, mensaje: string): string | undefined {
  const recortado = texto.trim();
  if (recortado === "") return mensaje;
  return recortado.length > MAXIMO_CARACTERES ? MENSAJE_MAXIMO : undefined;
}

/** Nombre, apellido y matrícula no blancos; todos, incluida la especialidad opcional, hasta 255 caracteres. */
export function validarProfesional(valores: ValoresProfesional): ErroresProfesional {
  return {
    nombre: obligatorio(valores.nombre, MENSAJE_NOMBRE_OBLIGATORIO),
    apellido: obligatorio(valores.apellido, MENSAJE_APELLIDO_OBLIGATORIO),
    matricula: obligatorio(valores.matricula, MENSAJE_MATRICULA_OBLIGATORIA),
    especialidad: valores.especialidad.trim().length > MAXIMO_CARACTERES ? MENSAJE_MAXIMO : undefined,
  };
}

export function hayErrores(errores: ErroresProfesional): boolean {
  return Object.values(errores).some(Boolean);
}

/** Recorta los textos y convierte la especialidad vacía en `null`. */
export function solicitudDesde(valores: ValoresProfesional): ProfesionalSolicitud {
  return {
    nombre: valores.nombre.trim(),
    apellido: valores.apellido.trim(),
    matricula: valores.matricula.trim(),
    especialidad: valores.especialidad.trim() || null,
  };
}

/** Precarga del formulario de edición a partir de la representación actual. */
export function valoresDesde(profesional: ProfesionalRespuesta): ValoresProfesional {
  return {
    nombre: profesional.nombre,
    apellido: profesional.apellido,
    matricula: profesional.matricula,
    especialidad: profesional.especialidad ?? "",
  };
}
