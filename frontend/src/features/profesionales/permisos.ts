import { useSesion } from "../auth/sesion";

/** Solo `ADMIN` ve y usa los controles de escritura de profesionales, sillones y prestaciones asociadas. */
export function useEsAdministrador(): boolean {
  const { estado } = useSesion();
  return estado.tipo === "activa" && estado.identidad.rol === "ADMIN";
}
