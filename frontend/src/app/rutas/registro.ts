import type { EntradaMenuResuelta, ModuloRutas, RegistroRutas, RutaFeature } from "./tipos";

/**
 * Junta las rutas de todos los `routes.ts` encontrados por `import.meta.glob` (clave = archivo).
 * Falla con un error explícito ante una ruta repetida o un módulo sin `rutas`.
 */
export function construirRegistro(modulos: Readonly<Record<string, ModuloRutas>>): RegistroRutas {
  const porRuta = new Map<string, { ruta: RutaFeature; archivo: string }>();

  for (const [archivo, modulo] of Object.entries(modulos).sort(([a], [b]) => a.localeCompare(b))) {
    if (!Array.isArray(modulo?.rutas)) {
      throw new Error(`${archivo} debe exportar "rutas" como un arreglo de RutaFeature`);
    }
    for (const ruta of modulo.rutas) {
      const previa = porRuta.get(ruta.ruta);
      if (previa) {
        throw new Error(`La ruta "${ruta.ruta}" está declarada en ${previa.archivo} y en ${archivo}`);
      }
      porRuta.set(ruta.ruta, { ruta, archivo });
    }
  }

  const rutas = [...porRuta.values()].map(({ ruta }) => ruta);
  const menu: EntradaMenuResuelta[] = rutas
    .filter((r): r is RutaFeature & { menu: NonNullable<RutaFeature["menu"]> } => r.menu !== undefined)
    .map((r) => ({ ruta: r.ruta, ...r.menu }))
    .sort((a, b) => a.orden - b.orden || a.etiqueta.localeCompare(b.etiqueta, "es"));

  return { rutas, menu };
}
