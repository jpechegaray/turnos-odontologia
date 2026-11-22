import type { RutaFeature } from "../../../tipos";
import { DetalleEjemplo, PaginaEjemplo } from "./PaginaEjemplo";

/** Así declara sus pantallas cualquier feature: sin tocar el router ni el menú central. */
export const rutas: RutaFeature[] = [
  { ruta: "/ejemplo", componente: PaginaEjemplo, menu: { etiqueta: "Ejemplo", orden: 50 } },
  { ruta: "/ejemplo/detalle", componente: DetalleEjemplo },
];
