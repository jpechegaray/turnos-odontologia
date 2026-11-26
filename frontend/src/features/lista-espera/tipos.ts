export type EstadoEntrada = "ACTIVA" | "ASIGNADA" | "CANCELADA" | "VENCIDA";

/** `RespuestaEntrada` de C-18: solo IDs, sin datos personales del paciente (RN-35). */
export type EntradaRespuesta = Readonly<{
  id: number;
  pacienteId: number;
  prestacionId: number;
  profesionalId: number | null;
  fechaDesde: string | null;
  fechaHasta: string;
  franjaDesde: string | null;
  franjaHasta: string | null;
  estado: EstadoEntrada;
  turnoAsignadoId: number | null;
  creadoEn: string;
}>;

/** Cuerpo del `POST /api/lista-espera`: fechas `aaaa-mm-dd`, horas `HH:mm` y nulos para lo opcional. */
export type SolicitudAltaEntrada = Readonly<{
  pacienteId: number;
  prestacionId: number;
  profesionalId: number | null;
  fechaDesde: string | null;
  fechaHasta: string;
  franjaDesde: string | null;
  franjaHasta: string | null;
}>;

/** Hueco consultado: profesional e intervalo `[inicio, fin)` como instantes ISO. */
export type HuecoConsultado = Readonly<{ profesionalId: number; inicio: string; fin: string }>;

export const ESTADOS: readonly { valor: EstadoEntrada; etiqueta: string; plural: string }[] = [
  { valor: "ACTIVA", etiqueta: "Activa", plural: "Activas" },
  { valor: "ASIGNADA", etiqueta: "Asignada", plural: "Asignadas" },
  { valor: "CANCELADA", etiqueta: "Cancelada", plural: "Canceladas" },
  { valor: "VENCIDA", etiqueta: "Vencida", plural: "Vencidas" },
];
