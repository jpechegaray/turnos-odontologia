import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../../app/AppRutas";
import { rutas } from "../routes";

const TOKEN_OFERTA = "oferta-ficticia_XYZ789";
const TOKEN_TURNO = "turno-nuevo_QRS456";
const fetchSimulado = vi.fn<Parameters<typeof fetch>, ReturnType<typeof fetch>>();
const consultar = vi.fn<[], Promise<Response>>();
const tomar = vi.fn<[], Promise<Response>>();
const oferta = {
  inicio: "2026-12-02T15:00:00Z",
  fin: "2026-12-02T15:30:00Z",
  profesional: "Ana Ficticia",
  prestacion: "Limpieza",
  estado: "DISPONIBLE",
};
const tomado = {
  inicio: oferta.inicio, fin: oferta.fin, estado: "PROGRAMADO",
  profesional: oferta.profesional, prestacion: oferta.prestacion, token: TOKEN_TURNO,
};

function json(cuerpo: unknown, status = 200) {
  return new Response(JSON.stringify(cuerpo), { status, headers: { "Content-Type": "application/json" } });
}

function problema(status: number, codigo?: string) {
  return new Response(JSON.stringify({ status, title: "Error", detail: "Error", codigo }), {
    status, headers: { "Content-Type": "application/problem+json" },
  });
}

function abrirOferta() {
  return render(
    <MemoryRouter initialEntries={[`/oferta/${TOKEN_OFERTA}`]}>
      <AppRutas registro={{ rutas, menu: [] }} />
    </MemoryRouter>,
  );
}

async function ofertaCargada() {
  abrirOferta();
  await screen.findByRole("heading", { name: "Se liberó un turno" });
}

beforeEach(() => {
  consultar.mockImplementation(async () => json(oferta));
  tomar.mockImplementation(async () => json(tomado, 201));
  fetchSimulado.mockImplementation(async (entrada, opciones) => {
    const url = new URL(String(entrada), "http://localhost");
    const base = `/api/public/ofertas/${TOKEN_OFERTA}`;
    if (url.pathname === base && (opciones?.method ?? "GET") === "GET") return consultar();
    if (url.pathname === `${base}/tomar` && opciones?.method === "POST") return tomar();
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

it("Una oferta disponible muestra el hueco y permite tomarlo", async () => {
  await ofertaCargada();
  expect(screen.getByText(/12:00/)).toBeInTheDocument();
  expect(screen.getByText("Ana Ficticia")).toBeInTheDocument();
  expect(screen.getByText("Limpieza")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Tomar este turno" })).toBeInTheDocument();
  expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
});

it("Una oferta tomada informa que el hueco ya fue tomado", async () => {
  consultar.mockResolvedValueOnce(json({ ...oferta, estado: "TOMADA" }));
  await ofertaCargada();
  expect(screen.getByText("Este turno ya fue tomado.")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Tomar este turno" })).not.toBeInTheDocument();
});

it("Una oferta vencida informa que el horario ya empezó", async () => {
  consultar.mockResolvedValueOnce(json({ ...oferta, estado: "VENCIDA" }));
  await ofertaCargada();
  expect(screen.getByText("Esta oferta venció: el horario ya empezó.")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Tomar este turno" })).not.toBeInTheDocument();
});

it("Una oferta no disponible lo informa sin ofrecer tomarla", async () => {
  consultar.mockResolvedValueOnce(json({ ...oferta, estado: "NO_DISPONIBLE" }));
  await ofertaCargada();
  expect(screen.getByText("Esta oferta ya no está disponible.")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Tomar este turno" })).not.toBeInTheDocument();
});

it("Un token de oferta inválido muestra la pantalla genérica", async () => {
  consultar.mockResolvedValueOnce(problema(404, "OFERTA_NO_ENCONTRADA"));
  abrirOferta();
  expect(await screen.findByRole("heading", { name: "Este enlace no está disponible" })).toBeInTheDocument();
  expect(document.body.textContent).not.toContain(TOKEN_OFERTA);
  expect(document.body.textContent).not.toMatch(/OFERTA_NO_ENCONTRADA|venci|tomad/i);
});

it("Tomar el hueco muestra el turno y su enlace", async () => {
  await ofertaCargada();
  fireEvent.click(screen.getByRole("button", { name: "Tomar este turno" }));
  expect(await screen.findByRole("heading", { name: "¡El turno es tuyo!" })).toBeInTheDocument();
  expect(screen.getByText(/12:00/)).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Gestionar mi turno" }))
    .toHaveAttribute("href", `/turno/${encodeURIComponent(TOKEN_TURNO)}`);
});

it("Un segundo clic en tomar no repite el pedido", async () => {
  let responder: ((respuesta: Response) => void) | undefined;
  tomar.mockImplementationOnce(() => new Promise((resolver) => { responder = resolver; }));
  await ofertaCargada();
  const boton = screen.getByRole("button", { name: "Tomar este turno" });
  fireEvent.click(boton);
  fireEvent.click(boton);
  responder?.(json(tomado, 201));
  await screen.findByRole("heading", { name: "¡El turno es tuyo!" });
  expect(tomar).toHaveBeenCalledTimes(1);
});

it("Llegar tarde a la toma informa que el hueco ya fue tomado", async () => {
  tomar.mockResolvedValueOnce(problema(409, "HUECO_YA_TOMADO"));
  await ofertaCargada();
  fireEvent.click(screen.getByRole("button", { name: "Tomar este turno" }));
  expect(await screen.findByText("Este turno ya fue tomado.")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Tomar este turno" })).not.toBeInTheDocument();
});

it("Tomar un hueco que ya empezó informa que venció", async () => {
  tomar.mockResolvedValueOnce(problema(409, "OFERTA_VENCIDA"));
  await ofertaCargada();
  fireEvent.click(screen.getByRole("button", { name: "Tomar este turno" }));
  expect(await screen.findByText("Esta oferta venció: el horario ya empezó.")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Tomar este turno" })).not.toBeInTheDocument();
});

it("Otro rechazo de la toma informa que la oferta no está disponible", async () => {
  for (const codigo of ["OFERTA_NO_DISPONIBLE", "CODIGO_DESCONOCIDO"]) {
    cleanup();
    tomar.mockResolvedValueOnce(problema(409, codigo));
    await ofertaCargada();
    fireEvent.click(screen.getByRole("button", { name: "Tomar este turno" }));
    expect(await screen.findByText("Esta oferta ya no está disponible.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Tomar este turno" })).not.toBeInTheDocument();
  }
});

it("El límite de solicitudes permite reintentar la toma", async () => {
  tomar.mockResolvedValueOnce(problema(429));
  await ofertaCargada();
  fireEvent.click(screen.getByRole("button", { name: "Tomar este turno" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(/Intentá de nuevo/);
  expect(screen.getByText("Limpieza")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Tomar este turno" }));
  expect(await screen.findByRole("heading", { name: "¡El turno es tuyo!" })).toBeInTheDocument();
  expect(tomar).toHaveBeenCalledTimes(2);
});

it("Los tokens de la oferta y del turno no quedan en el navegador", async () => {
  await ofertaCargada();
  fireEvent.click(screen.getByRole("button", { name: "Tomar este turno" }));
  await screen.findByRole("heading", { name: "¡El turno es tuyo!" });
  const guardado = [
    ...Object.keys(localStorage), ...Object.values(localStorage),
    ...Object.keys(sessionStorage), ...Object.values(sessionStorage),
    document.cookie, document.title,
  ].join(" ");
  expect(guardado).not.toContain(TOKEN_OFERTA);
  expect(guardado).not.toContain(TOKEN_TURNO);
  expect(document.body.textContent).not.toContain(TOKEN_OFERTA);
  expect(document.body.textContent).not.toContain(TOKEN_TURNO);
});
