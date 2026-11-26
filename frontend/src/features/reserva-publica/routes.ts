import { lazy } from "react";
import type { RutaFeature } from "../../app/rutas/tipos";

const PaginaReserva = lazy(() => import("./PaginaReserva"));
const PaginaTurnoToken = lazy(() => import("./turno-token/PaginaTurnoToken"));
const PaginaOferta = lazy(() => import("./oferta/PaginaOferta"));

export const rutas: RutaFeature[] = [
  { ruta: "/reservar", ambito: "publico", componente: PaginaReserva },
  { ruta: "/turno/:token", ambito: "publico", componente: PaginaTurnoToken },
  { ruta: "/oferta/:token", ambito: "publico", componente: PaginaOferta },
];
