import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppRutas } from "../../app/AppRutas";
import { registroDeFeatures } from "../../app/rutas/modulos";
import type { Rol } from "../../app/rutas/tipos";
import { simularSesionActiva } from "../../app/sesion.fixture";
import type { AgendaRespuesta, EstadoTurno, TurnoAgenda } from "../agenda/tipos";

// Datos ficticios (regla 1).
const PROFESIONALES = [{ id: 1, nombre: "Lucía", apellido: "Ferreyra", matricula: "MP-0001", especialidad: null, activo: true }];
const PRESTACIONES = [{ id: 1, nombre: "Limpieza", duracionDefaultMin: 30, activa: true }];

let siguienteId = 1;

function turno(hora: string, estado: EstadoTurno, apellido: string, conPaciente = true): TurnoAgenda {
  const [h, m] = hora.split(":").map(Number);
  const fin = `${String(h + (m + 30 >= 60 ? 1 : 0)).padStart(2, "0")}:${String((m + 30) % 60).padStart(2, "0")}`;
  return {
    id: siguienteId++,
    inicio: `2026-12-07T${hora}:00-03:00`,
    fin: `2026-12-07T${fin}:00-03:00`,
    duracionMin: 30,
    estado,
    profesionalId: 1,
    sillonId: 1,
    prestacionId: 1,
    pacienteId: siguienteId,
    paciente: conPaciente ? { nombre: "Paciente", apellido } : null,
    notas: null,
  };
}

function agendaDelDia(turnos: TurnoAgenda[]): AgendaRespuesta {
  return { vista: "dia", desde: "2026-12-07", hasta: "2026-12-07", turnos, horarios: [], bloqueos: [] };
}

function montar(rol: Rol, responderAgenda: () => AgendaRespuesta | Response) {
  const { fetch } = simularSesionActiva(rol, ({ url }) => {
    if (url.includes("/api/agenda")) {
      const respuesta = responderAgenda();
      return respuesta instanceof Response ? respuesta : Response.json(respuesta);
    }
    if (url.includes("/api/profesionales")) return Response.json(PROFESIONALES);
    if (url.includes("/api/prestaciones")) return Response.json(PRESTACIONES);
    if (url.includes("/api/sillones")) return Response.json([]);
    return Response.json({});
  });
  render(
    <MemoryRouter initialEntries={["/"]}>
      <AppRutas registro={registroDeFeatures} />
    </MemoryRouter>,
  );
  return fetch;
}

beforeEach(() => {
  // Solo se simula la fecha: las promesas y los temporizadores siguen reales.
  vi.useFakeTimers({ toFake: ["Date"] });
  vi.setSystemTime(new Date("2026-12-07T13:15:00Z")); // 10:15 en Buenos Aires
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("Accesos a las secciones en inicio", () => {
  it("Inicio muestra un recuadro por sección permitida", async () => {
    montar("RECEPCION", () => agendaDelDia([]));

    const accesos = await screen.findByRole("region", { name: "Secciones" });
    expect(within(accesos).getByRole("link", { name: /Agenda/ })).toHaveAttribute("href", "/agenda");
    expect(within(accesos).getByRole("link", { name: /Pacientes/ })).toHaveAttribute("href", "/pacientes");
    expect(within(accesos).getByRole("link", { name: /Configuración/ })).toHaveAttribute("href", "/configuracion");
    expect(within(accesos).queryByRole("link", { name: /Usuarios/ })).not.toBeInTheDocument();
    expect(within(accesos).queryByRole("link", { name: /Inicio/ })).not.toBeInTheDocument();
  });
});

describe("Resumen del día en inicio", () => {
  it("Inicio muestra las cifras del día", async () => {
    const fetch = montar("ADMIN", () => agendaDelDia([
      turno("09:00", "CONFIRMADO", "Uno"),
      turno("10:30", "CONFIRMADO", "Dos"),
      turno("11:00", "PROGRAMADO", "Tres"),
      turno("08:00", "ATENDIDO", "Cuatro"),
      turno("12:00", "CANCELADO", "Cinco"),
    ]));

    const cifras = await screen.findByRole("region", { name: "Cifras del día" });
    expect(within(cifras).getByText("Turnos de hoy").closest("div")).toHaveTextContent("4");
    expect(within(cifras).getByText("Confirmados").closest("div")).toHaveTextContent("2");
    expect(within(cifras).getByText("Reservados").closest("div")).toHaveTextContent("1");
    expect(within(cifras).getByText("Atendidos").closest("div")).toHaveTextContent("1");
    const pedido = fetch.mock.calls.map(([entrada]) => String(entrada)).find((url) => url.includes("/api/agenda"));
    expect(pedido).toBe("/api/agenda?vista=dia");
  });
});

describe("Próximos turnos en inicio", () => {
  it("Inicio lista los próximos turnos", async () => {
    montar("ADMIN", () => agendaDelDia([
      turno("11:00", "CONFIRMADO", "Tarde"),
      turno("09:00", "ATENDIDO", "Temprano"),
      turno("10:30", "PROGRAMADO", "Siguiente"),
      turno("12:00", "CANCELADO", "Cancelada"),
    ]));

    const proximos = await screen.findByRole("region", { name: "Próximos turnos" });
    const filas = await within(proximos).findAllByRole("listitem");
    expect(filas).toHaveLength(2);
    expect(filas[0]).toHaveTextContent("10:30");
    expect(filas[0]).toHaveTextContent("Paciente Siguiente");
    expect(filas[0]).toHaveTextContent("Reservado");
    expect(filas[1]).toHaveTextContent("11:00");
    expect(filas[1]).toHaveTextContent("Confirmado");
    expect(within(proximos).getByRole("link", { name: "Ver agenda" })).toHaveAttribute("href", "/agenda");
  });

  it("Inicio sin turnos pendientes", async () => {
    montar("ADMIN", () => agendaDelDia([]));

    expect(await screen.findByText("No quedan turnos para hoy")).toBeInTheDocument();
  });

  it("Un turno de otro profesional no muestra el paciente", async () => {
    montar("ODONTOLOGO", () => agendaDelDia([turno("11:00", "CONFIRMADO", "Oculto", false)]));

    expect(await screen.findByText("Paciente de otro profesional")).toBeInTheDocument();
  });
});

describe("Error al cargar el resumen", () => {
  it("Un fallo al cargar el resumen permite reintentar", async () => {
    let intentos = 0;
    montar("ADMIN", () => (++intentos === 1 ? new Response(null, { status: 500 }) : agendaDelDia([turno("11:00", "CONFIRMADO", "Uno")])));

    await userEvent.click(await screen.findByRole("button", { name: "Reintentar" }));

    const cifras = await screen.findByRole("region", { name: "Cifras del día" });
    expect(within(cifras).getByText("Turnos de hoy").closest("div")).toHaveTextContent("1");
    expect(screen.queryByRole("button", { name: "Reintentar" })).not.toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: "Principal" })).toBeInTheDocument();
  });
});
