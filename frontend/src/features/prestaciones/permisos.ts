import { useSesion } from "../auth/sesion";

/** Solo `ADMIN` ve y usa los controles de escritura del catálogo. */
export function useEsAdministrador(): boolean {
  const { estado } = useSesion();
  return estado.tipo === "activa" && estado.identidad.rol === "ADMIN";
}
