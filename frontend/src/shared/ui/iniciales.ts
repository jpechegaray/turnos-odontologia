const TITULOS = /^(dra?|lic)\.?$/i;

/** Iniciales de un nombre: "Ana Ficticia" → "AF"; ignora "Dr." y "Dra.". */
export function iniciales(nombre: string): string {
  const palabras = nombre.trim().split(/\s+/).filter((p) => p && !TITULOS.test(p));
  return palabras.slice(0, 2).map((p) => p.charAt(0).toUpperCase()).join("");
}

/** Clase de color de la paleta de profesionales (--prof-1 a --prof-5) para un id. */
export function claseColor(id: number): string {
  return `avatar--p${(((Math.abs(id) - 1) % 5) + 5) % 5 + 1}`;
}
