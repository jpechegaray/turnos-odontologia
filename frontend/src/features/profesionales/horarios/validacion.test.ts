import { describe, expect, it } from "vitest";
import type { TramoHorario } from "./tipos";
import { MENSAJE_FIN_POSTERIOR, MENSAJE_HORA_INVALIDA, validarTramo } from "./validacion";

const LUNES_MANANA: TramoHorario = { id: 7, profesionalId: 1, diaSemana: 1, horaInicio: "09:00", horaFin: "13:00" };

describe("validarTramo", () => {
  it("rechaza un fin igual o anterior al inicio", () => {
    expect(validarTramo({ diaSemana: 1, horaInicio: "12:00", horaFin: "12:00" }, [])).toBe(MENSAJE_FIN_POSTERIOR);
    expect(validarTramo({ diaSemana: 1, horaInicio: "12:00", horaFin: "09:00" }, [])).toBe(MENSAJE_FIN_POSTERIOR);
  });

  it("rechaza horas vacías o mal formadas", () => {
    expect(validarTramo({ diaSemana: 1, horaInicio: "", horaFin: "13:00" }, [])).toBe(MENSAJE_HORA_INVALIDA);
    expect(validarTramo({ diaSemana: 1, horaInicio: "9:00", horaFin: "13:00" }, [])).toBe(MENSAJE_HORA_INVALIDA);
  });

  it("rechaza un tramo superpuesto con otro del mismo día e informa cuál", () => {
    expect(validarTramo({ diaSemana: 1, horaInicio: "12:00", horaFin: "14:00" }, [LUNES_MANANA])).toBe(
      "El tramo se superpone con el de 09:00 a 13:00.",
    );
    expect(validarTramo({ diaSemana: 1, horaInicio: "10:00", horaFin: "11:00" }, [LUNES_MANANA])).toBe(
      "El tramo se superpone con el de 09:00 a 13:00.",
    );
  });

  it("acepta un tramo contiguo o de otro día", () => {
    expect(validarTramo({ diaSemana: 1, horaInicio: "13:00", horaFin: "17:00" }, [LUNES_MANANA])).toBeUndefined();
    expect(validarTramo({ diaSemana: 1, horaInicio: "07:00", horaFin: "09:00" }, [LUNES_MANANA])).toBeUndefined();
    expect(validarTramo({ diaSemana: 2, horaInicio: "09:00", horaFin: "13:00" }, [LUNES_MANANA])).toBeUndefined();
  });

  it("excluye de la comparación al tramo que se está modificando", () => {
    expect(
      validarTramo({ diaSemana: 1, horaInicio: "10:00", horaFin: "14:00" }, [LUNES_MANANA], LUNES_MANANA.id),
    ).toBeUndefined();
  });
});
