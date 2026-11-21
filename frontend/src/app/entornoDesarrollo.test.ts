// @vitest-environment node
import { describe, expect, it } from "vitest";
import configuracion from "../../vite.config";

describe("Servidor de desarrollo", () => {
  it("Las peticiones a /api se redirigen al backend en desarrollo", () => {
    const proxy = configuracion.server?.proxy;
    expect(proxy).toBeDefined();
    const regla = proxy?.["/api"];
    expect(regla).toMatchObject({ target: "http://localhost:8080" });
    expect(regla).not.toHaveProperty("rewrite");
    // Triangulación: /api es la única regla; no hay reglas que capturen otras rutas de la SPA.
    expect(Object.keys(proxy ?? {})).toEqual(["/api"]);
  });
});
