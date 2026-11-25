import type { TramoHorario } from "./tipos";

/** Días ISO (1 = lunes) en el orden en que se muestran. */
export const DIAS_SEMANA: readonly Readonly<{ dia: number; nombre: string }>[] = [
  { dia: 1, nombre: "Lunes" },
  { dia: 2, nombre: "Martes" },
  { dia: 3, nombre: "Miércoles" },
  { dia: 4, nombre: "Jueves" },
  { dia: 5, nombre: "Viernes" },
  { dia: 6, nombre: "Sábado" },
  { dia: 7, nombre: "Domingo" },
];

export type DiaConTramos = Readonly<{ dia: number; nombre: string; tramos: readonly TramoHorario[] }>;

/** Siempre los siete días; cada uno con sus tramos ordenados por hora de inicio y luego por id. */
export function agruparPorDia(tramos: readonly TramoHorario[]): DiaConTramos[] {
  return DIAS_SEMANA.map(({ dia, nombre }) => ({
    dia,
    nombre,
    tramos: tramos
      .filter((t) => t.diaSemana === dia)
      .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio) || a.id - b.id),
  }));
}
