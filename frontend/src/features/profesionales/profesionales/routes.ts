import { createElement } from "react";
import type { RutaFeature } from "../../../app/rutas/tipos";
import { PanelBloqueos } from "../bloqueos/PanelBloqueos";
import { PanelHorarios } from "../horarios/PanelHorarios";
import { PanelPrestaciones } from "../prestaciones/PanelPrestaciones";
import { PaginaProfesionales, type PaginaProfesionalesProps, type PanelDefinicion } from "./PaginaProfesionales";

const PANEL_PRESTACIONES: PanelDefinicion = {
  etiqueta: "Prestaciones",
  Componente: PanelPrestaciones,
};

/** Paneles por profesional, en el orden de sus acciones de fila. */
const PANELES: readonly PanelDefinicion[] = [
  PANEL_PRESTACIONES,
  { etiqueta: "Horarios", Componente: PanelHorarios },
  { etiqueta: "Bloqueos", Componente: PanelBloqueos },
];

/** `/profesionales` con los paneles conectados a sus acciones de fila. */
function PantallaProfesionales() {
  return createElement<PaginaProfesionalesProps>(PaginaProfesionales, { paneles: PANELES });
}

export const rutas: RutaFeature[] = [
  {
    ruta: "/profesionales",
    componente: PantallaProfesionales,
    menu: { etiqueta: "Profesionales", orden: 30 },
    roles: ["ADMIN", "RECEPCION", "ODONTOLOGO"],
  },
];
