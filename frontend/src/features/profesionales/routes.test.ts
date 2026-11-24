import { describe, expect, it } from "vitest";
import type { ModuloRutas, RutaFeature } from "../../app/rutas/tipos";
import { agruparRutas, rutas } from "./routes";

describe("agregador de rutas de profesionales", () => {
  it("El agregador expone las rutas de profesionales y sillones sin duplicados", () => {
    const caminos = rutas.map((r) => r.ruta);

    expect(caminos).toEqual(expect.arrayContaining(["/profesionales", "/sillones"]));
    expect(new Set(caminos).size).toBe(caminos.length);

    const profesionales = rutas.find((r) => r.ruta === "/profesionales");
    const sillones = rutas.find((r) => r.ruta === "/sillones");
    expect(profesionales?.menu).toEqual({ etiqueta: "Profesionales", orden: 30 });
    expect(sillones?.menu).toEqual({ etiqueta: "Sillones", orden: 40 });
    for (const ruta of [profesionales, sillones]) {
      expect(ruta?.roles).toEqual(expect.arrayContaining(["ADMIN", "RECEPCION", "ODONTOLOGO"]));
    }
  });

  it("El agregador falla si un routes.ts hijo no exporta rutas", () => {
    const valida: RutaFeature = { ruta: "/a", componente: () => null };
    const modulos = {
      "./a/routes.ts": { rutas: [valida] },
      "./b/routes.ts": {} as unknown as ModuloRutas,
    };

    expect(() => agruparRutas(modulos)).toThrow("./b/routes.ts");
  });
});
