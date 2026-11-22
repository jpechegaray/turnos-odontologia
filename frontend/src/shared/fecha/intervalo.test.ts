// @vitest-environment node
import { describe, expect, it } from "vitest";
import { contiene, crearIntervalo, seSolapan } from "./intervalo";

const h = (hora: string) => `2026-12-01T${hora}:00Z`;

describe("Intervalos semiabiertos", () => {
  it("Intervalos contiguos no se solapan", () => {
    const a = crearIntervalo(h("10:00"), h("10:30"));
    const b = crearIntervalo(h("10:30"), h("11:00"));
    expect(seSolapan(a, b)).toBe(false);
    expect(seSolapan(b, a)).toBe(false);
  });

  it("Intervalos que comparten un minuto se solapan", () => {
    const a = crearIntervalo(h("10:00"), h("10:30"));
    const b = crearIntervalo(h("10:29"), h("11:00"));
    expect(seSolapan(a, b)).toBe(true);
    expect(seSolapan(b, a)).toBe(true);
  });

  it("Un intervalo contenido en otro se solapa", () => {
    const grande = crearIntervalo(h("10:00"), h("11:00"));
    const chico = crearIntervalo(h("10:15"), h("10:45"));
    expect(seSolapan(grande, chico)).toBe(true);
    expect(seSolapan(chico, grande)).toBe(true);
  });

  it("el mismo intervalo exacto se solapa consigo mismo", () => {
    const a = crearIntervalo(h("10:00"), h("10:30"));
    expect(seSolapan(a, crearIntervalo(h("10:00"), h("10:30")))).toBe(true);
  });

  it("El fin de un intervalo no está contenido en él", () => {
    const a = crearIntervalo(h("10:00"), h("10:30"));
    expect(contiene(a, h("10:00"))).toBe(true);
    expect(contiene(a, h("10:29"))).toBe(true);
    expect(contiene(a, h("10:30"))).toBe(false);
  });

  it("Un intervalo vacío o invertido se rechaza", () => {
    expect(() => crearIntervalo(h("10:00"), h("10:00"))).toThrow();
    expect(() => crearIntervalo(h("10:30"), h("10:00"))).toThrow();
    expect(() => crearIntervalo("no es fecha", h("10:00"))).toThrow();
  });

  it("el intervalo es inmutable y guarda milisegundos", () => {
    const a = crearIntervalo(h("10:00"), h("10:30"));
    expect(a.fin - a.inicio).toBe(30 * 60_000);
    expect(Object.isFrozen(a)).toBe(true);
  });
});
