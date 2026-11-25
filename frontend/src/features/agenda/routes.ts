import type { RutaFeature } from "../../app/rutas/tipos";
import { PaginaAgenda } from "./PaginaAgenda";

export const rutas: RutaFeature[] = [
  {
    ruta: "/agenda",
    componente: PaginaAgenda,
    menu: { etiqueta: "Agenda", orden: 5 },
    // El odontólogo la ve en solo lectura (RN-33); la API conserva la autorización efectiva.
    roles: ["ADMIN", "RECEPCION", "ODONTOLOGO"],
  },
];
