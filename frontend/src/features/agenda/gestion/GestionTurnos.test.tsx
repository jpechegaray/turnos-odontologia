import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../../app/AppRutas";
import { construirRegistro } from "../../../app/rutas/registro";
import type { Rol } from "../../../app/rutas/tipos";
import { simularSesionActiva, type SolicitudSimulada } from "../../../app/sesion.fixture";
import { ErrorApi } from "../../../shared/api/errorApi";
import { rutas } from "../routes";
import type { TurnoAgenda } from "../tipos";
import { mensajeErrorGestion } from "./acciones";

const FECHA = "2026-12-07";
const AHORA = Date.parse(`${FECHA}T12:00:00Z`);
const REGISTRO = construirRegistro({ agenda: { rutas } });

function turno(id: number, hora: string, estado: TurnoAgenda["estado"]): TurnoAgenda {
  const inicio = `${FECHA}T${hora}:00Z`;
  return {
    id, inicio, fin: new Date(Date.parse(inicio) + 1_800_000).toISOString(),
    estado, duracionMin: 30, profesionalId: 1, sillonId: 1, prestacionId: 1, pacienteId: id,
    paciente: { nombre: "Paciente", apellido: "Ficticio" }, notas: null,
  };
}

const FUTURO = turno(1, "12:15", "PROGRAMADO");
const INICIADO = turno(2, "12:00", "CONFIRMADO");
const ATENDIDO = turno(3, "11:00", "ATENDIDO");
const CANCELADO = turno(4, "11:30", "CANCELADO");

type Opciones = Readonly<{
  rol?: Rol;
  turnos?: readonly TurnoAgenda[];
  configuracion?: () => Response;
  escribir?: (solicitud: SolicitudSimulada, url: URL) => Response;
}>;

function montar({ rol = "RECEPCION", turnos = [FUTURO], configuracion, escribir }: Opciones = {}) {
  const agendas: URL[] = [];
  const escrituras: SolicitudSimulada[] = [];
  let lecturasConfiguracion = 0;
  simularSesionActiva(rol, (solicitud) => {
    const url = new URL(solicitud.url, "http://localhost");
    if (solicitud.metodo === "POST" && url.pathname.startsWith("/api/turnos/")) {
      escrituras.push(solicitud);
      return escribir?.(solicitud, url) ?? Response.json({ ...turnos[0], cancelacionTardia: true, reprogramadoTarde: true });
    }
    switch (url.pathname) {
      case "/api/agenda":
        agendas.push(url);
        return Response.json({
          vista: url.searchParams.get("vista") ?? "dia", desde: FECHA, hasta: "2026-12-13",
          turnos, horarios: [], bloqueos: [],
        });
      case "/api/configuracion":
        lecturasConfiguracion++;
        return configuracion?.() ?? Response.json({ "cancelacion.plazo_horas": 1 });
      case "/api/profesionales": return Response.json([
        { id: 1, nombre: "Ana", apellido: "Ficticia", activo: true }, { id: 2, nombre: "Bruno", apellido: "Ficticio", activo: true },
      ]);
      case "/api/sillones": return Response.json([{ id: 1, nombre: "Sillón 1", activo: true }, { id: 2, nombre: "Sillón 2", activo: true }]);
      case "/api/prestaciones": return Response.json([{ id: 1, nombre: "Consulta", activa: true }]);
      default: return new Response(null, { status: 404 });
    }
  });
  render(<MemoryRouter initialEntries={["/agenda"]}><AppRutas registro={REGISTRO} /></MemoryRouter>);
  return { agendas, escrituras, lecturasConfiguracion: () => lecturasConfiguracion, usuario: userEvent.setup() };
}

type Usuario = ReturnType<typeof userEvent.setup>;
type Panel = ReturnType<typeof within>;

async function abrir(usuario: Usuario, id: number) {
  await usuario.click(await screen.findByRole("button", { name: "Gestionar turnos" }));
  const panel = within(screen.getByRole("region", { name: "Gestionar turnos" }));
  await usuario.selectOptions(panel.getByLabelText("Turno"), String(id));
  return panel;
}

function problema(estado: number, codigo: string, propiedades: Record<string, unknown> = {}) {
  return Response.json({ codigo, detail: "Error de prueba", ...propiedades }, { status: estado });
}

async function reprogramar(usuario: Usuario, panel: Panel, hora = "09:30") {
  await usuario.click(panel.getByRole("button", { name: "Reprogramar" }));
  const confirmar = panel.getByRole("button", { name: "Confirmar reprogramación" });

  fireEvent.change(panel.getByLabelText("Nueva hora"), { target: { value: hora } });
  await waitFor(() => expect(confirmar).toBeEnabled());
  return confirmar;
}

beforeEach(() => {
  vi.spyOn(Date, "now").mockReturnValue(AHORA);
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

it("Recepción dispone de acciones según la hora y el estado", async () => {
  const { usuario, agendas, escrituras } = montar({ turnos: [FUTURO, INICIADO, ATENDIDO, CANCELADO] });
  const panel = await abrir(usuario, 1);
  expect(panel.getByRole("button", { name: "Reprogramar" })).toBeInTheDocument();
  expect(panel.getByRole("button", { name: "Cancelar turno" })).toBeInTheDocument();
  expect(panel.queryByRole("button", { name: "Marcar asistencia" })).not.toBeInTheDocument();
  expect(Array.from((panel.getByLabelText("Turno") as HTMLSelectElement).options).map((opcion) => opcion.value))
    .toEqual(["", "2", "1"]);
  await usuario.selectOptions(panel.getByLabelText("Turno"), "2");
  expect(panel.queryByRole("button", { name: "Reprogramar" })).not.toBeInTheDocument();
  await usuario.click(panel.getByRole("button", { name: "Marcar asistencia" }));
  await usuario.selectOptions(panel.getByLabelText("Estado de asistencia"), "ATENDIDO");
  await usuario.click(panel.getByRole("button", { name: "Registrar asistencia" }));
  expect(await screen.findByText("Turno marcado como atendido.")).toBeInTheDocument();
  expect(escrituras[0].url).toBe("/api/turnos/2/asistencia");
  expect(escrituras[0].cuerpo).toEqual({ estado: "ATENDIDO" });
  await waitFor(() => expect(agendas).toHaveLength(2));
});

it("Administración puede corregir una asistencia", async () => {
  const { usuario, escrituras, agendas } = montar({ rol: "ADMIN", turnos: [ATENDIDO] });
  const panel = await abrir(usuario, 3);
  await usuario.click(panel.getByRole("button", { name: "Corregir asistencia" }));
  expect(panel.queryByRole("option", { name: "Atendido" })).not.toBeInTheDocument();
  await usuario.selectOptions(panel.getByLabelText("Estado de asistencia"), "AUSENTE");
  await usuario.click(panel.getByRole("button", { name: "Guardar corrección" }));
  expect(await screen.findByText(/Asistencia corregida.*ausente/i)).toBeInTheDocument();
  expect(escrituras[0].cuerpo).toEqual({ estado: "AUSENTE" });
  await waitFor(() => expect(agendas).toHaveLength(2));
});

it("El odontólogo no puede gestionar turnos", async () => {
  const { usuario, escrituras, lecturasConfiguracion } = montar({ rol: "ODONTOLOGO", turnos: [FUTURO, INICIADO] });
  await screen.findByRole("region", { name: /Agenda del día/ });
  expect(screen.queryByRole("button", { name: "Gestionar turnos" })).not.toBeInTheDocument();
  await usuario.click(screen.getByRole("button", { name: "Semana" }));
  await screen.findByRole("region", { name: /Agenda de la semana/ });
  expect(screen.queryByRole("button", { name: "Gestionar turnos" })).not.toBeInTheDocument();
  expect(lecturasConfiguracion()).toBe(0);
  expect(escrituras).toHaveLength(0);
});

it("La agenda semanal reprograma y muestra la marca tardía", async () => {
  const { usuario, escrituras, agendas, lecturasConfiguracion } = montar({
    escribir: () => Response.json({ ...FUTURO, inicio: `${FECHA}T12:30:00Z`, profesionalId: 2, sillonId: 2, reprogramadoTarde: true }),
  });
  await usuario.click(await screen.findByRole("button", { name: "Semana" }));
  await screen.findByRole("region", { name: /Agenda de la semana/ });
  const panel = await abrir(usuario, 1);
  const confirmar = await reprogramar(usuario, panel);
  expect(panel.getByText(/Se registrará como reprogramación tardía/i)).toBeInTheDocument();
  expect(lecturasConfiguracion()).toBe(1);
  expect(escrituras).toHaveLength(0);
  await usuario.selectOptions(panel.getByLabelText("Profesional nuevo"), "2");
  await usuario.selectOptions(panel.getByLabelText("Sillón nuevo"), "2");
  fireEvent.change(panel.getByLabelText("Duración nueva (minutos)"), { target: { value: "45" } });
  await usuario.click(confirmar);
  expect(await screen.findByText(/Turno reprogramado.*marca de reprogramación tardía/i)).toBeInTheDocument();
  expect(escrituras[0].url).toBe("/api/turnos/1/reprogramar");
  expect(escrituras[0].cabeceras.get("X-XSRF-TOKEN")).toBe("ficticio");
  expect(escrituras[0].cuerpo).toEqual({ inicio: `${FECHA}T12:30:00.000Z`, profesionalId: 2, sillonId: 2, duracionMin: 45 });
  await waitFor(() => expect(agendas).toHaveLength(3));
  expect(agendas.at(-1)?.searchParams.get("vista")).toBe("semana");
});

it("Recepción cancela con motivo y la marca del servidor", async () => {
  const { usuario, escrituras, agendas } = montar({
    configuracion: () => Response.json({ "cancelacion.plazo_horas": 0 }),
    escribir: () => Response.json({ ...FUTURO, estado: "CANCELADO", cancelacionTardia: true }),
  });
  const panel = await abrir(usuario, 1);
  await usuario.click(panel.getByRole("button", { name: "Cancelar turno" }));
  const confirmar = panel.getByRole("button", { name: "Confirmar cancelación" });
  await waitFor(() => expect(confirmar).toBeEnabled());
  expect(panel.queryByText(/Se registrará como cancelación tardía/i)).not.toBeInTheDocument();
  await usuario.type(panel.getByLabelText("Motivo (opcional)"), "Cambio de planes");
  await usuario.click(confirmar);
  expect(await screen.findByText(/Turno cancelado.*cancelación tardía/i)).toBeInTheDocument();
  expect(escrituras[0].url).toBe("/api/turnos/1/cancelar");
  expect(escrituras[0].cuerpo).toEqual({ motivo: "Cambio de planes" });
  await waitFor(() => expect(agendas).toHaveLength(2));
});

it("En el límite exacto no se anticipa una marca tardía", async () => {
  const { usuario } = montar({ turnos: [turno(1, "13:00", "CONFIRMADO")] });
  const panel = await abrir(usuario, 1);
  await usuario.click(panel.getByRole("button", { name: "Cancelar turno" }));
  await waitFor(() => expect(panel.getByRole("button", { name: "Confirmar cancelación" })).toBeEnabled());
  expect(panel.queryByText(/Se registrará como cancelación tardía/i)).not.toBeInTheDocument();
});

it("Si falla la consulta del plazo no se presume una marca tardía", async () => {
  let intentos = 0;
  const { usuario } = montar({ configuracion: () => ++intentos === 1 ? new Response(null, { status: 503 })
    : Response.json({ "cancelacion.plazo_horas": 1 }) });
  const panel = await abrir(usuario, 1);
  await usuario.click(panel.getByRole("button", { name: "Cancelar turno" }));
  expect(await panel.findByText(/El servidor determinará si la operación es tardía/i)).toBeInTheDocument();
  expect(panel.queryByText(/Se registrará como cancelación tardía/i)).not.toBeInTheDocument();
  await usuario.click(panel.getByRole("button", { name: "Reintentar consulta del plazo" }));
  expect(await panel.findByText(/Se registrará como cancelación tardía/i)).toBeInTheDocument();
});

it("Un conflicto de horario conserva la reprogramación", async () => {
  const { usuario, escrituras } = montar({ escribir: () => problema(409, "RN-01", { recurso: "SILLON" }) });
  const panel = await abrir(usuario, 1);
  const confirmar = await reprogramar(usuario, panel);
  await usuario.click(confirmar);
  expect(await panel.findByText(/sillón.*ocupado/i)).toBeInTheDocument();
  expect(panel.getByLabelText("Nueva fecha")).toHaveValue(FECHA);
  expect(panel.getByLabelText("Nueva hora")).toHaveValue("09:30");
  expect(escrituras).toHaveLength(1);
});

it("Un cambio concurrente permite actualizar la agenda", async () => {
  const { usuario, agendas, escrituras } = montar({ escribir: () => problema(409, "TURNO_MODIFICADO") });
  const panel = await abrir(usuario, 1);
  await usuario.click(panel.getByRole("button", { name: "Cancelar turno" }));
  const confirmar = panel.getByRole("button", { name: "Confirmar cancelación" });
  await waitFor(() => expect(confirmar).toBeEnabled());
  await usuario.click(confirmar);
  expect(await panel.findByText(/El turno cambió/i)).toBeInTheDocument();
  expect(confirmar).toBeDisabled();
  await usuario.click(panel.getByRole("button", { name: "Actualizar agenda" }));
  await waitFor(() => expect(agendas).toHaveLength(2));
  expect(escrituras).toHaveLength(1);
});

it("Una escritura denegada informa la falta de permiso", async () => {
  const { usuario } = montar({ escribir: () => problema(403, "PROHIBIDO") });
  const panel = await abrir(usuario, 1);
  const confirmar = await reprogramar(usuario, panel);
  await usuario.click(confirmar);
  expect(await panel.findByText(/no tiene permiso/i)).toBeInTheDocument();
  expect(screen.queryByText(/Turno reprogramado/)).not.toBeInTheDocument();
});

it("Una regla de negocio informa el ajuste necesario", () => {
  const casos = [
    ["RN-05", /ya empezó.*asistencia/i],
    ["RN-20", /todavía no empezó/i],
    ["RN-04", /fuera.*atención/i],
    ["RN-09", /múltiplos de 5/i],
  ] as const;
  for (const [codigo, texto] of casos) {
    expect(mensajeErrorGestion(new ErrorApi({ tipo: "no-procesable", estado: 422, codigo, detalle: "Error de prueba" }))).toMatch(texto);
  }
});
