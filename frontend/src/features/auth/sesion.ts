import { createContext, useContext } from "react";
import type { Rol } from "../../app/rutas/tipos";
import { ErrorApi } from "../../shared/api/errorApi";

export type Identidad = Readonly<{ id: number; email: string; rol: Rol }>;
export type Credenciales = Readonly<{ email: string; password: string }>;
export type EstadoSesion =
  | { tipo: "inicial" | "comprobando" | "error" | "anonima" }
  | { tipo: "activa"; identidad: Identidad };

export const ContextoSesion = createContext<{
  estado: EstadoSesion;
  comprobar: () => void;
  iniciar: (credenciales: Credenciales) => Promise<void>;
  cerrar: () => Promise<void>;
} | null>(null);

export function useSesion() {
  const sesion = useContext(ContextoSesion);
  if (!sesion) throw new Error("Falta el proveedor de sesión");
  return sesion;
}

export function esNoAutenticado(error: unknown): boolean {
  return error instanceof ErrorApi && error.estado === 401;
}
