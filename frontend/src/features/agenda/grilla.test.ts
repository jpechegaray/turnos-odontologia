// @vitest-environment node
import { describe, expect, it } from "vitest";
import { diasDeLaSemana, filasDe, nombreDelDia, rangoDeGrilla, sumarDias, tramoEnDia } from "./grilla";

describe("Fechas civiles de la agenda", () => {
  it("Sumar días cruza meses y años", () => {
    expect(sumarDias("2026-12-07", 1)).toBe("2026-12-08");
    expect(sumarDias("2026-12-07", -7)).toBe("2026-11-30");
    expect(sumarDias("2026-12-31", 1)).toBe("2027-01-01");
  });

  it("La semana tiene siete fechas desde el lunes con el nombre de cada día", () => {
    const dias = diasDeLaSemana("2026-12-07");
    expect(dias).toEqual([
      "2026-12-07", "2026-12-08", "2026-12-09", "2026-12-10", "2026-12-11", "2026-12-12", "2026-12-13",
    ]);
    expect(dias.map(nombreDelDia)).toEqual(["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"]);
  });

  it("Una fecha mal formada se rechaza", () => {
    expect(() => sumarDias("07/12/2026", 1)).toThrow(RangeError);
  });
});

describe("Tramos del día en Buenos Aires", () => {
  it("Un turno del día queda en sus minutos locales", () => {
    expect(tramoEnDia("2026-12-07T10:00:00-03:00", "2026-12-07T10:30:00-03:00", "2026-12-07"))
      .toEqual({ desdeMin: 600, hastaMin: 630 });
  });

  it("Un turno que cruza la medianoche se recorta a cada día", () => {
    const inicio = "2026-12-07T23:30:00-03:00";
    const fin = "2026-12-08T00:30:00-03:00";
    expect(tramoEnDia(inicio, fin, "2026-12-07")).toEqual({ desdeMin: 1410, hastaMin: 1440 });
    expect(tramoEnDia(inicio, fin, "2026-12-08")).toEqual({ desdeMin: 0, hastaMin: 30 });
  });

  it("Un bloqueo de varios días cubre el día entero del medio y no toca los demás", () => {
    const inicio = "2026-12-09T15:00:00-03:00";
    const fin = "2026-12-11T10:00:00-03:00";
    expect(tramoEnDia(inicio, fin, "2026-12-08")).toBeUndefined();
    expect(tramoEnDia(inicio, fin, "2026-12-09")).toEqual({ desdeMin: 900, hastaMin: 1440 });
    expect(tramoEnDia(inicio, fin, "2026-12-10")).toEqual({ desdeMin: 0, hastaMin: 1440 });
    expect(tramoEnDia(inicio, fin, "2026-12-11")).toEqual({ desdeMin: 0, hastaMin: 600 });
    // Termina justo a la medianoche del 12: [inicio, fin) no toca ese día.
    expect(tramoEnDia(inicio, "2026-12-12T00:00:00-03:00", "2026-12-12")).toBeUndefined();
  });
});

describe("Rango y filas de la grilla", () => {
  it("Sin datos la grilla va de 08:00 a 20:00", () => {
    expect(rangoDeGrilla([])).toEqual({ desdeMin: 480, hastaMin: 1200 });
  });

  it("Con datos la grilla va de la primera a la última hora en punto", () => {
    expect(rangoDeGrilla([{ desdeMin: 555, hastaMin: 600 }, { desdeMin: 690, hastaMin: 705 }]))
      .toEqual({ desdeMin: 540, hastaMin: 720 });
  });

  it("Cada tramo ocupa sus filas de 15 minutos debajo del encabezado", () => {
    const rango = { desdeMin: 540, hastaMin: 720 };
    expect(filasDe({ desdeMin: 540, hastaMin: 570 }, rango)).toEqual({ desde: 2, hasta: 4 });
    // Un turno de 10 minutos ocupa al menos una fila.
    expect(filasDe({ desdeMin: 600, hastaMin: 610 }, rango)).toEqual({ desde: 6, hasta: 7 });
  });
});
