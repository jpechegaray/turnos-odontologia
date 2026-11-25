import { PRESENTACION, type EstadoVisualTurno } from "./estadoTurno";
import { Icono } from "./Icono";
import "./ui.css";

export type { EstadoVisualTurno } from "./estadoTurno";

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
