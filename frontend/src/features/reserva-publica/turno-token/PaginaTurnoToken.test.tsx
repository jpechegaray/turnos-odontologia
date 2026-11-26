import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../../app/AppRutas";
import { rutas } from "../routes";

const TOKEN = "tok-ficticio_ABC123";
const fetchSimulado = vi.fn<Parameters<typeof fetch>, ReturnType<typeof fetch>>();
const consultar = vi.fn<[], Promise<Response>>();
const confirmar = vi.fn<[], Promise<Response>>();
const cancelar = vi.fn<[], Promise<Response>>();
const turno = {
  inicio: "2026-12-01T13:00:00Z",
  fin: "2026-12-01T13:30:00Z",
  estado: "PROGRAMADO",
  profesional: "Ana Ficticia",
  prestacion: "Consulta",
  cancelacionSeriaTardia: false,
};

function json(cuerpo: unknown, status = 200) {
  return new Response(JSON.stringify(cuerpo), { status, headers: { "Content-Type": "application/json" } });
}

function problema(status: number, codigo?: string) {
  return new Response(JSON.stringify({ status, title: "Error", detail: "Error", codigo }), {
    status, headers: { "Content-Type": "application/problem+json" },
  });
}

function abrirTurno() {
  return render(
    <MemoryRouter initialEntries={[`/turno/${TOKEN}`]}><AppRutas registro={{ rutas, menu: [] }} /></MemoryRouter>,
  );
}

async function turnoCargado() {
  abrirTurno();
  await screen.findByRole("heading", { name: "Tu turno" });
}

beforeEach(() => {
  consultar.mockImplementation(async () => json(turno));
  confirmar.mockImplementation(async () => json({ ...turno, estado: "CONFIRMADO" }));
  cancelar.mockImplementation(async () => new Response(null, { status: 204 }));
  fetchSimulado.mockImplementation(async (entrada, opciones) => {
    const url = new URL(String(entrada), "http://localhost");
    const base = `/api/public/turnos/${TOKEN}`;
    if (url.pathname === base && (opciones?.method ?? "GET") === "GET") return consultar();
    if (url.pathname === `${base}/confirmar` && opciones?.method === "POST") return confirmar();
    if (url.pathname === `${base}/cancelar` && opciones?.method === "POST") return cancelar();
    throw new Error("Ruta inesperada en la prueba");
  });
  vi.stubGlobal("fetch", fetchSimulado);
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.resetAllMocks();
  localStorage.clear();
  sessionStorage.clear();
});

it("El enlace válido muestra el turno sin pedir sesión", async () => {
  await turnoCargado();
  expect(screen.getByText(/10:00/)).toBeInTheDocument();
  expect(screen.getByText(/Ana Ficticia/)).toBeInTheDocument();
  expect(screen.getByText(/Consulta/)).toBeInTheDocument();
  expect(screen.getByText("Reservado")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /reprogramar/i })).not.toBeInTheDocument();
  expect(fetchSimulado.mock.calls.every(([url]) => String(url).startsWith("/api/public/"))).toBe(true);
});

it("Un turno cancelado o iniciado no ofrece acciones", async () => {
  consultar.mockResolvedValueOnce(json({ ...turno, estado: "ATENDIDO" }));
  await turnoCargado();
  expect(screen.getByText("Atendido")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Confirmar asistencia" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Cancelar turno" })).not.toBeInTheDocument();
  cleanup();
  consultar.mockResolvedValueOnce(json({ ...turno, estado: "CANCELADO" }));
  await turnoCargado();
  expect(screen.getByText("Cancelado")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Confirmar asistencia" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Cancelar turno" })).not.toBeInTheDocument();
});

it("Un token inválido muestra la pantalla genérica sin pistas", async () => {
  consultar.mockResolvedValueOnce(problema(404, "TOKEN_NO_ENCONTRADO"));
  abrirTurno();
  expect(await screen.findByRole("heading", { name: "Este enlace no está disponible" })).toBeInTheDocument();
  expect(document.body.textContent).not.toContain(TOKEN);
  expect(document.body.textContent).not.toMatch(/venci|revoc|inexistente/i);
});

it("Un token que vence durante la gestión muestra la pantalla genérica", async () => {
  confirmar.mockResolvedValueOnce(problema(404));
  await turnoCargado();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar asistencia" }));
  expect(await screen.findByRole("heading", { name: "Este enlace no está disponible" })).toBeInTheDocument();
  expect(screen.queryByRole("heading", { name: "Tu turno" })).not.toBeInTheDocument();
  cleanup();
  cancelar.mockResolvedValueOnce(problema(404));
  await turnoCargado();
  fireEvent.click(screen.getByRole("button", { name: "Cancelar turno" }));
  fireEvent.click(screen.getByRole("button", { name: "Sí, cancelar" }));
  expect(await screen.findByRole("heading", { name: "Este enlace no está disponible" })).toBeInTheDocument();
});

it("Confirmar pasa el turno a confirmado", async () => {
  await turnoCargado();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar asistencia" }));
  expect(await screen.findByText("Confirmado")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Confirmar asistencia" })).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Cancelar turno" })).toBeInTheDocument();
});

it("Un segundo clic en confirmar no repite el pedido", async () => {
  let responder: ((respuesta: Response) => void) | undefined;
  confirmar.mockImplementationOnce(() => new Promise((resolver) => { responder = resolver; }));
  await turnoCargado();
  const boton = screen.getByRole("button", { name: "Confirmar asistencia" });
  fireEvent.click(boton);
  fireEvent.click(boton);
  responder?.(json({ ...turno, estado: "CONFIRMADO" }));
  await screen.findByText("Confirmado");
  expect(confirmar).toHaveBeenCalledTimes(1);
});

it("Cancelar a tiempo pide confirmación y cancela el turno", async () => {
  await turnoCargado();
  fireEvent.click(screen.getByRole("button", { name: "Cancelar turno" }));
  expect(cancelar).not.toHaveBeenCalled();
  expect(screen.queryByText(/tardía/)).not.toBeInTheDocument();
  const si = screen.getByRole("button", { name: "Sí, cancelar" });
  fireEvent.click(si);
  fireEvent.click(si);
  expect(await screen.findByText("Tu turno quedó cancelado.")).toBeInTheDocument();
  expect(screen.getByText("Cancelado")).toBeInTheDocument();
  expect(cancelar).toHaveBeenCalledTimes(1);
  expect(consultar).toHaveBeenCalledTimes(1);
  expect(screen.queryByRole("button", { name: "Cancelar turno" })).not.toBeInTheDocument();
});

it("Una cancelación tardía avisa antes de confirmar", async () => {
  consultar.mockResolvedValueOnce(json({ ...turno, cancelacionSeriaTardia: true }));
  await turnoCargado();
  fireEvent.click(screen.getByRole("button", { name: "Cancelar turno" }));
  expect(screen.getByText("Esta cancelación es tardía.")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Sí, cancelar" })).toBeInTheDocument();
  expect(cancelar).not.toHaveBeenCalled();
});

it("Desistir de la cancelación no envía el pedido", async () => {
  await turnoCargado();
  fireEvent.click(screen.getByRole("button", { name: "Cancelar turno" }));
  fireEvent.click(screen.getByRole("button", { name: "Volver" }));
  expect(cancelar).not.toHaveBeenCalled();
  expect(screen.getByText("Reservado")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Cancelar turno" })).toBeInTheDocument();
});

it("El límite de solicitudes permite reintentar la acción", async () => {
  confirmar.mockResolvedValueOnce(problema(429));
  await turnoCargado();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar asistencia" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(/Intentá de nuevo/);
  expect(screen.getByRole("heading", { name: "Tu turno" })).toBeInTheDocument();
  expect(confirmar).toHaveBeenCalledTimes(1);
  fireEvent.click(screen.getByRole("button", { name: "Confirmar asistencia" }));
  expect(await screen.findByText("Confirmado")).toBeInTheDocument();
  expect(confirmar).toHaveBeenCalledTimes(2);
  cancelar.mockResolvedValueOnce(problema(429));
  fireEvent.click(screen.getByRole("button", { name: "Cancelar turno" }));
  fireEvent.click(screen.getByRole("button", { name: "Sí, cancelar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(/Intentá de nuevo/);
  fireEvent.click(screen.getByRole("button", { name: "Sí, cancelar" }));
  expect(await screen.findByText("Tu turno quedó cancelado.")).toBeInTheDocument();
  expect(cancelar).toHaveBeenCalledTimes(2);
});

it("El token no queda en el almacenamiento del navegador", async () => {
  await turnoCargado();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar asistencia" }));
  await screen.findByText("Confirmado");
  fireEvent.click(screen.getByRole("button", { name: "Cancelar turno" }));
  fireEvent.click(screen.getByRole("button", { name: "Sí, cancelar" }));
  await screen.findByText("Tu turno quedó cancelado.");
  await waitFor(() => expect(cancelar).toHaveBeenCalled());
  const guardado = [
    ...Object.keys(localStorage), ...Object.values(localStorage),
    ...Object.keys(sessionStorage), ...Object.values(sessionStorage),
    document.cookie, document.title,
  ].join(" ");
  expect(guardado).not.toContain(TOKEN);
  expect(document.body.textContent).not.toContain(TOKEN);
});
