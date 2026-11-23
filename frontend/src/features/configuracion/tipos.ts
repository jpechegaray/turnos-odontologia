export type ConfiguracionRespuesta = Readonly<{
  "cancelacion.plazo_horas": number;
  "recordatorio.anticipacion_horas": number;
  "reserva_online.horizonte_dias": number;
  "reserva_online.anticipacion_minima_horas": number;
  "reserva_online.max_turnos_activos_por_email": number;
  "lista_espera.max_avisos_por_hueco": number;
  "consultorio.nombre": string;
}>;

export type ClaveConfiguracion = keyof ConfiguracionRespuesta;

/** Los valores del formulario viven como texto para no perder lo que la persona escribió. */
export type ConfiguracionTexto = Readonly<Record<ClaveConfiguracion, string>>;
