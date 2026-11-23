import type { RutaFeature } from "../../app/rutas/tipos";
import { PaginaConfiguracion } from "./PaginaConfiguracion";

export const rutas: RutaFeature[] = [
  {
    ruta: "/configuracion",
    componente: PaginaConfiguracion,
    menu: { etiqueta: "Configuración", orden: 90 },
    roles: ["ADMIN", "RECEPCION"],
  },
];
