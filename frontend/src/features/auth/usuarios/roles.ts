import type { Rol } from "../../../app/rutas/tipos";

/** Cómo se nombra cada rol en la pantalla. */
export const ETIQUETAS_ROL: Readonly<Record<Rol, string>> = {
  ADMIN: "Administrador",
  RECEPCION: "Recepción",
  ODONTOLOGO: "Odontólogo",
};
