import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import { PaginaLogin } from "./PaginaLogin";
import { ProveedorSesion } from "./ProveedorSesion";
import { rutas } from "./routes";

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

async function montar(responder: () => Promise<Response>) {
  document.cookie = "XSRF-TOKEN=token-ficticio; Path=/";
  const fetch = vi.fn().mockResolvedValueOnce(new Response(null, { status: 401 })).mockImplementation(responder);
  vi.stubGlobal("fetch", fetch);
  render(<MemoryRouter initialEntries={["/login"]}><ProveedorSesion><Routes>
    {rutas.map(({ ruta, componente: Componente }) => <Route key={ruta} path={ruta} element={<Componente />} />)}
    <Route path="/" element={<p>Pantalla de inicio</p>} />
  </Routes></ProveedorSesion></MemoryRouter>);
  fireEvent.change(await screen.findByLabelText("Email"), { target: { value: "personal@example.test" } });
  fireEvent.change(screen.getByLabelText("Contraseña"), { target: { value: "ficticia" } });
  return fetch;
}

const enviar = () => fireEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));
const identidad = { id: 1, email: "personal@example.test", rol: "ADMIN" };

it("El personal inicia sesión y llega a Inicio", async () => {
  const fetch = await montar(async () => Response.json(identidad));
  enviar();
  expect(await screen.findByText("Pantalla de inicio")).toBeInTheDocument();
  expect(fetch).toHaveBeenLastCalledWith("/api/auth/login", expect.objectContaining({
    method: "POST", credentials: "same-origin", headers: expect.any(Headers),
  }));
  expect(fetch.mock.calls.at(-1)?.[1].headers.get("X-XSRF-TOKEN")).toBe("token-ficticio");
  expect(localStorage).toHaveLength(0);
  expect(sessionStorage).toHaveLength(0);
});

it("Un envío pendiente no duplica el login", async () => {
  let resolver!: (respuesta: Response) => void;
  const fetch = await montar(() => new Promise((r) => { resolver = r; }));
  enviar();
  fireEvent.submit(screen.getByRole("form"));
  await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2));
  await act(async () => resolver(Response.json(identidad)));
});

it("Todos los rechazos de credenciales muestran el mismo mensaje", async () => {
  const fetch = await montar(async () => Response.json({ detail: "detalle interno" }, { status: 401 }));
  for (const causa of ["incorrecta", "inexistente", "inactiva", "bloqueada"]) {
    fetch.mockResolvedValueOnce(Response.json({ detail: causa }, { status: 401 }));
    enviar();
    expect(await screen.findByRole("alert")).toHaveTextContent("No pudimos iniciar sesión. Verificá tus credenciales");
    expect(screen.getByText("Si ya hiciste varios intentos, esperá unos minutos antes de volver a probar.")).toBeVisible();
    expect(screen.queryByText(causa)).not.toBeInTheDocument();
  }
});

it.each([403, 500, "red"])("Un fallo técnico de ingreso no se presenta como credenciales inválidas", async (fallo) => {
  await montar(async () => {
    if (typeof fallo === "string") throw new Error("Red caída");
    return new Response(null, { status: fallo });
  });
  enviar();
  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos completar el ingreso. Intentá de nuevo.");
  expect(alerta).not.toHaveTextContent("Verificá tus credenciales");
  expect(screen.getByRole("button", { name: "Iniciar sesión" })).toBeEnabled();
});

it("La pantalla de login es la ruta registrada por autenticación", () => {
  expect(rutas).toContainEqual(expect.objectContaining({ ruta: "/login", componente: PaginaLogin }));
});
