import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AppRutas } from "./AppRutas";
import { registroDeFeatures } from "./rutas/modulos";

function renderEn(ruta: string) {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <AppRutas registro={registroDeFeatures} />
    </MemoryRouter>,
  );
}

describe("Layout con navegación", () => {
  it("El layout muestra la marca, la navegación y la pantalla actual", () => {
    renderEn("/");

    expect(screen.getByRole("heading", { level: 1, name: window.BRAND.name })).toBeInTheDocument();
    const inicio = within(screen.getByRole("navigation", { name: "Principal" })).getByRole("link", { name: "Inicio" });
    expect(inicio).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("heading", { level: 2, name: "Inicio" })).toBeInTheDocument();
    expect(screen.getByRole("main")).toBeInTheDocument();
  });
});

describe("Ruta inexistente", () => {
  it("Una ruta desconocida muestra página no encontrada", async () => {
    renderEn("/ruta-que-no-existe");

    expect(screen.getByRole("heading", { name: "Página no encontrada" })).toBeInTheDocument();
    const volver = within(screen.getByRole("main")).getByRole("link", { name: "Volver al inicio" });
    expect(volver).toHaveAttribute("href", "/");

    await userEvent.click(volver);
    expect(screen.getByRole("heading", { level: 2, name: "Inicio" })).toBeInTheDocument();
  });
});
