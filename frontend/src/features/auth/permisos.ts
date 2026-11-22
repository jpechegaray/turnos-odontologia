import type { Rol } from "../../app/rutas/tipos";

export function tieneRol(rol: Rol, permitidos?: readonly Rol[]) {
  return permitidos === undefined || permitidos.includes(rol);
}
