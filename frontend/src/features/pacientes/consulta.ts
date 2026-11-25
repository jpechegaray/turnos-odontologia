import type { FiltrosPacientes } from "./tipos";

export const FILTROS_VACIOS: FiltrosPacientes = { apellido: "", nombre: "", dni: "", email: "" };

const ORDEN_FILTROS = ["apellido", "nombre", "dni", "email"] as const;

/** Ruta de la consulta con solo los filtros recortados y no vacíos, en orden fijo. */
export function consultaDePacientes(filtros: FiltrosPacientes): string {
  const parametros = new URLSearchParams();
  for (const clave of ORDEN_FILTROS) {
    const valor = filtros[clave].trim();
    if (valor !== "") parametros.set(clave, valor);
  }
  const texto = parametros.toString();
  return texto === "" ? "/pacientes" : `/pacientes?${texto}`;
}
