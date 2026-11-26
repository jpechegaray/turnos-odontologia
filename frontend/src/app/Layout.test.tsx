import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRutas } from "./AppRutas";
import { registroDeFeatures } from "./rutas/modulos";
import type { Rol } from "./rutas/tipos";
import { simularSesionActiva } from "./sesion.fixture";

afterEach(() => { vi.unstubAllGlobals(); });

function renderEn(ruta: string, rol: Rol = "ADMIN") {
  simularSesionActiva(rol);
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <AppRutas registro={registroDeFeatures} />
    </MemoryRouter>,
  );
}

describe("Layout con navegación", () => {
  it("El layout muestra la marca, la navegación y la pantalla actual", async () => {
    renderEn("/");

    expect(await screen.findByRole("heading", { level: 1, name: window.BRAND.name })).toBeInTheDocument();
    const inicio = within(screen.getByRole("navigation", { name: "Principal" })).getByRole("link", { name: "Inicio" });
    expect(inicio).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("heading", { level: 2, name: "Inicio" })).toBeInTheDocument();
    expect(screen.getByRole("main")).toBeInTheDocument();
  });
});

describe("Menú lateral agrupado con íconos", () => {
  it("El menú agrupa las entradas por sección", async () => {
    renderEn("/");

    const nav = await screen.findByRole("navigation", { name: "Principal" });
    const grupos = within(nav).getAllByRole("heading", { level: 2 }).map((h) => h.textContent);
    expect(grupos).toEqual(["Atención", "Consultorio", "Administración"]);
    const atencion = within(nav).getByRole("list", { name: "Atención" });
    expect(within(atencion).getByRole("link", { name: "Agenda" })).toBeInTheDocument();
    const administracion = within(nav).getByRole("list", { name: "Administración" });
    expect(within(administracion).getByRole("link", { name: "Usuarios" })).toBeInTheDocument();
  });

  it("Un grupo sin entradas para el rol no se muestra", async () => {
    renderEn("/", "ODONTOLOGO");

    const nav = await screen.findByRole("navigation", { name: "Principal" });
    expect(within(nav).getByRole("link", { name: "Agenda" })).toBeInTheDocument();
    expect(within(nav).queryByRole("heading", { name: "Administración" })).not.toBeInTheDocument();
  });
});

describe("Menú desplegable en celular", () => {
  it('El botón "Menú" abre y cierra el menú completo', async () => {
    renderEn("/");

    const menu = await screen.findByRole("button", { name: "Menú" });
    expect(menu).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("navigation", { name: "Accesos rápidos" })).not.toBeInTheDocument();

    await userEvent.click(menu);
    expect(menu).toHaveAttribute("aria-expanded", "true");

    await userEvent.keyboard("{Escape}");
    expect(menu).toHaveAttribute("aria-expanded", "false");

    // Elegir una entrada del menú también lo cierra.
    await userEvent.click(menu);
    await userEvent.click(within(screen.getByRole("navigation", { name: "Principal" })).getByRole("link", { name: "Pacientes" }));
    expect(menu).toHaveAttribute("aria-expanded", "false");
  });
});

describe("Ruta inexistente", () => {
  it("Una ruta desconocida muestra página no encontrada", async () => {
    renderEn("/ruta-que-no-existe");

    expect(await screen.findByRole("heading", { name: "Página no encontrada" })).toBeInTheDocument();
    const volver = within(screen.getByRole("main")).getByRole("link", { name: "Volver al inicio" });
    expect(volver).toHaveAttribute("href", "/");

    await userEvent.click(volver);
    expect(screen.getByRole("heading", { level: 2, name: "Inicio" })).toBeInTheDocument();
  });
});
