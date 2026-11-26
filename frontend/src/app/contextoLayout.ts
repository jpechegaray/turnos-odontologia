import { useOutletContext } from "react-router-dom";
import { GRUPOS_MENU, type EntradaMenuResuelta } from "./rutas/tipos";

/** Lo que el layout comparte con la pantalla abierta: el menú ya filtrado por el rol de la sesión. */
export type ContextoLayout = Readonly<{ menu: readonly EntradaMenuResuelta[] }>;

export type GrupoConEntradas = Readonly<{
  id: (typeof GRUPOS_MENU)[number]["id"];
  titulo: string;
  entradas: readonly EntradaMenuResuelta[];
}>;

/** Agrupa las entradas en el orden de GRUPOS_MENU; sin grupo van en Atención y no se devuelven grupos vacíos. */
export function agruparMenu(menu: readonly EntradaMenuResuelta[]): GrupoConEntradas[] {
  return GRUPOS_MENU
    .map((grupo) => ({ ...grupo, entradas: menu.filter((e) => (e.grupo ?? "atencion") === grupo.id) }))
    .filter((grupo) => grupo.entradas.length > 0);
}

/** Fuera del layout (por ejemplo en un test aislado) el menú queda vacío. */
export function useMenuDelLayout(): readonly EntradaMenuResuelta[] {
  return useOutletContext<ContextoLayout | undefined>()?.menu ?? [];
}
