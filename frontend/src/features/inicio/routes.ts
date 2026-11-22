import type { RutaFeature } from "../../app/rutas/tipos";
import { PaginaInicio } from "./PaginaInicio";

export const rutas: RutaFeature[] = [
  { ruta: "/", componente: PaginaInicio, menu: { etiqueta: "Inicio", orden: 0 } },
];
