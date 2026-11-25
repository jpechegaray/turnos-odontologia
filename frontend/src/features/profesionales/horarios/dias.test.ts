import { describe, expect, it } from "vitest";
import { agruparPorDia, DIAS_SEMANA } from "./dias";
import type { TramoHorario } from "./tipos";

const tramo = (id: number, diaSemana: number, horaInicio: string, horaFin: string): TramoHorario => ({
  id,
  profesionalId: 1,
  diaSemana,
  horaInicio,
  horaFin,
});

describe("agruparPorDia", () => {
  it("devuelve los siete días de lunes a domingo y ordena cada uno por hora de inicio", () => {
    const dias = agruparPorDia([tramo(1, 1, "13:00", "17:00"), tramo(2, 3, "09:00", "13:00"), tramo(3, 1, "08:00", "12:00")]);

    expect(dias.map((d) => d.nombre)).toEqual(["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"]);
    expect(dias.map((d) => d.dia)).toEqual([1, 2, 3, 4, 5, 6, 7]);
    expect(dias[0].tramos.map((t) => t.id)).toEqual([3, 1]);
    expect(dias[2].tramos.map((t) => t.id)).toEqual([2]);
    expect(dias[1].tramos).toEqual([]);
  });

  it("con una lista vacía devuelve los siete días sin tramos", () => {
    const dias = agruparPorDia([]);

    expect(dias).toHaveLength(DIAS_SEMANA.length);
    expect(dias.every((d) => d.tramos.length === 0)).toBe(true);
  });
});
