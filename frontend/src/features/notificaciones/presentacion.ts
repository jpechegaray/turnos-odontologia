import { formatearFecha, formatearHora } from "../../shared/fecha/zonaHoraria";
import type { NotificacionRespuesta, PaginaNotificaciones, TipoNotificacion } from "./tipos";

export const MARCA_VACIO = "—";

export const ETIQUETA_AVISO_HUECO = "Aviso de hueco";

export const ETIQUETAS_TIPO: Readonly<Record<TipoNotificacion, string>> = {
  RECORDATORIO: "Recordatorio",
  CONFIRMACION_RESERVA: "Confirmación de reserva",
  CANCELACION: "Cancelación",
  REPROGRAMACION: "Reprogramación",
  OFERTA_HUECO: ETIQUETA_AVISO_HUECO,
  INGRESO_LISTA_ESPERA: "Ingreso a lista de espera",
  CONFIRMACION_LISTA_ESPERA: "Turno desde lista de espera",
};

/** Fecha y hora del consultorio (Buenos Aires), o la marca de vacío. */
export function fechaHora(instante: string | null): string {
  return instante ? `${formatearFecha(instante)} ${formatearHora(instante)}` : MARCA_VACIO;
}

/** `#id` del turno; los avisos de oferta no tienen turno (C-36). */
export function turnoDe(notificacion: NotificacionRespuesta): string {
  if (notificacion.turnoId !== null) return `#${notificacion.turnoId}`;
  return notificacion.ofertaHuecoId !== null ? ETIQUETA_AVISO_HUECO : MARCA_VACIO;
}

export function textoPaginacion(
  pagina: Pick<PaginaNotificaciones, "pagina" | "totalPaginas" | "totalElementos">,
): string {
  const envios = pagina.totalElementos === 1 ? "1 envío" : `${pagina.totalElementos} envíos`;
  return `Página ${pagina.pagina + 1} de ${pagina.totalPaginas} · ${envios}`;
}
