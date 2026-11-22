import { render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import { AppRutas } from "./AppRutas";
import { construirRegistro } from "./rutas/registro";
import type { Rol } from "./rutas/tipos";

const registro = construirRegistro({ ejemplo: { rutas: [
  { ruta: "/", componente: () => <h2>Agenda de lectura</h2>, menu: { etiqueta: "Agenda", orden: 0 } },
  { ruta: "/escritura", componente: () => <button type="button">Nuevo turno</button>, roles: ["ADMIN"], menu: { etiqueta: "Gestión", orden: 1 } },
  { ruta: "/reservar", componente: () => <h2>Reserva pública</h2>, ambito: "publico", menu: { etiqueta: "Reserva", orden: 2 } },
] } });

function montar(ruta: string, rol?: Rol) {
  const fetch = vi.fn().mockResolvedValue(Response.json({ id: 1, rol, email: "personal@example.test" }));
  vi.stubGlobal("fetch", fetch);
  render(<MemoryRouter initialEntries={[ruta]}><AppRutas registro={registro} /></MemoryRouter>);
  return fetch;
}

afterEach(() => { vi.unstubAllGlobals(); });

it.each(["ADMIN", "RECEPCION", "ODONTOLOGO"] as const)("El menú y el acceso directo respetan el mismo permiso", async (rol) => {
  montar("/escritura", rol);
  const menu = await screen.findByRole("navigation");
  const enlaces = within(menu).getAllByRole("link").map((enlace) => enlace.textContent);
  if (rol === "ADMIN") {
    expect(enlaces).toEqual(["Agenda", "Gestión"]);
    expect(within(menu).getByRole("link", { name: "Gestión" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("button", { name: "Nuevo turno" })).toBeVisible();
  } else {
    expect(enlaces).toEqual(["Agenda"]);
    expect(screen.getByText("No tenés permiso")).toBeVisible();
    expect(screen.queryByRole("button", { name: "Nuevo turno" })).not.toBeInTheDocument();
  }
});

it("El odontólogo ve lectura pero no acciones operativas de escritura", async () => {
  montar("/", "ODONTOLOGO");
  expect(await screen.findByRole("heading", { name: "Agenda de lectura" })).toBeVisible();
  expect(screen.queryByRole("link", { name: "Gestión" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Nuevo turno" })).not.toBeInTheDocument();
});

it("La ruta pública se abre sin comprobar la sesión ni mostrar navegación interna", () => {
  const fetch = montar("/reservar");
  expect(screen.getByRole("heading", { name: "Reserva pública" })).toBeVisible();
  expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
  expect(fetch).not.toHaveBeenCalled();
});
