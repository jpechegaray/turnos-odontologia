import { act, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../../app/AppRutas";
import { construirRegistro } from "../../../app/rutas/registro";
import type { Rol } from "../../../app/rutas/tipos";
import { simularSesionActiva, type SolicitudSimulada } from "../../../app/sesion.fixture";
import { ErrorApi } from "../../../shared/api/errorApi";
import { rutas } from "../routes";
import { mensajeDelTurno } from "./errores";

const FECHA = "2026-12-07";
const REGISTRO = construirRegistro({ agenda: { rutas } });
const PROFESIONALES = [
  { id: 1, nombre: "Ana", apellido: "Ficticia", matricula: "MP-0001", especialidad: null, activo: true },
  { id: 2, nombre: "Bruno", apellido: "Inventado", matricula: "MP-0002", especialidad: null, activo: false },
];
const SILLONES = [
  { id: 1, nombre: "Sillón 1", descripcion: null, activo: true },
  { id: 2, nombre: "Sillón 2", descripcion: null, activo: true },
  { id: 3, nombre: "Sillón desactivado", descripcion: null, activo: false },
];
const PRESTACIONES = [
  { id: 11, nombre: "Consulta", duracionDefaultMin: 30, activa: true },
  { id: 12, nombre: "Prestación no asignada", duracionDefaultMin: 45, activa: true },
];
const PACIENTE = { id: 7, nombre: "Paciente", apellido: "Prueba", activo: true, dni: "00000001" };
const HUECO = { inicio: `${FECHA}T12:00:00Z`, fin: `${FECHA}T12:30:00Z`, sillonId: 1 };
const OTRO_HUECO = { inicio: `${FECHA}T13:00:00Z`, fin: `${FECHA}T13:30:00Z`, sillonId: 2 };

type Opciones = Readonly<{
  rol?: Rol;
  disponibilidad?: (url: URL) => Response | Promise<Response>;
  escribir?: (solicitud: SolicitudSimulada) => Response;
}>;

function montar({ rol = "RECEPCION", disponibilidad, escribir }: Opciones = {}) {
  const consultas: URL[] = [];
  const altas: SolicitudSimulada[] = [];
  const fechasAgenda: Array<string | null> = [];
  const filtrosPaciente: URL[] = [];
  simularSesionActiva(rol, (solicitud) => {
    const url = new URL(solicitud.url, "http://localhost");
    switch (url.pathname) {
      case "/api/agenda":
        fechasAgenda.push(url.searchParams.get("fecha"));
        return Response.json({ vista: "dia", desde: url.searchParams.get("fecha") ?? FECHA, hasta: FECHA,
          turnos: [], horarios: [], bloqueos: [] });
      case "/api/profesionales": return Response.json(PROFESIONALES);
      case "/api/profesionales/1/prestaciones": return Response.json(PRESTACIONES.slice(0, 1));
      case "/api/sillones": return Response.json(SILLONES);
      case "/api/prestaciones": return Response.json(PRESTACIONES);
      case "/api/pacientes":
        filtrosPaciente.push(url);
        return Response.json([PACIENTE, { ...PACIENTE, id: 8, apellido: "Inactivo", activo: false }]);
      case "/api/agenda/disponibilidad":
        consultas.push(url);
        return disponibilidad?.(url) ?? Response.json([HUECO]);
      case "/api/turnos":
        altas.push(solicitud);
        return escribir?.(solicitud) ?? Response.json({ id: 42, inicio: HUECO.inicio }, { status: 201 });
      default: return new Response(null, { status: 404 });
    }
  });
  render(<MemoryRouter initialEntries={["/agenda"]}><AppRutas registro={REGISTRO} /></MemoryRouter>);
  return { consultas, altas, fechasAgenda, filtrosPaciente, usuario: userEvent.setup() };
}

type Usuario = ReturnType<typeof userEvent.setup>;

async function abrir(usuario: Usuario) {
  await usuario.click(await screen.findByRole("button", { name: "Nuevo turno" }));
  return within(screen.getByRole("region", { name: "Nuevo turno" }));
}

async function elegirCriterios(usuario: Usuario) {
  const panel = await abrir(usuario);
  await panel.findByRole("option", { name: "Ana Ficticia" });
  expect(panel.queryByRole("option", { name: "Bruno Inventado" })).not.toBeInTheDocument();
  await usuario.selectOptions(panel.getByLabelText("Profesional"), "1");
  await panel.findByRole("option", { name: "Consulta" });
  expect(panel.queryByRole("option", { name: "Prestación no asignada" })).not.toBeInTheDocument();
  await usuario.selectOptions(panel.getByLabelText("Prestación"), "11");
  return panel;
}

async function elegirPaciente(usuario: Usuario, panel: ReturnType<typeof within>) {
  await usuario.type(panel.getByLabelText("Dato del paciente"), "Prueba");
  await usuario.click(panel.getByRole("button", { name: "Buscar paciente" }));
  await panel.findByRole("option", { name: /Paciente Prueba/ });
  expect(panel.queryByRole("option", { name: /Paciente Inactivo/ })).not.toBeInTheDocument();
  await usuario.selectOptions(panel.getByLabelText("Paciente"), "7");
}

async function elegirHorario(usuario: Usuario, panel: ReturnType<typeof within>) {
  await usuario.click(panel.getByRole("button", { name: "Buscar horarios" }));
  await panel.findByRole("option", { name: /09:00 a 09:30/ });
  await usuario.selectOptions(panel.getByLabelText("Horario sugerido"), HUECO.inicio);
}

function problema(status: 409 | 422, codigo: string, datos: object) {
  return Response.json({ codigo, detail: "Error de prueba", ...datos }, { status });
}

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
  vi.unstubAllGlobals();
});

it("Recepción crea el turno y actualiza la agenda", async () => {
  const { usuario, altas, filtrosPaciente, fechasAgenda } = montar();
  await waitFor(() => expect(fechasAgenda).toEqual([null]));
  const panel = await elegirCriterios(usuario);
  await elegirPaciente(usuario, panel);
  expect(filtrosPaciente.map((url) => url.search)).toEqual(["?apellido=Prueba"]);
  await elegirHorario(usuario, panel);
  expect(panel.getByLabelText("Sillón")).toHaveValue("1");
  expect(panel.queryByRole("option", { name: "Sillón desactivado" })).not.toBeInTheDocument();
  await usuario.selectOptions(panel.getByLabelText("Sillón"), "2");
  await usuario.click(panel.getByRole("button", { name: "Guardar turno" }));
  expect(await screen.findByText("Turno creado.")).toBeInTheDocument();
  expect(altas).toHaveLength(1);
  expect(altas[0].metodo).toBe("POST");
  expect(altas[0].cabeceras.get("X-XSRF-TOKEN")).toBe("ficticio");
  expect(altas[0].cuerpo).toEqual({ pacienteId: 7, profesionalId: 1, prestacionId: 11, sillonId: 2,
    inicio: HUECO.inicio, duracionMin: 30 });
  await waitFor(() => expect(fechasAgenda).toEqual([null, FECHA]));
  expect(screen.getByRole("button", { name: "Nuevo turno" })).toHaveFocus();
});

it("Administración puede abrir el formulario de alta", async () => {
  const { usuario } = montar({ rol: "ADMIN" });
  const panel = await abrir(usuario);
  expect(panel.getByRole("button", { name: "Guardar turno" })).toBeDisabled();
});

it("El odontólogo no ve el alta de turno", async () => {
  const { consultas, altas } = montar({ rol: "ODONTOLOGO" });
  await screen.findByRole("heading", { name: /Lunes 07\/12\/2026/ });
  expect(screen.queryByRole("button", { name: "Nuevo turno" })).not.toBeInTheDocument();
  expect(consultas).toHaveLength(0);
  expect(altas).toHaveLength(0);
});

it("La duración ajustada vuelve a consultar el día de Buenos Aires", async () => {
  const { usuario, consultas } = montar({ disponibilidad: (url) => Response.json(url.searchParams.get("duracionMin") === "45"
    ? [{ ...HUECO, fin: `${FECHA}T12:45:00Z` }] : [HUECO]) });
  const panel = await elegirCriterios(usuario);
  await elegirHorario(usuario, panel);
  const duracion = panel.getByLabelText("Duración (minutos)");
  expect(duracion).toHaveValue(30);
  await usuario.clear(duracion);
  await usuario.type(duracion, "45");
  expect(panel.queryByRole("option", { name: /09:00 a 09:30/ })).not.toBeInTheDocument();
  await usuario.click(panel.getByRole("button", { name: "Buscar horarios" }));
  await panel.findByRole("option", { name: /09:00 a 09:45/ });
  expect(Object.fromEntries(consultas.at(-1)?.searchParams ?? [])).toEqual({
    desde: `${FECHA}T03:00:00.000Z`, hasta: "2026-12-08T03:00:00.000Z",
    duracionMin: "45", profesionalId: "1", prestacionId: "11",
  });
});

it("Un cambio de fecha descarta la disponibilidad anterior", async () => {
  let responder: (respuesta: Response) => void = () => { throw new Error("No comenzó la consulta"); };
  const pendiente = new Promise<Response>((resolve) => { responder = resolve; });
  const { usuario, consultas } = montar({ disponibilidad: () => pendiente });
  const panel = await elegirCriterios(usuario);
  await usuario.click(panel.getByRole("button", { name: "Buscar horarios" }));
  await waitFor(() => expect(consultas).toHaveLength(1));
  const fecha = panel.getByLabelText("Fecha del turno");
  await usuario.clear(fecha);
  await usuario.type(fecha, "2026-12-08");
  expect(fecha).toHaveValue("2026-12-08");
  await act(async () => responder(Response.json([HUECO])));
  expect(panel.queryByRole("option", { name: /09:00 a 09:30/ })).not.toBeInTheDocument();
});

it("Un conflicto del profesional actualiza las alternativas", async () => {
  let busquedas = 0;
  const { usuario, consultas } = montar({
    disponibilidad: () => Response.json(++busquedas === 1 ? [HUECO] : [OTRO_HUECO]),
    escribir: () => problema(409, "RN-01", { recurso: "PROFESIONAL" }),
  });
  const panel = await elegirCriterios(usuario);
  await elegirPaciente(usuario, panel);
  await elegirHorario(usuario, panel);
  await usuario.click(panel.getByRole("button", { name: "Guardar turno" }));
  expect(await panel.findByRole("alert")).toHaveTextContent(/profesional.*ocupado/i);
  await panel.findByRole("option", { name: /10:00 a 10:30/ });
  expect(panel.getByLabelText("Paciente")).toHaveValue("7");
  expect(consultas.map((url) => url.search)).toEqual([consultas[0].search, consultas[0].search]);
  expect(panel.getByRole("button", { name: "Guardar turno" })).toBeDisabled();
});

it("Un conflicto del sillón permite elegir otro sillón", async () => {
  let busquedas = 0;
  const { usuario } = montar({
    disponibilidad: () => Response.json([{ ...HUECO, sillonId: ++busquedas }]),
    escribir: () => problema(409, "RN-01", { recurso: "SILLON" }),
  });
  const panel = await elegirCriterios(usuario);
  await elegirPaciente(usuario, panel);
  await elegirHorario(usuario, panel);
  await usuario.click(panel.getByRole("button", { name: "Guardar turno" }));
  expect(await panel.findByRole("alert")).toHaveTextContent(/sillón.*ocupado/i);
  const nueva = await panel.findByRole("option", { name: /09:00 a 09:30.*Sillón 2/ });
  await usuario.selectOptions(panel.getByLabelText("Horario sugerido"), nueva);
  expect(panel.getByLabelText("Sillón")).toHaveValue("2");
});

it("Un bloqueo del profesional indica cambiar el horario", async () => {
  const { usuario } = montar({ escribir: () => problema(422, "RN-04", { motivo: "BLOQUEO" }) });
  const panel = await elegirCriterios(usuario);
  await elegirPaciente(usuario, panel);
  await elegirHorario(usuario, panel);
  await usuario.click(panel.getByRole("button", { name: "Guardar turno" }));
  expect(await panel.findByRole("alert")).toHaveTextContent(/bloqueo.*otro horario/i);
  expect(panel.getByLabelText("Paciente")).toHaveValue("7");
});

it("Los códigos de regla indican cómo corregir el turno", () => {
  const ejemplos = [
    ["RN-04", /fuera.*atención.*otro horario/i],
    ["RN-09", /duración.*5 a 480.*múltiplos/i],
    ["RN-13", /pasó.*futuro/i],
    ["RN-11", /profesional.*prestación/i],
    ["PACIENTE_INACTIVO", /paciente.*otro paciente/i],
    ["PROFESIONAL_INEXISTENTE", /profesional.*otro profesional/i],
    ["PRESTACION_INACTIVA", /prestación.*otra prestación/i],
    ["SILLON_INACTIVO", /sillón.*otro sillón/i],
  ] as const;
  for (const [codigo, expresion] of ejemplos) {
    expect(mensajeDelTurno(new ErrorApi({ tipo: "no-procesable", estado: 422, codigo, detalle: "Error de prueba" })))
      .toMatch(expresion);
  }
});
