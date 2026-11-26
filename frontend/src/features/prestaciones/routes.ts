import type { RutaFeature } from "../../app/rutas/tipos";
import { PaginaPrestaciones } from "./PaginaPrestaciones";

export const rutas: RutaFeature[] = [
  {
    ruta: "/prestaciones",
    componente: PaginaPrestaciones,
    menu: { etiqueta: "Prestaciones", orden: 20, grupo: "consultorio", icono: "prestaciones" },
    roles: ["ADMIN", "RECEPCION", "ODONTOLOGO"],
  },
];
