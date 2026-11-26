import type { RutaFeature } from "../../../app/rutas/tipos";
import { PaginaSillones } from "./PaginaSillones";

export const rutas: RutaFeature[] = [
  {
    ruta: "/sillones",
    componente: PaginaSillones,
    menu: { etiqueta: "Sillones", orden: 40, grupo: "consultorio", icono: "sillones" },
    roles: ["ADMIN", "RECEPCION", "ODONTOLOGO"],
  },
];
