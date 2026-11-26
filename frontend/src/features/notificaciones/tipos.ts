/** Estado de un envío según el registro de C-25 (RN-30). */
export type EstadoNotificacion = "PENDIENTE" | "ENVIADA" | "FALLIDA";

export type TipoNotificacion = "RECORDATORIO" | "CONFIRMACION_RESERVA" | "CANCELACION" | "REPROGRAMACION"
  | "OFERTA_HUECO" | "INGRESO_LISTA_ESPERA" | "CONFIRMACION_LISTA_ESPERA";

/** Elemento de `GET /api/notificaciones`; los instantes vienen en UTC (ISO-8601). */
export type NotificacionRespuesta = Readonly<{
  id: number;
  tipo: TipoNotificacion;
  estado: EstadoNotificacion;
  canal: string;
  destinatario: string;
  turnoId: number | null;
  ofertaHuecoId: number | null;
  pacienteId: number | null;
  inicioReferencia: string | null;
  intentos: number;
  ultimoError: string | null;
  programadaPara: string | null;
  enviadaEn: string | null;
}>;

/** Formato de página de la API (C-25). */
export type PaginaNotificaciones = Readonly<{
  contenido: readonly NotificacionRespuesta[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}>;

export type FiltrosNotificaciones = Readonly<{
  /** Número de turno tal como lo escribió el usuario; se valida antes de consultar. */
  turno: string;
}>;
