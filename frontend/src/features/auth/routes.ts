import type { RutaFeature } from "../../app/rutas/tipos";
import { PaginaLogin } from "./PaginaLogin";

export const rutas: RutaFeature[] = [{ ruta: "/login", componente: PaginaLogin, ambito: "ingreso" }];
