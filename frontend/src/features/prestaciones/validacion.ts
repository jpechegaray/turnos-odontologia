export const MENSAJE_DURACION_RN09 = "La duración debe ser un múltiplo de 5 entre 5 y 480 minutos.";
export const MENSAJE_NOMBRE_OBLIGATORIO = "El nombre es obligatorio.";

export type ErroresPrestacion = Readonly<{ nombre?: string; duracion?: string }>;

/**
 * Entero estricto sobre el texto recortado: evita que `Number("2,5")` o `Number("")` se cuelen como
 * valores válidos. Devuelve `undefined` si el texto no es un entero.
 */
export function enteroEstricto(texto: string): number | undefined {
  const recortado = texto.trim();
  return /^-?\d+$/u.test(recortado) ? Number(recortado) : undefined;
}

/** RN-09: nombre no vacío y duración entera, múltiplo de 5, entre 5 y 480 minutos. */
export function validarPrestacion(nombre: string, duracionTexto: string): ErroresPrestacion {
  const duracion = enteroEstricto(duracionTexto);
  return {
    nombre: nombre.trim() === "" ? MENSAJE_NOMBRE_OBLIGATORIO : undefined,
    duracion:
      duracion === undefined || duracion < 5 || duracion > 480 || duracion % 5 !== 0
        ? MENSAJE_DURACION_RN09
        : undefined,
  };
}
