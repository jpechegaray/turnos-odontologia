import type { RutaFeature } from "../../app/rutas/tipos";
import { PaginaPacientes } from "./PaginaPacientes";

export const rutas: RutaFeature[] = [
  {
    ruta: "/pacientes",
    componente: PaginaPacientes,
    menu: { etiqueta: "Pacientes", orden: 10, grupo: "atencion", icono: "pacientes" },
    roles: ["ADMIN", "RECEPCION"],
  },
];
