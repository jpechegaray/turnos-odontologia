import { act, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, useLocation, useNavigate, useNavigationType } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../app/AppRutas";
import { construirRegistro } from "../../app/rutas/registro";
import { api } from "../../shared/api/clienteApi";
import { rutas } from "./routes";
import { useState } from "react";

function Pantalla({ publica = false }: { publica?: boolean }) {
  const [fallo, actualizar] = useState(false);
  const consultar = (ruta: string) => { void api.get(ruta).catch(() => actualizar(true)); };
  return <>
    <h2>{publica ? "Reserva" : "Inicio interno"}</h2>
    <button onClick={() => consultar(publica ? "/public/reservar" : "/turnos")}>Consultar</button>
    {publica ? null : <button onClick={() => consultar("/pendiente")}>Consulta pendiente</button>}
    {fallo ? <p>Consulta rechazada</p> : null}
  </>;
}

const registro = construirRegistro({ app: { rutas: [
  ...rutas,
  { ruta: "/", componente: Pantalla },
  { ruta: "/reservar", componente: () => <Pantalla publica />, ambito: "publico" },
] } });
const identidad = { id: 1, email: "personal@example.test", rol: "ADMIN" };

function HistorialDePrueba() {
  const navegar = useNavigate();
  const { pathname } = useLocation();
  const accion = useNavigationType();
  return <>
    <output aria-label="Última navegación">{pathname} {accion}</output>
    <button type="button" onClick={() => navegar(-1)}>Atrás</button>
  </>;
}

async function montar(responder: (ruta: string) => Response | Promise<Response>, ruta = "/") {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
  const fetch = vi.fn(async (url: string, _opciones?: RequestInit) => {
    void _opciones;
    return url === "/api/auth/me" ? Response.json(identidad) : responder(url);
  });
  vi.stubGlobal("fetch", fetch);
  // Resolver la consulta inicial simulada antes de buscar controles del layout.
  await act(async () => {
    render(<MemoryRouter initialEntries={["/", ruta]}><HistorialDePrueba /><AppRutas registro={registro} /></MemoryRouter>);
  });
  return fetch;
}

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

const pulsar = async (name: string) => fireEvent.click(await screen.findByRole("button", { name }));
const respuesta = (status: number) => new Response(null, { status });

it.each([204, 401])("Cerrar sesión lleva a login y no deja volver al contenido interno", async (status) => {
  const fetch = await montar(() => respuesta(status));
  fireEvent.click(screen.getByRole("button", { name: "Cerrar sesión" }));
  expect(await screen.findByLabelText("Email")).toBeVisible();
  const opciones = fetch.mock.calls.find(([ruta]) => ruta === "/api/auth/logout")?.[1];
  expect(opciones?.method).toBe("POST");
  expect(new Headers(opciones?.headers).get("X-XSRF-TOKEN")).toBe("ficticio");
  expect(screen.queryByText("Inicio interno")).not.toBeInTheDocument();
  await pulsar("Atrás");
  expect(await screen.findByLabelText("Email")).toBeVisible();
  expect(screen.queryByText("Inicio interno")).not.toBeInTheDocument();
});

it.each([403, 500])("Un fallo de logout permite reintentar sin anunciar el cierre", async (status) => {
  let veces = 0;
  await montar(() => respuesta(++veces === 1 ? status : 204));
  await pulsar("Cerrar sesión");
  expect(await screen.findByRole("alert")).toHaveTextContent("No pudimos confirmar el cierre");
  expect(screen.getByText("Inicio interno")).toBeVisible();
  await pulsar("Cerrar sesión");
  expect(await screen.findByLabelText("Email")).toBeVisible();
});

it("Una operación interna con sesión vencida lleva a login", async () => {
  await montar(() => respuesta(401));
  await pulsar("Consultar");
  expect(await screen.findByLabelText("Email")).toBeVisible();
  expect(screen.getByLabelText("Última navegación")).toHaveTextContent(/^\/login REPLACE$/);
  expect(screen.queryByText("Inicio interno")).not.toBeInTheDocument();
});

it("Un 403 interno no cierra la sesión", async () => {
  await montar(() => respuesta(403));
  await pulsar("Consultar");
  expect(await screen.findByText("Consulta rechazada")).toBeVisible();
  expect(screen.getByText("Inicio interno")).toBeVisible();
});

it("Una ruta pública no depende de la sesión del personal", async () => {
  const fetch = await montar(() => respuesta(401), "/reservar");
  await pulsar("Consultar");
  expect(await screen.findByText("Consulta rechazada")).toBeVisible();
  expect(screen.getByText("Reserva")).toBeVisible();
  expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
  expect(fetch.mock.calls.map(([ruta]) => ruta)).toEqual(["/api/public/reservar"]);
});

it("Un 401 de la sesión anterior no invalida un login nuevo", async () => {
  let completar!: (value: Response) => void;
  await montar((ruta) => {
    if (ruta === "/api/pendiente") return new Promise((r) => { completar = r; });
    return ruta === "/api/auth/login" ? Response.json(identidad) : respuesta(204);
  });
  await pulsar("Consulta pendiente");
  await pulsar("Cerrar sesión");
  fireEvent.change(await screen.findByLabelText("Email"), { target: { value: identidad.email } });
  fireEvent.change(screen.getByLabelText("Contraseña"), { target: { value: "ficticia" } });
  await pulsar("Iniciar sesión");
  expect(await screen.findByText("Inicio interno")).toBeVisible();
  await act(async () => completar(respuesta(401)));
  expect(screen.getByText("Inicio interno")).toBeVisible();
});
