import { Icono, type NombreIcono } from "./Icono";
import "./ui.css";

/** Estados de RN-12 más las dos variantes visuales de la agenda: cancelación tardía y bloqueo. */
export type EstadoVisualTurno =
  | "PROGRAMADO"
  | "CONFIRMADO"
  | "ATENDIDO"
  | "AUSENTE"
  | "CANCELADO"
  | "CANCELADO_TARDE"
  | "BLOQUEO";

const PRESENTACION: Readonly<Record<EstadoVisualTurno, { texto: string; clase: string; icono: NombreIcono }>> = {
  PROGRAMADO: { texto: "Reservado", clase: "st-reservado", icono: "reloj" },
  CONFIRMADO: { texto: "Confirmado", clase: "st-confirmado", icono: "check" },
  ATENDIDO: { texto: "Atendido", clase: "st-atendido", icono: "hecho" },
  AUSENTE: { texto: "Ausente", clase: "st-ausente", icono: "ausente" },
  CANCELADO: { texto: "Cancelado", clase: "st-cancelado", icono: "cruz" },
  CANCELADO_TARDE: { texto: "Cancelación tardía", clase: "st-tardia", icono: "alerta" },
  BLOQUEO: { texto: "Bloqueo", clase: "st-bloqueo", icono: "candado" },
};

/** Color + ícono + texto, nunca solo color (design-system §Reglas de uso). */
export function BadgeEstadoTurno({ estado }: { estado: EstadoVisualTurno }) {
  const { texto, clase, icono } = PRESENTACION[estado];
  return (
    <span className={`badge ${clase}`}>
      <Icono nombre={icono} />
      <span className="badge__txt">{texto}</span>
    </span>
  );
}
