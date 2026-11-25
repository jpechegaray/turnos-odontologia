import { useSesion } from "../auth/sesion";

/** Solo `ADMIN` ve y usa la baja de pacientes; alta y edición las ven `ADMIN` y `RECEPCION`. */
export function useEsAdministrador(): boolean {
  const { estado } = useSesion();
  return estado.tipo === "activa" && estado.identidad.rol === "ADMIN";
}
