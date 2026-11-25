import type { NombreIcono } from "./Icono";

/** Estados de RN-12 más las dos variantes visuales de la agenda: cancelación tardía y bloqueo. */
export type EstadoVisualTurno =
  | "PROGRAMADO"
  | "CONFIRMADO"
  | "ATENDIDO"
  | "AUSENTE"
  | "CANCELADO"
  | "CANCELADO_TARDE"
  | "BLOQUEO";

export const PRESENTACION: Readonly<Record<EstadoVisualTurno, { texto: string; clase: string; icono: NombreIcono }>> = {
  PROGRAMADO: { texto: "Reservado", clase: "st-reservado", icono: "reloj" },
  CONFIRMADO: { texto: "Confirmado", clase: "st-confirmado", icono: "check" },
  ATENDIDO: { texto: "Atendido", clase: "st-atendido", icono: "hecho" },
  AUSENTE: { texto: "Ausente", clase: "st-ausente", icono: "ausente" },
  CANCELADO: { texto: "Cancelado", clase: "st-cancelado", icono: "cruz" },
  CANCELADO_TARDE: { texto: "Cancelación tardía", clase: "st-tardia", icono: "alerta" },
  BLOQUEO: { texto: "Bloqueo", clase: "st-bloqueo", icono: "candado" },
};

/** Clase de color del estado, para pintar el bloque que contiene al badge (slots de la agenda). */
export function claseDeEstado(estado: EstadoVisualTurno): string {
  return PRESENTACION[estado].clase;
}
