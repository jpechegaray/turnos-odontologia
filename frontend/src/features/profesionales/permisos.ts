import { useSesion } from "../auth/sesion";

/** Solo `ADMIN` ve y usa los controles de escritura de profesionales, sillones y prestaciones asociadas. */
export function useEsAdministrador(): boolean {
  const { estado } = useSesion();
  return estado.tipo === "activa" && estado.identidad.rol === "ADMIN";
}

/** `ADMIN` y `RECEPCION` editan horarios y bloqueos; `ODONTOLOGO` solo los consulta (RN-33). */
export function usePuedeGestionarAgenda(): boolean {
  const { estado } = useSesion();
  return estado.tipo === "activa" && (estado.identidad.rol === "ADMIN" || estado.identidad.rol === "RECEPCION");
}
