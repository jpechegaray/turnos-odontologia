import { describe, expect, it } from "vitest";
import { consultaDeNotificaciones, FILTROS_VACIOS, turnoValido } from "./consulta";
import { fechaHora, MARCA_VACIO, textoPaginacion } from "./presentacion";

describe("consultaDeNotificaciones", () => {
  it("sin filtros pide la página con el tamaño fijo", () => {
    expect(consultaDeNotificaciones(FILTROS_VACIOS, 0)).toBe("/notificaciones?pagina=0&tamanio=20");
  });

  it("arma la consulta y recorta el turno", () => {
    expect(consultaDeNotificaciones({ turno: " 42 " }, 3)).toBe(
      "/notificaciones?turnoId=42&pagina=3&tamanio=20",
    );
  });

  it("omite el turno vacío", () => {
    expect(consultaDeNotificaciones({ turno: "   " }, 0)).toBe(
      "/notificaciones?pagina=0&tamanio=20",
    );
  });
});

describe("turnoValido", () => {
  it("acepta vacío y enteros positivos recortados", () => {
    expect(turnoValido("")).toBe(true);
    expect(turnoValido("  ")).toBe(true);
    expect(turnoValido(" 42 ")).toBe(true);
  });

  it.each(["abc", "0", "-1", "1.5", "1e3", "007x"])("rechaza %s", (texto) => {
    expect(turnoValido(texto)).toBe(false);
  });
});

describe("presentación", () => {
  it("formatea fecha y hora en Buenos Aires o la marca de vacío", () => {
    expect(fechaHora("2026-12-04T13:00:00Z")).toBe("04/12/2026 10:00");
    expect(fechaHora(null)).toBe(MARCA_VACIO);
  });

  it("arma el texto de la paginación en singular y plural", () => {
    expect(textoPaginacion({ pagina: 1, totalPaginas: 2, totalElementos: 25 })).toBe("Página 2 de 2 · 25 envíos");
    expect(textoPaginacion({ pagina: 0, totalPaginas: 1, totalElementos: 1 })).toBe("Página 1 de 1 · 1 envío");
  });
});
