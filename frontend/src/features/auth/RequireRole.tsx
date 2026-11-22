import type { ReactNode } from "react";
import type { Rol } from "../../app/rutas/tipos";
import { Alerta } from "../../shared/ui/Alerta";
import { tieneRol } from "./permisos";
import { useSesion } from "./sesion";

export function RequireRole({ roles, children }: { roles?: readonly Rol[]; children: ReactNode }) {
  const { estado } = useSesion();
  return estado.tipo === "activa" && tieneRol(estado.identidad.rol, roles)
    ? children
    : <Alerta tipo="peligro" titulo="No tenés permiso" />;
}
