import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../app/AppRutas";
import { rutas } from "./routes";

const fetchSimulado = vi.fn<Parameters<typeof fetch>, ReturnType<typeof fetch>>();
const reservar = vi.fn<[], Promise<Response>>();
const consultarHorarios = vi.fn<[URL], Promise<Response>>();
const consultorio = {
  nombre: "Consultorio ficticio",
  emailContacto: "contacto@example.test",
  domicilio: "Calle de prueba 12",
  retencionAnios: 2,
  versionConsentimiento: "2026-11-v1",
};
const horario = { inicio: "2026-12-01T13:00:00Z", fin: "2026-12-01T13:30:00Z" };
const alternativa = { inicio: "2026-12-01T14:00:00Z", fin: "2026-12-01T14:30:00Z" };

function json(cuerpo: unknown, status = 200) {
  return new Response(JSON.stringify(cuerpo), { status, headers: { "Content-Type": "application/json" } });
}

function abrirReserva() {
  return render(<MemoryRouter initialEntries={["/reservar"]}><AppRutas registro={{ rutas, menu: [] }} /></MemoryRouter>);
}

async function irAHorarios() {
  abrirReserva();
  fireEvent.click(await screen.findByRole("button", { name: "Consulta" }));
  fireEvent.click(await screen.findByRole("button", { name: "Ana Ficticia" }));
}

async function irADatos() {
  await irAHorarios();
  fireEvent.click(await screen.findByRole("button", { name: /10:00/ }));
  await screen.findByRole("heading", { name: "Tus datos" });
}

function completarDatos() {
  for (const [etiqueta, value] of Object.entries({
    Nombre: "Sol", Apellido: "Ficticia", Email: "sol@example.test",
    Teléfono: "1100000000", "Obra social": "Particular",
  })) {
    fireEvent.change(screen.getByLabelText(etiqueta, { exact: true }), { target: { value } });
  }
}

async function aceptar() {
  const casilla = await screen.findByRole("checkbox", { name: /Leí y acepto/ });
  fireEvent.click(casilla);
}

function confirmar() {
  fireEvent.click(screen.getByRole("button", { name: "Reservar turno" }));
}

async function formularioListo() {
  await irADatos();
  completarDatos();
  await aceptar();
}

beforeEach(() => {
  reservar.mockImplementation(async () => json({
    ...horario, prestacion: "Consulta", profesional: "Ana Ficticia", token: "token-ficticio",
  }, 201));
  consultarHorarios.mockImplementation(async () => json([horario]));
  fetchSimulado.mockImplementation(async (entrada) => {
    const url = new URL(String(entrada), "http://localhost");
    switch (url.pathname) {
      case "/api/public/prestaciones": return json([{ id: 1, nombre: "Consulta" }, { id: 2, nombre: "Limpieza" }]);
      case "/api/public/profesionales": return json([
        { id: 3, nombre: "Ana", apellido: "Ficticia" }, { id: 4, nombre: "Luis", apellido: "Ficticio" },
      ]);
      case "/api/public/disponibilidad": return consultarHorarios(url);
      case "/api/public/consultorio": return json(consultorio);
      case "/api/public/reservas": return reservar();
      default: throw new Error("Ruta inesperada en la prueba");
    }
  });
  vi.stubGlobal("fetch", fetchSimulado);
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.resetAllMocks();
});

it("Una persona sin sesión puede elegir un horario desde el celular", async () => {
  await irADatos();
  expect(screen.getByText(/10:00/)).toBeInTheDocument();
  expect(screen.queryByLabelText(/DNI/i)).not.toBeInTheDocument();
  expect(fetchSimulado.mock.calls.every(([url]) => String(url).startsWith("/api/public/"))).toBe(true);
});

it("Cambiar prestación o profesional limpia las selecciones dependientes", async () => {
  let responder: ((respuesta: Response) => void) | undefined;
  consultarHorarios.mockImplementationOnce(() => new Promise((resolver) => { responder = resolver; }));
  await irAHorarios();
  await waitFor(() => expect(consultarHorarios).toHaveBeenCalledTimes(1));
  fireEvent.click(screen.getByRole("button", { name: "Atrás" }));
  fireEvent.click(await screen.findByRole("button", { name: "Luis Ficticio" }));
  await screen.findByRole("button", { name: /10:00/ });
  await act(async () => { responder?.(json([alternativa])); });
  expect(screen.queryByRole("button", { name: /11:00/ })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Atrás" }));
  fireEvent.click(screen.getByRole("button", { name: "Atrás" }));
  fireEvent.click(await screen.findByRole("button", { name: "Limpieza" }));
  expect(await screen.findByRole("heading", { name: "Elegí un profesional" })).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /10:00/ })).not.toBeInTheDocument();
});

it("Un rango sin horarios permite elegir otras fechas", async () => {
  consultarHorarios.mockResolvedValueOnce(json([]));
  await irAHorarios();
  expect(await screen.findByText(/No hay horarios/)).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Semana siguiente" }));
  expect(await screen.findByRole("button", { name: /10:00/ })).toBeInTheDocument();
  expect(consultarHorarios.mock.calls[1][0].searchParams.get("desde"))
    .not.toBe(consultarHorarios.mock.calls[0][0].searchParams.get("desde"));
});

it("El formulario exige aceptar el aviso antes de reservar", async () => {
  await irADatos();
  completarDatos();
  expect(await screen.findByRole("checkbox")).not.toBeChecked();
  expect(screen.getByText("Cómo usamos tus datos", { selector: "summary" })).toBeInTheDocument();
  confirmar();
  expect(await screen.findByText("Aceptá el tratamiento de tus datos para reservar.")).toBeInTheDocument();
  expect(reservar).not.toHaveBeenCalled();
});

it("Una versión de consentimiento vencida exige volver a aceptar", async () => {
  reservar.mockResolvedValueOnce(json({ codigo: "CONSENTIMIENTO_VERSION_NO_VIGENTE" }, 422));
  await formularioListo();
  confirmar();
  expect(await screen.findByText(/volvé a aceptar/i)).toBeInTheDocument();
  expect(await screen.findByRole("checkbox")).not.toBeChecked();
  expect(screen.getByLabelText("Email")).toHaveValue("sol@example.test");
  expect(fetchSimulado.mock.calls.filter(([url]) => String(url).endsWith("/consultorio"))).toHaveLength(2);
});

it("La pantalla bloquea el envío si falta el texto vigente", async () => {
  const habitual = fetchSimulado.getMockImplementation();
  fetchSimulado.mockImplementation((entrada, opciones) => String(entrada).endsWith("/consultorio")
    ? Promise.resolve(json({ ...consultorio, versionConsentimiento: "2026-12-v2" }))
    : habitual!(entrada, opciones));
  await irADatos();
  expect(await screen.findByText(/El consentimiento vigente no está disponible/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Reservar turno" })).toBeDisabled();
  expect(reservar).not.toHaveBeenCalled();
});

it("Los errores de validación se muestran junto al campo correspondiente", async () => {
  reservar.mockResolvedValueOnce(json({ errores: [{ campo: "telefono", mensaje: "Revisá el teléfono." }] }, 400));
  await formularioListo();
  confirmar();
  expect(await screen.findByText("Revisá el teléfono.")).toBeInTheDocument();
  expect(screen.getByLabelText("Teléfono")).toHaveAccessibleDescription("Revisá el teléfono.");
  expect(screen.getByLabelText("Teléfono")).toHaveValue("1100000000");
});

it("Un horario ocupado muestra alternativas y conserva el formulario", async () => {
  reservar.mockResolvedValueOnce(json({ codigo: "RN-01", alternativas: [alternativa] }, 409));
  await formularioListo();
  confirmar();
  fireEvent.click(await screen.findByRole("button", { name: /11:00/ }));
  expect(screen.getByLabelText("Email")).toHaveValue("sol@example.test");
  confirmar();
  await screen.findByRole("link", { name: "Gestionar mi turno" });
  const envios = fetchSimulado.mock.calls.filter(([url]) => String(url).endsWith("/reservas"));
  expect(JSON.parse(String(envios[1][1]?.body))).toMatchObject({ inicio: alternativa.inicio, email: "sol@example.test" });
});

it("Un conflicto sin alternativas permite volver a buscar", async () => {
  reservar.mockResolvedValueOnce(json({ codigo: "RN-01", alternativas: [] }, 409));
  await formularioListo();
  confirmar();
  fireEvent.click(await screen.findByRole("button", { name: "Buscar otros horarios" }));
  fireEvent.click(await screen.findByRole("button", { name: /10:00/ }));
  expect(screen.getByLabelText("Email")).toHaveValue("sol@example.test");
});

it("Una reserva confirmada muestra el resumen y el enlace", async () => {
  await formularioListo();
  confirmar();
  expect(await screen.findByRole("link", { name: "Gestionar mi turno" })).toHaveAttribute("href", "/turno/token-ficticio");
  expect(screen.getByText(/10:00/)).toBeInTheDocument();
  expect(screen.queryByLabelText("Email")).not.toBeInTheDocument();
});

it("Un segundo envío queda bloqueado mientras se confirma la reserva", async () => {
  let responder: ((respuesta: Response) => void) | undefined;
  reservar.mockImplementationOnce(() => new Promise((resolver) => { responder = resolver; }));
  await formularioListo();
  const boton = screen.getByRole("button", { name: "Reservar turno" });
  fireEvent.click(boton);
  fireEvent.click(boton);
  expect(reservar).toHaveBeenCalledTimes(1);
  responder?.(json({ ...horario, prestacion: "Consulta", profesional: "Ana Ficticia", token: "ficticio" }, 201));
  await screen.findByRole("link", { name: "Gestionar mi turno" });
});

it("El límite de solicitudes conserva el formulario y permite reintentar", async () => {
  reservar.mockResolvedValueOnce(json({}, 429));
  await formularioListo();
  confirmar();
  expect(await screen.findByText(/Demasiadas solicitudes/)).toBeInTheDocument();
  expect(screen.getByLabelText("Email")).toHaveValue("sol@example.test");
  confirmar();
  await screen.findByRole("link", { name: "Gestionar mi turno" });
});
