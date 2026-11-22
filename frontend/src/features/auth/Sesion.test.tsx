import { act, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useNavigationType } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import { ProveedorSesion } from "./ProveedorSesion";
import { RequireSesion } from "./RequireSesion";

const identidad = { id: 1, email: "personal@example.test", rol: "ADMIN" };

function FormularioDePrueba() {
  const accion = useNavigationType();
  return <p>Formulario de ingreso: {accion}</p>;
}

function montar() {
  render(
    <MemoryRouter initialEntries={["/interna"]}>
      <ProveedorSesion>
        <Routes>
          <Route element={<RequireSesion />}>
            <Route path="/interna" element={<p>Contenido interno</p>} />
          </Route>
          <Route path="/login" element={<FormularioDePrueba />} />
        </Routes>
      </ProveedorSesion>
    </MemoryRouter>,
  );
}

afterEach(() => { vi.unstubAllGlobals(); });

it("Una recarga recupera la identidad desde el servidor", async () => {
  let resolver!: (respuesta: Response) => void;
  const fetch = vi.fn(() => new Promise<Response>((r) => { resolver = r; }));
  vi.stubGlobal("fetch", fetch);
  montar();
  expect(screen.getByRole("status")).toHaveTextContent("Comprobando");
  expect(screen.queryByText("Contenido interno")).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledWith("/api/auth/me", expect.objectContaining({ method: "GET" }));
  await act(async () => resolver(Response.json(identidad)));
  expect(screen.getByText("Contenido interno")).toBeInTheDocument();
});

it("Entrar al área interna sin sesión lleva a login", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));
  montar();
  expect(await screen.findByText("Formulario de ingreso: REPLACE")).toBeInTheDocument();
  expect(screen.queryByText("Contenido interno")).not.toBeInTheDocument();
});

it("Un fallo técnico al restaurar sesión permite reintentar", async () => {
  const fetch = vi.fn().mockRejectedValueOnce(new Error("Red caída")).mockResolvedValueOnce(Response.json(identidad));
  vi.stubGlobal("fetch", fetch);
  montar();
  expect(await screen.findByRole("alert")).toHaveTextContent("No pudimos comprobar");
  expect(screen.queryByText("Contenido interno")).not.toBeInTheDocument();
  expect(screen.queryByText(/Formulario de ingreso/)).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(await screen.findByText("Contenido interno")).toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(2);
});
