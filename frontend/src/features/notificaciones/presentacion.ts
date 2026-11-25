import { formatearFecha, formatearHora } from "../../shared/fecha/zonaHoraria";
import type { EstadoNotificacion, NotificacionRespuesta, PaginaNotificaciones, TipoNotificacion } from "./tipos";

export const MARCA_VACIO = "—";

export const ETIQUETA_AVISO_HUECO = "Aviso de hueco";

export const ETIQUETAS_TIPO: Readonly<Record<TipoNotificacion, string>> = {
  RECORDATORIO: "Recordatorio",
  CONFIRMACION_RESERVA: "Confirmación de reserva",
  CANCELACION: "Cancelación",
  REPROGRAMACION: "Reprogramación",
  OFERTA_HUECO: ETIQUETA_AVISO_HUECO,
};

export const ETIQUETAS_ESTADO: Readonly<Record<EstadoNotificacion, string>> = {
  PENDIENTE: "Pendiente",
  ENVIADA: "Enviada",
  FALLIDA: "Fallida",
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

/** El último error solo se muestra en los envíos fallidos (RN-30). */
export function errorDe(notificacion: NotificacionRespuesta): string {
  return notificacion.estado === "FALLIDA" && notificacion.ultimoError ? notificacion.ultimoError : MARCA_VACIO;
}

export function textoPaginacion(
  pagina: Pick<PaginaNotificaciones, "pagina" | "totalPaginas" | "totalElementos">,
): string {
  const envios = pagina.totalElementos === 1 ? "1 envío" : `${pagina.totalElementos} envíos`;
  return `Página ${pagina.pagina + 1} de ${pagina.totalPaginas} · ${envios}`;
}
