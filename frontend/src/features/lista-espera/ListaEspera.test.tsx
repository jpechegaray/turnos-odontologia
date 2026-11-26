import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../app/AppRutas";
import { construirRegistro } from "../../app/rutas/registro";
import type { Rol } from "../../app/rutas/tipos";
import { simularSesionActiva, type SolicitudSimulada } from "../../app/sesion.fixture";
import { rutas } from "./routes";

type Entrada = {
  id: number; pacienteId: number; prestacionId: number; profesionalId: number | null;
  fechaDesde: string; fechaHasta: string; franjaDesde: string | null; franjaHasta: string | null;
  estado: "ACTIVA" | "ASIGNADA" | "CANCELADA" | "VENCIDA"; turnoAsignadoId: number | null; creadoEn: string;
};

const registro = construirRegistro({ listaEspera: { rutas } });

const PACIENTES = [
  { id: 5, nombre: "Sol", apellido: "Ficticia", dni: "10000001", email: "sol@example.test", activo: true },
  { id: 6, nombre: "Luz", apellido: "Inventada", dni: "10000002", email: null, activo: true },
];
const PRESTACIONES = [
  { id: 1, nombre: "Consulta", duracionDefaultMin: 30, activa: true },
  { id: 2, nombre: "Limpieza", duracionDefaultMin: 45, activa: true },
];
const PROFESIONALES = [
  { id: 3, nombre: "Ana", apellido: "Ficticia", matricula: "M-1", especialidad: null, activo: true },
  { id: 4, nombre: "Luis", apellido: "Ficticio", matricula: "M-2", especialidad: null, activo: true },
];

function entrada(extra: Partial<Entrada> = {}): Entrada {
  return {
    id: 10, pacienteId: 5, prestacionId: 1, profesionalId: 3, fechaDesde: "2026-12-01", fechaHasta: "2026-12-20",
    franjaDesde: "09:00", franjaHasta: "12:00", estado: "ACTIVA", turnoAsignadoId: null,
    creadoEn: "2026-11-25T12:00:00Z", ...extra,
  };
}

function problema(status: number, codigo?: string, extra: Record<string, unknown> = {}) {
  return new Response(JSON.stringify({ status, title: "Error", detail: "Error", codigo, ...extra }), {
    status, headers: { "Content-Type": "application/problem+json" },
  });
}

let entradas: Entrada[];
let compatibles: Entrada[];
const alta = vi.fn<[SolicitudSimulada], Response | Promise<Response>>();
const baja = vi.fn<[SolicitudSimulada], Response | Promise<Response>>();
const asignar = vi.fn<[SolicitudSimulada], Response | Promise<Response>>();
let solicitudes: SolicitudSimulada[];

function responder(solicitud: SolicitudSimulada): Response | Promise<Response> {
  solicitudes.push(solicitud);
  const url = new URL(solicitud.url, "http://localhost");
  const { pathname } = url;
  if (pathname === "/api/pacientes") return Response.json(PACIENTES);
  if (pathname === "/api/prestaciones") return Response.json(PRESTACIONES);
  if (pathname === "/api/profesionales") return Response.json(PROFESIONALES);
  if (pathname === "/api/lista-espera/compatibles") return Response.json(compatibles);
  if (pathname === "/api/lista-espera" && solicitud.metodo === "GET") {
    const estado = url.searchParams.get("estado");
    return Response.json(entradas.filter((e) => !estado || e.estado === estado));
  }
  if (pathname === "/api/lista-espera" && solicitud.metodo === "POST") return alta(solicitud);
  if (/^\/api\/lista-espera\/\d+$/u.test(pathname) && solicitud.metodo === "DELETE") return baja(solicitud);
  if (/^\/api\/lista-espera\/\d+\/asignar$/u.test(pathname)) return asignar(solicitud);
  return Response.json({});
}

function montar(rol: Rol = "RECEPCION") {
  simularSesionActiva(rol, responder);
  render(<MemoryRouter initialEntries={["/lista-espera"]}><AppRutas registro={registro} /></MemoryRouter>);
}

function pedidos(metodo: string, prefijo: string) {
  return solicitudes.filter((s) => s.metodo === metodo && s.url.startsWith(prefijo));
}

async function tablaEntradas() {
  return screen.findByRole("table", { name: "Entradas de la lista de espera" });
}

async function pantallaLista() {
  montar();
  const tabla = await tablaEntradas();
  await within(tabla).findByText("Sol Ficticia");
  return tabla;
}

function seccion(nombre: string) {
  return within(screen.getByRole("region", { name: nombre }));
}

function abrirAccion(nombre: "Anotar paciente" | "Asignar hueco") {
  const boton = screen.getByRole("button", { name: nombre });
  if (boton.getAttribute("aria-expanded") === "false") fireEvent.click(boton);
}

async function elegirPaciente() {
  abrirAccion("Anotar paciente");
  const alta = seccion("Anotar paciente");
  fireEvent.change(alta.getByLabelText("Dato del paciente"), { target: { value: "Ficticia" } });
  fireEvent.click(alta.getByRole("button", { name: "Buscar paciente" }));
  fireEvent.change(await alta.findByLabelText("Paciente"), { target: { value: "5" } });
}

function completarAlta(valores: Partial<Record<"Prestación" | "Profesional preferido" | "Desde" | "Hasta" | "Franja desde" | "Franja hasta", string>>) {
  const alta = seccion("Anotar paciente");
  for (const [etiqueta, value] of Object.entries(valores)) {
    fireEvent.change(alta.getByLabelText(etiqueta), { target: { value } });
  }
}

function enviarAlta() {
  abrirAccion("Anotar paciente");
  fireEvent.click(seccion("Anotar paciente").getByRole("button", { name: "Anotar en la lista" }));
}

function completarHueco(valores: Partial<Record<"Profesional del hueco" | "Fecha del hueco" | "Hora de inicio" | "Hora de fin", string>>) {
  abrirAccion("Asignar hueco");
  const hueco = seccion("Asignar un hueco");
  for (const [etiqueta, value] of Object.entries(valores)) {
    fireEvent.change(hueco.getByLabelText(etiqueta), { target: { value } });
  }
}

async function buscarCompatibles() {
  completarHueco({ "Profesional del hueco": "3", "Fecha del hueco": "2026-12-03", "Hora de inicio": "10:00", "Hora de fin": "10:30" });
  fireEvent.click(seccion("Asignar un hueco").getByRole("button", { name: "Buscar compatibles" }));
  return screen.findByRole("table", { name: "Entradas compatibles con el hueco" });
}

beforeEach(() => {
  entradas = [entrada(), entrada({ id: 11, pacienteId: 6, prestacionId: 2, profesionalId: null, franjaDesde: null, franjaHasta: null })];
  compatibles = [entrada()];
  solicitudes = [];
  alta.mockImplementation(({ cuerpo }) => {
    const nueva = entrada({ id: 20, ...(cuerpo as Partial<Entrada>), estado: "ACTIVA" });
    entradas = [...entradas, nueva];
    return Response.json(nueva, { status: 201 });
  });
  baja.mockImplementation(({ url }) => {
    const id = Number(url.split("/").pop());
    entradas = entradas.map((e) => (e.id === id ? { ...e, estado: "CANCELADA" } : e));
    return new Response(null, { status: 204 });
  });
  asignar.mockImplementation(({ url }) => {
    const id = Number(url.split("/").at(-2));
    entradas = entradas.map((e) => (e.id === id ? { ...e, estado: "ASIGNADA", turnoAsignadoId: 99 } : e));
    compatibles = compatibles.filter((e) => e.id !== id);
    return Response.json({ id: 99 }, { status: 201 });
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.resetAllMocks();
});

// --- Acceso ---

it("Recepción ve la lista de espera en el menú", async () => {
  montar("RECEPCION");
  const navegacion = await screen.findByRole("navigation", { name: "Principal" });
  expect(within(navegacion).getByRole("link", { name: "Lista de espera" })).toHaveAttribute("href", "/lista-espera");
  expect(await screen.findByRole("heading", { name: "Lista de espera" })).toBeInTheDocument();
});

it("El odontólogo no accede a la lista de espera", async () => {
  montar("ODONTOLOGO");
  expect(await screen.findByRole("alert")).toHaveTextContent("No tenés permiso");
  const navegacion = screen.getByRole("navigation", { name: "Principal" });
  expect(within(navegacion).queryByRole("link", { name: "Lista de espera" })).not.toBeInTheDocument();
  expect(pedidos("GET", "/api/lista-espera")).toHaveLength(0);
});

// --- Listado ---

it("El listado muestra las entradas activas con nombres", async () => {
  const tabla = await pantallaLista();
  expect(pedidos("GET", "/api/lista-espera")).not.toHaveLength(0);
  const fila = within(within(tabla).getByText("Sol Ficticia").closest("tr") as HTMLElement);
  expect(fila.getByText("Consulta")).toBeInTheDocument();
  expect(fila.getByText("Ana Ficticia")).toBeInTheDocument();
  expect(fila.getByText("01/12/2026 al 20/12/2026")).toBeInTheDocument();
  expect(fila.getByText("09:00 a 12:00")).toBeInTheDocument();
  expect(fila.getByText("Activa")).toBeInTheDocument();
  const otra = within(within(tabla).getByText("Luz Inventada").closest("tr") as HTMLElement);
  expect(otra.getByText("Cualquiera")).toBeInTheDocument();
  expect(otra.getByText("Sin preferencia")).toBeInTheDocument();
});

it("Cambiar el filtro de estado consulta ese estado", async () => {
  entradas = [...entradas, entrada({ id: 12, pacienteId: 6, estado: "ASIGNADA", turnoAsignadoId: 7 })];
  const tabla = await pantallaLista();
  fireEvent.click(seccion("Entradas").getByRole("button", { name: /^Asignadas/ }));
  expect(await within(tabla).findByText("Asignada")).toBeInTheDocument();
  expect(pedidos("GET", "/api/lista-espera")).toHaveLength(1);
  expect(within(tabla).queryByText("Sol Ficticia")).not.toBeInTheDocument();
});

it("Un estado sin entradas lo informa", async () => {
  const tabla = await pantallaLista();
  fireEvent.click(seccion("Entradas").getByRole("button", { name: /^Vencidas/ }));
  expect(await within(tabla).findByText("No hay entradas en este estado.")).toBeInTheDocument();
});

// --- Baja ---

it("Dar de baja una entrada la quita de las activas", async () => {
  const tabla = await pantallaLista();
  const fila = within(within(tabla).getByText("Sol Ficticia").closest("tr") as HTMLElement);
  fireEvent.click(fila.getByRole("button", { name: "Dar de baja" }));
  const confirmar = fila.getByRole("button", { name: "Confirmar baja" });
  fireEvent.click(confirmar);
  fireEvent.click(confirmar);
  await waitFor(() => expect(within(tabla).queryByText("Sol Ficticia")).not.toBeInTheDocument());
  expect(baja).toHaveBeenCalledTimes(1);
  expect(baja.mock.calls[0][0].url).toBe("/api/lista-espera/10");
});

it("Desistir de la baja no envía el pedido", async () => {
  const tabla = await pantallaLista();
  const fila = within(within(tabla).getByText("Sol Ficticia").closest("tr") as HTMLElement);
  fireEvent.click(fila.getByRole("button", { name: "Dar de baja" }));
  fireEvent.click(fila.getByRole("button", { name: "Volver" }));
  expect(baja).not.toHaveBeenCalled();
  expect(fila.getByRole("button", { name: "Dar de baja" })).toBeInTheDocument();
});

it("Dar de baja una entrada que ya no está activa recarga el listado", async () => {
  baja.mockImplementationOnce(() => {
    entradas = entradas.map((e) => (e.id === 10 ? { ...e, estado: "ASIGNADA" } : e));
    return problema(409, "ENTRADA_NO_ACTIVA");
  });
  const tabla = await pantallaLista();
  const consultasPrevias = pedidos("GET", "/api/lista-espera").length;
  const fila = within(within(tabla).getByText("Sol Ficticia").closest("tr") as HTMLElement);
  fireEvent.click(fila.getByRole("button", { name: "Dar de baja" }));
  fireEvent.click(fila.getByRole("button", { name: "Confirmar baja" }));
  expect(await screen.findByText("La entrada ya no estaba activa. Actualizamos el listado.")).toBeInTheDocument();
  await waitFor(() => expect(within(tabla).queryByText("Sol Ficticia")).not.toBeInTheDocument());
  expect(pedidos("GET", "/api/lista-espera").length).toBeGreaterThan(consultasPrevias);
});

// --- Alta ---

it("Los botones de lista de espera abren su formulario", async () => {
  await pantallaLista();
  const anotar = screen.getByRole("button", { name: "Anotar paciente" });
  const asignar = screen.getByRole("button", { name: "Asignar hueco" });
  expect(screen.queryByRole("region", { name: "Anotar paciente" })).not.toBeInTheDocument();
  expect(screen.queryByRole("region", { name: "Asignar un hueco" })).not.toBeInTheDocument();

  fireEvent.click(anotar);
  expect(anotar).toHaveAttribute("aria-expanded", "true");
  expect(seccion("Anotar paciente").getByLabelText("Dato del paciente")).toBeVisible();

  fireEvent.click(asignar);
  expect(asignar).toHaveAttribute("aria-expanded", "true");
  expect(seccion("Asignar un hueco").getByLabelText("Fecha del hueco")).toBeVisible();
  expect(screen.queryByRole("region", { name: "Anotar paciente" })).not.toBeInTheDocument();

  fireEvent.click(asignar);
  expect(asignar).toHaveAttribute("aria-expanded", "false");
  expect(screen.queryByRole("region", { name: "Asignar un hueco" })).not.toBeInTheDocument();
});

it("Un alta válida agrega la entrada al listado", async () => {
  entradas = [];
  montar();
  await within(await tablaEntradas()).findByText("No hay entradas en este estado.");
  await elegirPaciente();
  completarAlta({ Prestación: "2", "Profesional preferido": "4", Hasta: "2026-12-31", "Franja desde": "14:00", "Franja hasta": "18:00" });
  enviarAlta();
  enviarAlta();
  expect(await screen.findByText("El paciente quedó anotado en la lista de espera.")).toBeInTheDocument();
  expect(alta).toHaveBeenCalledTimes(1);
  expect(alta.mock.calls[0][0].cuerpo).toEqual({
    pacienteId: 5, prestacionId: 2, profesionalId: 4, fechaDesde: null, fechaHasta: "2026-12-31",
    franjaDesde: "14:00", franjaHasta: "18:00",
  });
  const tabla = await tablaEntradas();
  expect(await within(tabla).findByText("Limpieza")).toBeInTheDocument();
  expect(within(tabla).getByText("Luis Ficticio")).toBeInTheDocument();
});

it("El alta sin datos obligatorios no se envía", async () => {
  await pantallaLista();
  enviarAlta();
  const alta = seccion("Anotar paciente");
  expect(await alta.findByText("Elegí un paciente.")).toBeInTheDocument();
  expect(alta.getByText("Elegí una prestación.")).toBeInTheDocument();
  expect(alta.getByText("Indicá hasta qué fecha.")).toBeInTheDocument();
  expect(pedidos("POST", "/api/lista-espera")).toHaveLength(0);
});

it("Un rango de fechas invertido no se envía", async () => {
  await pantallaLista();
  await elegirPaciente();
  completarAlta({ Prestación: "1", Desde: "2026-12-20", Hasta: "2026-12-10" });
  enviarAlta();
  expect(await screen.findByText("La fecha de fin no puede ser anterior a la de inicio.")).toBeInTheDocument();
  expect(seccion("Anotar paciente").getByLabelText("Hasta")).toHaveAttribute("aria-invalid", "true");
  expect(alta).not.toHaveBeenCalled();
});

it("Una franja incompleta o invertida no se envía", async () => {
  await pantallaLista();
  await elegirPaciente();
  completarAlta({ Prestación: "1", Hasta: "2026-12-10", "Franja desde": "10:00" });
  enviarAlta();
  expect(await screen.findByText("Completá las dos horas de la franja o ninguna.")).toBeInTheDocument();
  completarAlta({ "Franja hasta": "09:00" });
  enviarAlta();
  expect(await screen.findByText("La hora de fin debe ser posterior a la de inicio.")).toBeInTheDocument();
  expect(alta).not.toHaveBeenCalled();
});

it("Una entrada activa duplicada se informa sin perder el formulario", async () => {
  alta.mockImplementationOnce(() => problema(409, "ENTRADA_ACTIVA_DUPLICADA"));
  await pantallaLista();
  await elegirPaciente();
  completarAlta({ Prestación: "1", Hasta: "2026-12-10" });
  enviarAlta();
  expect(await screen.findByText("El paciente ya está anotado para esa prestación.")).toBeInTheDocument();
  const formulario = seccion("Anotar paciente");
  expect(formulario.getByLabelText("Prestación")).toHaveValue("1");
  expect(formulario.getByLabelText("Hasta")).toHaveValue("2026-12-10");
  expect(formulario.getByLabelText("Paciente")).toHaveValue("5");
});

it("Un paciente sin email no se puede anotar", async () => {
  alta.mockImplementationOnce(() => problema(422, "PACIENTE_SIN_EMAIL"));
  await pantallaLista();
  await elegirPaciente();
  completarAlta({ Prestación: "1", Hasta: "2026-12-10" });
  enviarAlta();
  expect(await screen.findByText(/necesita un email para recibir el aviso/)).toBeInTheDocument();
  expect(seccion("Anotar paciente").getByLabelText("Hasta")).toHaveValue("2026-12-10");
});

// --- Sugerencias y asignación ---

it("Buscar un hueco lista las entradas compatibles", async () => {
  await pantallaLista();
  const tabla = await buscarCompatibles();
  expect(await within(tabla).findByText("Sol Ficticia")).toBeInTheDocument();
  expect(within(tabla).getByText("Consulta")).toBeInTheDocument();
  const consulta = pedidos("GET", "/api/lista-espera/compatibles")[0];
  const parametros = new URL(consulta.url, "http://localhost").searchParams;
  expect(parametros.get("profesionalId")).toBe("3");
  expect(parametros.get("inicio")).toBe("2026-12-03T13:00:00.000Z");
  expect(parametros.get("fin")).toBe("2026-12-03T13:30:00.000Z");
});

it("Un hueco sin compatibles lo informa", async () => {
  compatibles = [];
  await pantallaLista();
  const tabla = await buscarCompatibles();
  expect(await within(tabla).findByText("No hay entradas compatibles con este hueco.")).toBeInTheDocument();
});

it("Un hueco con horario invertido no se consulta", async () => {
  await pantallaLista();
  completarHueco({ "Profesional del hueco": "3", "Fecha del hueco": "2026-12-03", "Hora de inicio": "11:00", "Hora de fin": "10:00" });
  fireEvent.click(seccion("Asignar un hueco").getByRole("button", { name: "Buscar compatibles" }));
  expect(await screen.findByText("La hora de fin debe ser posterior a la de inicio.")).toBeInTheDocument();
  expect(pedidos("GET", "/api/lista-espera/compatibles")).toHaveLength(0);
});

it("Asignar el hueco crea el turno y cierra la entrada", async () => {
  const listado = await pantallaLista();
  const tabla = await buscarCompatibles();
  fireEvent.click(await within(tabla).findByRole("button", { name: "Asignar" }));
  expect(await screen.findByText("Se asignó el turno a Sol Ficticia.")).toBeInTheDocument();
  expect(asignar.mock.calls[0][0].cuerpo).toEqual({ profesionalId: 3, inicio: "2026-12-03T13:00:00.000Z" });
  await waitFor(() => expect(within(tabla).queryByText("Sol Ficticia")).not.toBeInTheDocument());
  await waitFor(() => expect(within(listado).queryByText("Sol Ficticia")).not.toBeInTheDocument());
});

it("Un segundo clic en asignar no repite el pedido", async () => {
  let responderAsignacion: ((respuesta: Response) => void) | undefined;
  asignar.mockImplementationOnce(() => new Promise((resolver) => { responderAsignacion = resolver; }));
  await pantallaLista();
  const tabla = await buscarCompatibles();
  const boton = await within(tabla).findByRole("button", { name: "Asignar" });
  fireEvent.click(boton);
  fireEvent.click(boton);
  await waitFor(() => expect(asignar).toHaveBeenCalledTimes(1));
  compatibles = [];
  responderAsignacion?.(Response.json({ id: 99 }, { status: 201 }));
  expect(await screen.findByText("Se asignó el turno a Sol Ficticia.")).toBeInTheDocument();
  expect(asignar).toHaveBeenCalledTimes(1);
});

it("Un horario ocupado al asignar se informa y recarga las sugerencias", async () => {
  asignar.mockImplementationOnce(() => problema(409, "RN-01", { recurso: "PROFESIONAL" }));
  await pantallaLista();
  const tabla = await buscarCompatibles();
  fireEvent.click(await within(tabla).findByRole("button", { name: "Asignar" }));
  expect(await screen.findByText(/ocupado en ese horario/)).toBeInTheDocument();
  await waitFor(() => expect(pedidos("GET", "/api/lista-espera/compatibles")).toHaveLength(2));
});

it("Asignar una entrada que ya no está activa se informa", async () => {
  asignar.mockImplementationOnce(() => problema(409, "ENTRADA_NO_ACTIVA"));
  await pantallaLista();
  const tabla = await buscarCompatibles();
  fireEvent.click(await within(tabla).findByRole("button", { name: "Asignar" }));
  expect(await screen.findByText("La entrada ya no está activa. Actualizamos las sugerencias.")).toBeInTheDocument();
  await waitFor(() => expect(pedidos("GET", "/api/lista-espera/compatibles")).toHaveLength(2));
});

it("Una entrada incompatible con el hueco se informa", async () => {
  asignar.mockImplementationOnce(() => problema(422, "ENTRADA_INCOMPATIBLE"));
  await pantallaLista();
  const tabla = await buscarCompatibles();
  fireEvent.click(await within(tabla).findByRole("button", { name: "Asignar" }));
  expect(await screen.findByText("La entrada no es compatible con ese hueco.")).toBeInTheDocument();
  expect(screen.queryByText(/Se asignó el turno/)).not.toBeInTheDocument();
});
