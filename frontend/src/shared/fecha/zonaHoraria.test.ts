// @vitest-environment node
import { afterEach, describe, expect, it } from "vitest";
import {
  aInstanteUtc,
  fechaLocal,
  formatearFecha,
  formatearHora,
  minutosDelDia,
  ZONA_CONSULTORIO,
} from "./zonaHoraria";

const zonaOriginal = process.env.TZ;

/** Corre la verificación con la zona del sistema en UTC y en otra zona lejana. */
function enVariasZonas(verificar: () => void) {
  for (const zona of ["UTC", "Asia/Tokyo", "America/Los_Angeles"]) {
    process.env.TZ = zona;
    verificar();
  }
}

afterEach(() => {
  process.env.TZ = zonaOriginal;
});

describe("Presentación en la zona de Buenos Aires", () => {
  it("usa la zona del consultorio", () => {
    expect(ZONA_CONSULTORIO).toBe("America/Argentina/Buenos_Aires");
  });

  it("Un instante UTC se muestra en hora de Buenos Aires", () => {
    enVariasZonas(() => {
      expect(formatearFecha("2026-12-01T12:00:00Z")).toBe("01/12/2026");
      expect(formatearHora("2026-12-01T12:00:00Z")).toBe("09:00");
    });
  });

  it("Un turno de noche se muestra en el día local anterior al día UTC", () => {
    enVariasZonas(() => {
      expect(formatearFecha("2026-12-02T02:30:00Z")).toBe("01/12/2026");
      expect(formatearHora("2026-12-02T02:30:00Z")).toBe("23:30");
    });
  });

  it("La medianoche local se muestra como 00:00 del día local", () => {
    enVariasZonas(() => {
      expect(formatearFecha("2026-12-02T03:00:00Z")).toBe("02/12/2026");
      expect(formatearHora("2026-12-02T03:00:00Z")).toBe("00:00");
    });
  });

  it("acepta Date y milisegundos además de texto ISO", () => {
    const instante = Date.parse("2026-12-01T12:00:00Z");
    expect(formatearHora(instante)).toBe("09:00");
    expect(formatearFecha(new Date(instante))).toBe("01/12/2026");
  });
});

describe("Conversión de hora local a instante UTC", () => {
  it("Una hora local se convierte al instante UTC", () => {
    enVariasZonas(() => {
      expect(aInstanteUtc("2026-12-01", "09:00").toISOString()).toBe("2026-12-01T12:00:00.000Z");
    });
  });

  it("Una hora local cercana a la medianoche cae en el día UTC siguiente", () => {
    enVariasZonas(() => {
      expect(aInstanteUtc("2026-12-01", "23:30").toISOString()).toBe("2026-12-02T02:30:00.000Z");
    });
  });

  it("Una hora local mal formada se rechaza", () => {
    expect(() => aInstanteUtc("2026-12-01", "25:00")).toThrow();
    expect(() => aInstanteUtc("2026-12-01", "9:00")).toThrow();
    expect(() => aInstanteUtc("2026-02-30", "09:00")).toThrow();
    expect(() => aInstanteUtc("01/12/2026", "09:00")).toThrow();
  });

  it("ida y vuelta: convertir y formatear devuelve la misma hora local", () => {
    const instante = aInstanteUtc("2026-07-15", "18:45");
    expect(formatearFecha(instante)).toBe("15/07/2026");
    expect(formatearHora(instante)).toBe("18:45");
  });
});

describe("Fecha local y minutos del día en Buenos Aires", () => {
  it("La fecha local de un instante UTC es la de Buenos Aires", () => {
    enVariasZonas(() => {
      expect(fechaLocal("2026-12-08T02:30:00Z")).toBe("2026-12-07");
      expect(fechaLocal("2026-12-08T03:00:00Z")).toBe("2026-12-08");
      expect(fechaLocal("2026-12-07T23:30:00-03:00")).toBe("2026-12-07");
    });
  });

  it("Los minutos del día se cuentan desde la medianoche de Buenos Aires", () => {
    enVariasZonas(() => {
      expect(minutosDelDia("2026-12-08T02:30:00Z")).toBe(23 * 60 + 30);
      expect(minutosDelDia("2026-12-08T03:00:00Z")).toBe(0);
      expect(minutosDelDia("2026-12-07T09:15:00-03:00")).toBe(9 * 60 + 15);
    });
  });
});
