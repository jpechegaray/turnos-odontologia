import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { obtenerApiBaseUrl } from "./apiBaseUrl";

describe("Base de la API", () => {
  let valorOriginal: string | undefined;

  beforeEach(() => {
    valorOriginal = import.meta.env.VITE_API_BASE_URL;
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    if (valorOriginal === undefined) {
      delete import.meta.env.VITE_API_BASE_URL;
    } else {
      import.meta.env.VITE_API_BASE_URL = valorOriginal;
    }
  });

  it("La base de la API es /api por defecto", () => {
    delete import.meta.env.VITE_API_BASE_URL;
    expect(obtenerApiBaseUrl()).toBe("/api");
    // Triangulación: un valor vacío también cae en /api.
    import.meta.env.VITE_API_BASE_URL = "";
    expect(obtenerApiBaseUrl()).toBe("/api");
  });

  it("La base de la API toma el valor de VITE_API_BASE_URL", () => {
    vi.stubEnv("VITE_API_BASE_URL", "/api-prueba");
    expect(obtenerApiBaseUrl()).toBe("/api-prueba");
  });
});
