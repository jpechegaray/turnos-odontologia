import { BadgeEstadoTurno } from "../../shared/ui";
import { nombreDelPaciente, rangoLegible } from "./presentacion";
import type { TurnoAgenda } from "./tipos";

/** Paciente elegido en la agenda y, si se eligió desde un turno, ese turno. */
export type FichaElegida = Readonly<{ pacienteId: number; nombre: string; turno?: TurnoAgenda }>;

type Props = Readonly<{
  turno: TurnoAgenda;
  prestacion: string;
  /** Sillón o profesional, según lo que no indique la columna. */
  detalle: string;
  onVerFicha: (ficha: FichaElegida) => void;
}>;

/**
 * Paciente, horario, prestación y estado de un turno. Solo lectura: el único botón abre la ficha básica,
 * y no aparece si la API ocultó al paciente (turnos de otro profesional para el odontólogo, RN-33).
 */
export function ContenidoTurno({ turno, prestacion, detalle, onVerFicha }: Props) {
  const paciente = nombreDelPaciente(turno);
  return (
    <>
      <strong>
        {paciente ? (
          <button
            type="button"
            className="slot__paciente"
            aria-label={`Ver ficha de ${paciente}`}
            onClick={() => onVerFicha({ pacienteId: turno.pacienteId, nombre: paciente, turno })}
          >
            {paciente}
          </button>
        ) : (
          "Paciente de otro profesional"
        )}
      </strong>
      <span className="slot__meta">{`${rangoLegible(turno.inicio, turno.fin)} · ${prestacion}`}</span>
      <span className="slot__pie">
        <BadgeEstadoTurno estado={turno.estado} />
        <span className="slot__detalle">{detalle}</span>
      </span>
    </>
  );
}
