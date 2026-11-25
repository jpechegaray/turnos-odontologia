import type { PrestacionAsociable } from "./tipos";

function porNombre(a: PrestacionAsociable, b: PrestacionAsociable): number {
  return a.nombre.localeCompare(b.nombre, "es", { sensitivity: "base" }) || a.id - b.id;
}

/**
 * Casillas que ve el administrador: las prestaciones activas del catálogo más las ya asociadas,
 * aunque estén inactivas (se pueden conservar o quitar). Las inactivas no asociadas no se ofrecen.
 */
export function construirOpciones(
  catalogo: readonly PrestacionAsociable[],
  asociadas: readonly PrestacionAsociable[],
): readonly PrestacionAsociable[] {
  const idsAsociadas = new Set(asociadas.map((p) => p.id));
  const opciones = new Map<number, PrestacionAsociable>();
  for (const p of catalogo) {
    if (p.activa || idsAsociadas.has(p.id)) opciones.set(p.id, p);
  }
  for (const p of asociadas) {
    if (!opciones.has(p.id)) opciones.set(p.id, p);
  }
  return [...opciones.values()].sort(porNombre);
}

/** Selección inicial: las prestaciones asociadas hoy. */
export function seleccionInicial(asociadas: readonly PrestacionAsociable[]): ReadonlySet<number> {
  return new Set(asociadas.map((p) => p.id));
}

/** Ids a enviar en el reemplazo: los seleccionados entre las opciones, una sola vez y en su orden. */
export function idsSeleccionados(
  opciones: readonly PrestacionAsociable[],
  seleccion: ReadonlySet<number>,
): number[] {
  return [...new Set(opciones.filter((p) => seleccion.has(p.id)).map((p) => p.id))];
}
