import type { RutaFeature } from "../../app/rutas/tipos";
import { PaginaListaEspera } from "./PaginaListaEspera";

export const rutas: RutaFeature[] = [
  {
    ruta: "/lista-espera",
    componente: PaginaListaEspera,
    menu: { etiqueta: "Lista de espera", orden: 15, grupo: "atencion", icono: "espera" },
    roles: ["ADMIN", "RECEPCION"],
  },
];
