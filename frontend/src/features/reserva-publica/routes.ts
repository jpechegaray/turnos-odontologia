import { lazy } from "react";
import type { RutaFeature } from "../../app/rutas/tipos";

const PaginaReserva = lazy(() => import("./PaginaReserva"));

export const rutas: RutaFeature[] = [
  { ruta: "/reservar", ambito: "publico", componente: PaginaReserva },
];
