import { seSolapan, type Intervalo } from "../../../shared/fecha/intervalo";
import type { TramoHorario, TramoSolicitud } from "./tipos";

export const MENSAJE_HORA_INVALIDA = "Ingresá la hora de inicio y de fin en formato HH:mm.";
export const MENSAJE_FIN_POSTERIOR = "La hora de fin debe ser posterior a la de inicio.";
export const MENSAJE_DIA_INVALIDO = "Elegí un día de la semana.";

const PATRON_HORA = /^([01]\d|2[0-3]):([0-5]\d)$/;

function aMinutos(hora: string): number | undefined {
  const partes = PATRON_HORA.exec(hora);
  return partes ? Number(partes[1]) * 60 + Number(partes[2]) : undefined;
}

/** `[inicio, fin)` en minutos del día; sirve para reutilizar `seSolapan` (RN-16). */
function intervaloDe(horaInicio: string, horaFin: string): Intervalo {
  return { inicio: aMinutos(horaInicio) ?? 0, fin: aMinutos(horaFin) ?? 0 };
}

/**
 * Devuelve el primer motivo de rechazo o `undefined` si el tramo es válido. `idEditado` excluye al
 * propio tramo de la comparación al modificarlo.
 */
export function validarTramo(
  tramo: TramoSolicitud,
  existentes: readonly TramoHorario[],
  idEditado?: number,
): string | undefined {
  if (!Number.isInteger(tramo.diaSemana) || tramo.diaSemana < 1 || tramo.diaSemana > 7) return MENSAJE_DIA_INVALIDO;
  const inicio = aMinutos(tramo.horaInicio);
  const fin = aMinutos(tramo.horaFin);
  if (inicio === undefined || fin === undefined) return MENSAJE_HORA_INVALIDA;
  if (fin <= inicio) return MENSAJE_FIN_POSTERIOR;

  const candidato: Intervalo = { inicio, fin };
  const conflicto = existentes.find(
    (t) => t.id !== idEditado && t.diaSemana === tramo.diaSemana && seSolapan(candidato, intervaloDe(t.horaInicio, t.horaFin)),
  );
  return conflicto && `El tramo se superpone con el de ${conflicto.horaInicio} a ${conflicto.horaFin}.`;
}
