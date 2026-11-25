import { describe, expect, it } from "vitest";
import type { FormularioBloqueoValores } from "./tipos";
import {
  MENSAJE_FECHA_INVALIDA,
  MENSAJE_FIN_POSTERIOR,
  MENSAJE_MOTIVO_LARGO,
  MENSAJE_MOTIVO_OBLIGATORIO,
  validarBloqueo,
} from "./validacion";

const VALIDO: FormularioBloqueoValores = {
  fechaInicio: "2026-12-01",
  horaInicio: "10:00",
  fechaFin: "2026-12-01",
  horaFin: "18:00",
  motivo: " Congreso ",
};

describe("validarBloqueo", () => {
  it("arma el cuerpo con los instantes en UTC y el motivo recortado", () => {
    expect(validarBloqueo(VALIDO)).toEqual({
      valido: true,
      cuerpo: { inicio: "2026-12-01T13:00:00.000Z", fin: "2026-12-01T21:00:00.000Z", motivo: "Congreso" },
    });
  });

  it("acepta un bloqueo de varios días", () => {
    const resultado = validarBloqueo({ ...VALIDO, fechaFin: "2026-12-15", horaFin: "09:00" });
    expect(resultado).toMatchObject({ valido: true, cuerpo: { fin: "2026-12-15T12:00:00.000Z" } });
  });

  it("rechaza un fin igual o anterior al inicio", () => {
    expect(validarBloqueo({ ...VALIDO, horaFin: "10:00" })).toEqual({
      valido: false,
      errores: { fin: MENSAJE_FIN_POSTERIOR },
    });
    expect(validarBloqueo({ ...VALIDO, horaInicio: "18:00", horaFin: "10:00" })).toEqual({
      valido: false,
      errores: { fin: MENSAJE_FIN_POSTERIOR },
    });
  });

  it("rechaza fechas u horas vacías", () => {
    expect(validarBloqueo({ ...VALIDO, fechaInicio: "" })).toEqual({
      valido: false,
      errores: { inicio: MENSAJE_FECHA_INVALIDA },
    });
    expect(validarBloqueo({ ...VALIDO, horaFin: "" })).toEqual({
      valido: false,
      errores: { fin: MENSAJE_FECHA_INVALIDA },
    });
  });

  it("rechaza un motivo en blanco", () => {
    expect(validarBloqueo({ ...VALIDO, motivo: "   " })).toEqual({
      valido: false,
      errores: { motivo: MENSAJE_MOTIVO_OBLIGATORIO },
    });
  });

  it("rechaza un motivo de 256 caracteres y acepta uno de 255", () => {
    expect(validarBloqueo({ ...VALIDO, motivo: "x".repeat(256) })).toEqual({
      valido: false,
      errores: { motivo: MENSAJE_MOTIVO_LARGO },
    });
    expect(validarBloqueo({ ...VALIDO, motivo: "x".repeat(255) })).toMatchObject({ valido: true });
  });

  it("informa a la vez los errores de fechas y de motivo", () => {
    expect(validarBloqueo({ ...VALIDO, horaFin: "09:00", motivo: "" })).toEqual({
      valido: false,
      errores: { fin: MENSAJE_FIN_POSTERIOR, motivo: MENSAJE_MOTIVO_OBLIGATORIO },
    });
  });
});
