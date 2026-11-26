import type { FiltrosNotificaciones } from "./tipos";

export const TAMANIO_PAGINA = 20;

export const FILTROS_VACIOS: FiltrosNotificaciones = { turno: "" };

const ENTERO_POSITIVO = /^[1-9]\d*$/;

/** Vacío (sin filtro) o un entero positivo, después de recortar espacios. */
export function turnoValido(texto: string): boolean {
  const recortado = texto.trim();
  return recortado === "" || ENTERO_POSITIVO.test(recortado);
}

export function hayFiltros(filtros: FiltrosNotificaciones): boolean {
  return filtros.turno.trim() !== "";
}

/** Ruta de la consulta con el turno opcional y la página del registro. */
export function consultaDeNotificaciones(filtros: FiltrosNotificaciones, pagina: number): string {
  const parametros = new URLSearchParams();
  const turno = filtros.turno.trim();
  if (turno !== "") parametros.set("turnoId", turno);
  parametros.set("pagina", String(pagina));
  parametros.set("tamanio", String(TAMANIO_PAGINA));
  return `/notificaciones?${parametros.toString()}`;
}
