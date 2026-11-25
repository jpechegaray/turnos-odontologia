import type { RutaFeature } from "../../app/rutas/tipos";
import { PaginaNotificaciones } from "./PaginaNotificaciones";

export const rutas: RutaFeature[] = [
  {
    ruta: "/notificaciones",
    componente: PaginaNotificaciones,
    menu: { etiqueta: "Notificaciones", orden: 80 },
    roles: ["ADMIN", "RECEPCION"],
  },
];
