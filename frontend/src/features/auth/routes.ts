import type { RutaFeature } from "../../app/rutas/tipos";
import { PaginaLogin } from "./PaginaLogin";
import { rutas as rutasUsuarios } from "./usuarios/routes";

export const rutas: RutaFeature[] = [
  { ruta: "/login", componente: PaginaLogin, ambito: "ingreso" },
  ...rutasUsuarios,
];
