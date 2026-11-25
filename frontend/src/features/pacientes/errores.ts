import { ErrorApi } from "../../shared/api/errorApi";
import type { CampoPaciente } from "./tipos";
import { MENSAJE_CONTACTO_OBLIGATORIO } from "./validacion";

export const MENSAJE_DNI_DUPLICADO = "Ya existe un paciente activo con ese DNI.";
export const MENSAJE_CAMPO_INVALIDO = "Revisá este dato: no es válido o es demasiado largo.";
export const MENSAJE_FICHA_NO_GUARDADA = "No pudimos guardar la ficha. Revisá los datos e intentá de nuevo.";
export const MENSAJE_PACIENTE_NO_ENCONTRADO = "El paciente ya no existe.";
export const MENSAJE_PERMISO = "No tenés permiso para realizar esta acción.";
export const MENSAJE_GENERICO = "No pudimos completar la operación. Intentá de nuevo.";

export type ErroresDeApi = Readonly<{
  campos: Readonly<Partial<Record<CampoPaciente, string>>>;
  alerta?: string;
}>;

const CAMPOS_CONOCIDOS: ReadonlySet<string> = new Set<CampoPaciente>([
  "nombre",
  "apellido",
  "dni",
  "email",
  "telefono",
  "obraSocial",
  "nroAfiliado",
  "notas",
]);

function esCampoPaciente(campo: string): campo is CampoPaciente {
  return CAMPOS_CONOCIDOS.has(campo);
}

/** Traduce `ErrorApi` por código y estado; nunca expone el `detalle` ni los mensajes del servidor. */
export function erroresDeApiPaciente(error: unknown): ErroresDeApi {
  if (error instanceof ErrorApi) {
    if (error.estado === 409 && error.codigo === "RN-32") {
      return { campos: { dni: MENSAJE_DNI_DUPLICADO } };
    }
    if (error.estado === 422 && error.codigo === "RN-32") {
      return { campos: { email: MENSAJE_CONTACTO_OBLIGATORIO, telefono: MENSAJE_CONTACTO_OBLIGATORIO } };
    }
    if (error.estado === 400) {
      const conocidos = error.campos.map((c) => c.campo).filter(esCampoPaciente);
      if (conocidos.length === 0) return { campos: {}, alerta: MENSAJE_FICHA_NO_GUARDADA };
      return { campos: Object.fromEntries(conocidos.map((campo) => [campo, MENSAJE_CAMPO_INVALIDO])) };
    }
    if (error.estado === 404 && error.codigo === "PACIENTE_NO_ENCONTRADO") {
      return { campos: {}, alerta: MENSAJE_PACIENTE_NO_ENCONTRADO };
    }
    if (error.estado === 403) return { campos: {}, alerta: MENSAJE_PERMISO };
  }
  return { campos: {}, alerta: MENSAJE_GENERICO };
}
