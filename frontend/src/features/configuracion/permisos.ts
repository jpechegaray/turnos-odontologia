import { useSesion } from "../auth/sesion";

/** Solo `ADMIN` edita y guarda la configuración; el resto consulta en solo lectura. */
export function useEsAdministrador(): boolean {
  const { estado } = useSesion();
  return estado.tipo === "activa" && estado.identidad.rol === "ADMIN";
}

/** Condición única de solo lectura de la pantalla: cualquier rol distinto de `ADMIN`. */
export function useSoloLecturaConfiguracion(): boolean {
  return !useEsAdministrador();
}
