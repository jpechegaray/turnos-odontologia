import type { RutaFeature } from "../../../app/rutas/tipos";
import { PaginaUsuarios } from "./PaginaUsuarios";

/** Gestión de cuentas del personal: solo `ADMIN` (US-002, RN-33). La API conserva la autorización efectiva. */
export const rutas: RutaFeature[] = [
  {
    ruta: "/usuarios",
    componente: PaginaUsuarios,
    menu: { etiqueta: "Usuarios", orden: 90 },
    roles: ["ADMIN"],
  },
];
