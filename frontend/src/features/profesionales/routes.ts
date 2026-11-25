import type { ModuloRutas, RutaFeature } from "../../app/rutas/tipos";

/**
 * Concatena, ordenadas por archivo, las `rutas` de los `routes.ts` hijos. Falla con un error
 * explícito si un hijo no exporta `rutas` como arreglo. Las rutas repetidas las detecta
 * `construirRegistro` al juntar todas las features.
 */
export function agruparRutas(modulos: Readonly<Record<string, ModuloRutas>>): RutaFeature[] {
  return Object.entries(modulos)
    .sort(([a], [b]) => a.localeCompare(b))
    .flatMap(([archivo, modulo]) => {
      if (!Array.isArray(modulo?.rutas)) {
        throw new Error(`${archivo} debe exportar "rutas" como un arreglo de RutaFeature`);
      }
      return modulo.rutas;
    });
}

/** Cada subcarpeta (profesionales, sillones y, más adelante, horarios o bloqueos) suma su `routes.ts`. */
const modulos = import.meta.glob<ModuloRutas>("./*/routes.ts", { eager: true });

export const rutas: readonly RutaFeature[] = agruparRutas(modulos);
