import { createElement } from "react";
import type { RutaFeature } from "../../../app/rutas/tipos";
import { PanelPrestaciones } from "../prestaciones/PanelPrestaciones";
import { PaginaProfesionales, type PaginaProfesionalesProps } from "./PaginaProfesionales";

const PANEL_PRESTACIONES: NonNullable<PaginaProfesionalesProps["panel"]> = {
  etiqueta: "Prestaciones",
  Componente: PanelPrestaciones,
};

/** `/profesionales` con el panel de prestaciones conectado a la acción de fila «Prestaciones». */
function PantallaProfesionales() {
  return createElement<PaginaProfesionalesProps>(PaginaProfesionales, { panel: PANEL_PRESTACIONES });
}

export const rutas: RutaFeature[] = [
  {
    ruta: "/profesionales",
    componente: PantallaProfesionales,
    menu: { etiqueta: "Profesionales", orden: 30 },
    roles: ["ADMIN", "RECEPCION", "ODONTOLOGO"],
  },
];
