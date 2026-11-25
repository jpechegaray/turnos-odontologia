import type { ConsultaAgenda } from "./tipos";

/** Ruta de `GET /api/agenda` con solo los parámetros informados. */
export function rutaDeAgenda(consulta: ConsultaAgenda): string {
  const parametros = new URLSearchParams({ vista: consulta.vista });
  if (consulta.fecha) parametros.set("fecha", consulta.fecha);
  if (consulta.profesionalId !== undefined) parametros.set("profesionalId", String(consulta.profesionalId));
  if (consulta.sillonId !== undefined) parametros.set("sillonId", String(consulta.sillonId));
  return `/agenda?${parametros.toString()}`;
}
