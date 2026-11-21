// @vitest-environment node
import { existsSync, statSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const FEATURES = [
  "auth",
  "agenda",
  "pacientes",
  "profesionales",
  "prestaciones",
  "lista-espera",
  "configuracion",
  "notificaciones",
  "reserva-publica",
];

function esCarpeta(ruta: string): boolean {
  return existsSync(ruta) && statSync(ruta).isDirectory();
}

describe("Estructura de carpetas", () => {
  it("La estructura de carpetas sigue la arquitectura del frontend", () => {
    const src = new URL("../", import.meta.url);
    for (const carpeta of ["app", "shared", "features"]) {
      expect(esCarpeta(fileURLToPath(new URL(carpeta, src))), carpeta).toBe(true);
    }
    for (const feature of FEATURES) {
      expect(esCarpeta(fileURLToPath(new URL(`features/${feature}`, src))), feature).toBe(true);
    }
  });
});
