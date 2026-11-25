export const MENSAJE_NOMBRE_OBLIGATORIO = "El nombre es obligatorio.";

export type ErroresSillon = Readonly<{ nombre?: string }>;

/** Nombre no blanco después de `trim`; la descripción es opcional. */
export function validarSillon(nombre: string): ErroresSillon {
  return { nombre: nombre.trim() === "" ? MENSAJE_NOMBRE_OBLIGATORIO : undefined };
}

/** Recorta los textos y convierte la descripción vacía en `null`. */
export function normalizarSillon(nombre: string, descripcion: string) {
  const recortada = descripcion.trim();
  return { nombre: nombre.trim(), descripcion: recortada === "" ? null : recortada };
}
