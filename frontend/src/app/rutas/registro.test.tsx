import { render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRutas } from "../AppRutas";
import { simularSesionActiva } from "../sesion.fixture";
import { construirRegistro } from "./registro";
import type { ModuloRutas } from "./tipos";

/** Mismo mecanismo que modulos.ts, apuntado a la feature de ejemplo de los fixtures. */
const modulosDeEjemplo = import.meta.glob<ModuloRutas>("./__fixtures__/features/*/routes.ts", { eager: true });

const Vacia = () => null;
afterEach(() => { vi.unstubAllGlobals(); });

function renderEn(ruta: string, modulos: Record<string, ModuloRutas> = modulosDeEjemplo) {
  simularSesionActiva();
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <AppRutas registro={construirRegistro(modulos)} />
    </MemoryRouter>,
  );
}

describe("Registro automático de rutas por feature", () => {
  it("Un routes.ts de ejemplo aparece en el router sin editar archivos centrales", async () => {
    const registro = construirRegistro(modulosDeEjemplo);
    expect(registro.rutas.map((r) => r.ruta)).toContain("/ejemplo");

    renderEn("/ejemplo");

    expect(await screen.findByRole("heading", { name: "Pantalla de ejemplo" })).toBeInTheDocument();
  });

  it("Dos features con la misma ruta producen un error explícito", () => {
    const modulos: Record<string, ModuloRutas> = {
      "../../features/a/routes.ts": { rutas: [{ ruta: "/repetida", componente: Vacia }] },
      "../../features/b/routes.ts": { rutas: [{ ruta: "/repetida", componente: Vacia }] },
    };
    expect(() => construirRegistro(modulos)).toThrow(/\/repetida/);
  });

  it("un routes.ts que no exporta rutas produce un error que nombra el archivo", () => {
    const modulos = { "../../features/rota/routes.ts": {} as ModuloRutas };
    expect(() => construirRegistro(modulos)).toThrow(/features\/rota\/routes\.ts/);
  });
});

describe("Menú construido desde las features", () => {
  it("Un routes.ts de ejemplo aparece en el menú sin editar archivos centrales", async () => {
    renderEn("/ejemplo");

    const navegacion = await screen.findByRole("navigation", { name: "Principal" });
    expect(within(navegacion).getByRole("link", { name: "Ejemplo" })).toHaveAttribute("href", "/ejemplo");
  });

  it("El menú respeta el orden declarado", () => {
    const modulos: Record<string, ModuloRutas> = {
      "../../features/agenda/routes.ts": { rutas: [{ ruta: "/agenda", componente: Vacia, menu: { etiqueta: "Agenda", orden: 10 } }] },
      "../../features/inicio/routes.ts": { rutas: [{ ruta: "/", componente: Vacia, menu: { etiqueta: "Inicio", orden: 0 } }] },
      "../../features/b/routes.ts": { rutas: [{ ruta: "/b", componente: Vacia, menu: { etiqueta: "Bloqueos", orden: 10 } }] },
    };
    expect(construirRegistro(modulos).menu.map((e) => e.etiqueta)).toEqual(["Inicio", "Agenda", "Bloqueos"]);
  });

  it("Una ruta sin entrada de menú no aparece en la navegación", async () => {
    const registro = construirRegistro(modulosDeEjemplo);
    expect(registro.menu.map((e) => e.ruta)).not.toContain("/ejemplo/detalle");
    expect(registro.rutas.map((r) => r.ruta)).toContain("/ejemplo/detalle");

    renderEn("/ejemplo/detalle");

    expect(await screen.findByRole("heading", { name: "Detalle de ejemplo" })).toBeInTheDocument();
    expect(within(screen.getByRole("navigation")).queryByRole("link", { name: /detalle/i })).not.toBeInTheDocument();
  });
});
