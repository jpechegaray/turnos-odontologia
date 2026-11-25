import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../app/AppRutas";
import { construirRegistro } from "../../app/rutas/registro";
import type { Rol } from "../../app/rutas/tipos";
import { simularSesionActiva } from "../../app/sesion.fixture";
import { rutas } from "./routes";
import type { AgendaRespuesta, BloqueoAgenda, HorarioAgenda, TurnoAgenda } from "./tipos";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const registro = construirRegistro({ agenda: { rutas } });

// Datos ficticios (regla 1).
const PROFESIONALES = [
  { id: 1, nombre: "Lucía", apellido: "Ferreyra", matricula: "MP-0001", especialidad: null, activo: true },
  { id: 2, nombre: "Matías", apellido: "Orellana", matricula: "MP-0002", especialidad: null, activo: true },
];
const SILLONES = [
  { id: 1, nombre: "Sillón 1", descripcion: null, activo: true },
  { id: 2, nombre: "Sillón 2", descripcion: null, activo: true },
];
const PRESTACIONES = [
  { id: 1, nombre: "Limpieza", duracionDefaultMin: 30, activa: true },
  { id: 2, nombre: "Endodoncia", duracionDefaultMin: 45, activa: true },
];
const FICHA = {
  id: 7,
  nombre: "Ana",
  apellido: "Prueba",
  dni: "30111222",
  email: "ana.prueba@example.test",
  telefono: "11 5555-0101",
  obraSocial: "Obra Social Ficticia",
  nroAfiliado: "AF-123",
  notas: null,
  activo: true,
  creadoEn: "2026-11-01T12:00:00Z",
};

let siguienteId = 100;

function turno(datos: Partial<TurnoAgenda> & Pick<TurnoAgenda, "inicio" | "fin">): TurnoAgenda {
  return {
    id: siguienteId++,
    duracionMin: 30,
    estado: "PROGRAMADO",
    profesionalId: 1,
    sillonId: 1,
    prestacionId: 1,
    pacienteId: 7,
    paciente: { nombre: "Ana", apellido: "Prueba" },
    notas: null,
    ...datos,
  };
}

function horario(profesionalId: number, fecha: string, desde: string, hasta: string): HorarioAgenda {
  return { profesionalId, fecha, inicio: `${fecha}T${desde}:00-03:00`, fin: `${fecha}T${hasta}:00-03:00` };
}

function bloqueo(datos: Omit<BloqueoAgenda, "id">): BloqueoAgenda {
  return { id: siguienteId++, ...datos };
}

function agenda(
  vista: "dia" | "semana",
  desde: string,
  hasta: string,
  contenido: Partial<Pick<AgendaRespuesta, "turnos" | "horarios" | "bloqueos">> = {},
): AgendaRespuesta {
  return { vista, desde, hasta, turnos: [], horarios: [], bloqueos: [], ...contenido };
}

type RespuestaAgendaSimulada = (url: URL) => AgendaRespuesta | Response;

function montar(rol: Rol, responderAgenda: RespuestaAgendaSimulada): FetchSimulado {
  const { fetch } = simularSesionActiva(rol, ({ url, metodo }) => {
    if (metodo !== "GET") return Response.json({});
    const direccion = new URL(url, "http://localhost");
    if (direccion.pathname === "/api/agenda") {
      const respuesta = responderAgenda(direccion);
      return respuesta instanceof Response ? respuesta : Response.json(respuesta);
    }
    if (direccion.pathname === "/api/profesionales") return Response.json(PROFESIONALES);
    if (direccion.pathname === "/api/sillones") return Response.json(SILLONES);
    if (direccion.pathname === "/api/prestaciones") return Response.json(PRESTACIONES);
    if (direccion.pathname === "/api/pacientes/7") return Response.json(FICHA);
    return new Response(null, { status: 404 });
  });
  render(
    <MemoryRouter initialEntries={["/agenda"]}>
      <AppRutas registro={registro} />
    </MemoryRouter>,
  );
  return fetch;
}

function consultasAgenda(fetch: FetchSimulado): URL[] {
  return fetch.mock.calls
    .map(([url]) => new URL(String(url), "http://localhost"))
    .filter((url) => url.pathname === "/api/agenda");
}

function ultimaConsulta(fetch: FetchSimulado): URL {
  const consultas = consultasAgenda(fetch);
  return consultas[consultas.length - 1];
}

function escrituras(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(([, init]) => ((init as RequestInit | undefined)?.method ?? "GET") !== "GET");
}

/** Bloque de la grilla o de la semana que contiene el texto. */
function bloqueCon(texto: string): HTMLElement {
  const bloque = screen.getByText(texto).closest<HTMLElement>(".slot, .semana__item");
  if (!bloque) throw new Error(`Sin bloque para ${texto}`);
  return bloque;
}

/** Lunes 7 de diciembre de 2026: el "hoy" que responde el servidor. */
const HOY = "2026-12-07";

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

it("Al entrar se muestra la agenda de hoy", async () => {
  const fetch = montar("RECEPCION", () =>
    agenda("dia", HOY, HOY, {
      turnos: [turno({ inicio: `${HOY}T10:00:00-03:00`, fin: `${HOY}T10:30:00-03:00` })],
    }),
  );

  expect(await screen.findByRole("heading", { name: "Lunes 07/12/2026" })).toBeInTheDocument();
  expect(screen.getByText("Ana Prueba")).toBeInTheDocument();
  const primera = consultasAgenda(fetch)[0];
  expect(primera.searchParams.get("vista")).toBe("dia");
  expect(primera.searchParams.has("fecha")).toBe(false);
});

it("Un fallo al cargar la agenda permite reintentar", async () => {
  let intentos = 0;
  montar("RECEPCION", () =>
    ++intentos === 1 ? new Response(null, { status: 500 }) : agenda("dia", HOY, HOY),
  );

  expect(await screen.findByText("No pudimos cargar la agenda.")).toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

  expect(await screen.findByRole("heading", { name: "Lunes 07/12/2026" })).toBeInTheDocument();
  expect(screen.queryByText("No pudimos cargar la agenda.")).not.toBeInTheDocument();
});

it("La vista diaria ubica cada turno en la columna de su profesional", async () => {
  montar("RECEPCION", () =>
    agenda("dia", HOY, HOY, {
      turnos: [
        turno({ inicio: `${HOY}T10:00:00-03:00`, fin: `${HOY}T10:30:00-03:00` }),
        turno({
          inicio: `${HOY}T11:00:00-03:00`,
          fin: `${HOY}T11:45:00-03:00`,
          duracionMin: 45,
          profesionalId: 2,
          prestacionId: 2,
          pacienteId: 8,
          paciente: { nombre: "Bruno", apellido: "Ejemplo" },
        }),
      ],
    }),
  );

  const lucia = await screen.findByRole("region", { name: "Lucía Ferreyra" });
  const matias = screen.getByRole("region", { name: "Matías Orellana" });
  expect(within(lucia).getByText("Ana Prueba")).toBeInTheDocument();
  expect(within(lucia).getByText(/10:00 a 10:30 · Limpieza/)).toBeInTheDocument();
  expect(within(lucia).queryByText("Bruno Ejemplo")).not.toBeInTheDocument();
  expect(within(matias).getByText("Bruno Ejemplo")).toBeInTheDocument();
  expect(within(matias).getByText(/11:00 a 11:45 · Endodoncia/)).toBeInTheDocument();
});

it("La vista diaria por sillón agrupa los turnos por sillón", async () => {
  montar("RECEPCION", () =>
    agenda("dia", HOY, HOY, {
      turnos: [
        turno({ inicio: `${HOY}T10:00:00-03:00`, fin: `${HOY}T10:30:00-03:00` }),
        turno({
          inicio: `${HOY}T10:30:00-03:00`,
          fin: `${HOY}T11:00:00-03:00`,
          sillonId: 2,
          pacienteId: 8,
          paciente: { nombre: "Bruno", apellido: "Ejemplo" },
        }),
      ],
    }),
  );

  await userEvent.click(await screen.findByRole("button", { name: "Por sillón" }));

  const sillon1 = screen.getByRole("region", { name: "Sillón 1" });
  const sillon2 = screen.getByRole("region", { name: "Sillón 2" });
  expect(within(sillon1).getByText("Ana Prueba")).toBeInTheDocument();
  expect(within(sillon1).queryByText("Bruno Ejemplo")).not.toBeInTheDocument();
  expect(within(sillon2).getByText("Bruno Ejemplo")).toBeInTheDocument();
  expect(screen.queryByRole("region", { name: "Lucía Ferreyra" })).not.toBeInTheDocument();
});

it("Se muestran los horarios de atención y los bloqueos del profesional", async () => {
  montar("RECEPCION", () =>
    agenda("dia", HOY, HOY, {
      horarios: [horario(1, HOY, "09:00", "13:00")],
      bloqueos: [
        bloqueo({ profesionalId: 1, inicio: `${HOY}T12:00:00-03:00`, fin: `${HOY}T13:00:00-03:00`, motivo: "Capacitación" }),
      ],
    }),
  );

  const lucia = await screen.findByRole("region", { name: "Lucía Ferreyra" });
  expect(within(lucia).getByText("Atiende de 09:00 a 13:00")).toBeInTheDocument();
  const bloque = bloqueCon("Capacitación");
  expect(lucia).toContainElement(bloque);
  expect(within(bloque).getByText("Bloqueo")).toBeInTheDocument();
  expect(within(bloque).getByText(/12:00 a 13:00/)).toBeInTheDocument();
  expect(within(screen.getByRole("region", { name: "Matías Orellana" })).getByText("No atiende")).toBeInTheDocument();
});

it("La vista semanal muestra cada turno en su día", async () => {
  const fetch = montar("RECEPCION", (url) =>
    url.searchParams.get("vista") === "semana"
      ? agenda("semana", HOY, "2026-12-13", {
          turnos: [
            turno({ inicio: `${HOY}T10:00:00-03:00`, fin: `${HOY}T10:30:00-03:00` }),
            turno({
              inicio: "2026-12-13T09:00:00-03:00",
              fin: "2026-12-13T09:30:00-03:00",
              pacienteId: 8,
              paciente: { nombre: "Bruno", apellido: "Ejemplo" },
            }),
          ],
        })
      : agenda("dia", HOY, HOY),
  );

  await userEvent.click(await screen.findByRole("button", { name: "Semana" }));

  expect(await screen.findByRole("heading", { name: "Semana del 07/12/2026 al 13/12/2026" })).toBeInTheDocument();
  const consulta = ultimaConsulta(fetch);
  expect(consulta.searchParams.get("vista")).toBe("semana");
  expect(consulta.searchParams.get("fecha")).toBe(HOY);
  const dias = ["Lunes 07/12", "Martes 08/12", "Miércoles 09/12", "Jueves 10/12", "Viernes 11/12", "Sábado 12/12", "Domingo 13/12"];
  for (const dia of dias) expect(screen.getByRole("region", { name: dia })).toBeInTheDocument();
  expect(within(screen.getByRole("region", { name: "Lunes 07/12" })).getByText("Ana Prueba")).toBeInTheDocument();
  expect(within(screen.getByRole("region", { name: "Domingo 13/12" })).getByText("Bruno Ejemplo")).toBeInTheDocument();
  expect(within(screen.getByRole("region", { name: "Martes 08/12" })).queryByText(/Prueba|Ejemplo/)).not.toBeInTheDocument();
});

it("Un bloqueo de varios días aparece en cada día que toca", async () => {
  montar("RECEPCION", (url) =>
    url.searchParams.get("vista") === "semana"
      ? agenda("semana", HOY, "2026-12-13", {
          bloqueos: [
            bloqueo({ profesionalId: 1, inicio: "2026-12-09T15:00:00-03:00", fin: "2026-12-11T10:00:00-03:00", motivo: "Congreso" }),
          ],
        })
      : agenda("dia", HOY, HOY),
  );

  await userEvent.click(await screen.findByRole("button", { name: "Semana" }));
  await screen.findByRole("heading", { name: "Semana del 07/12/2026 al 13/12/2026" });

  for (const dia of ["Miércoles 09/12", "Jueves 10/12", "Viernes 11/12"]) {
    expect(within(screen.getByRole("region", { name: dia })).getByText(/Congreso/)).toBeInTheDocument();
  }
  for (const dia of ["Lunes 07/12", "Martes 08/12", "Sábado 12/12", "Domingo 13/12"]) {
    expect(within(screen.getByRole("region", { name: dia })).queryByText(/Congreso/)).not.toBeInTheDocument();
  }
});

it("Ir al día siguiente pide la fecha siguiente", async () => {
  const fetch = montar("RECEPCION", (url) => {
    const fecha = url.searchParams.get("fecha") ?? HOY;
    return agenda("dia", fecha, fecha);
  });

  await screen.findByRole("heading", { name: "Lunes 07/12/2026" });
  await userEvent.click(screen.getByRole("button", { name: "Siguiente" }));

  expect(await screen.findByRole("heading", { name: "Martes 08/12/2026" })).toBeInTheDocument();
  expect(ultimaConsulta(fetch).searchParams.get("fecha")).toBe("2026-12-08");

  await userEvent.click(screen.getByRole("button", { name: "Hoy" }));
  expect(await screen.findByRole("heading", { name: "Lunes 07/12/2026" })).toBeInTheDocument();
  expect(ultimaConsulta(fetch).searchParams.has("fecha")).toBe(false);
});

it("En la vista semanal se avanza una semana", async () => {
  const fetch = montar("RECEPCION", (url) => {
    if (url.searchParams.get("vista") === "semana") {
      return url.searchParams.get("fecha") === "2026-11-30"
        ? agenda("semana", "2026-11-30", "2026-12-06")
        : agenda("semana", HOY, "2026-12-13");
    }
    return agenda("dia", HOY, HOY);
  });

  await userEvent.click(await screen.findByRole("button", { name: "Semana" }));
  await screen.findByRole("heading", { name: "Semana del 07/12/2026 al 13/12/2026" });
  await userEvent.click(screen.getByRole("button", { name: "Anterior" }));

  expect(await screen.findByRole("heading", { name: "Semana del 30/11/2026 al 06/12/2026" })).toBeInTheDocument();
  const consulta = ultimaConsulta(fetch);
  expect(consulta.searchParams.get("vista")).toBe("semana");
  expect(consulta.searchParams.get("fecha")).toBe("2026-11-30");
});

it("Filtrar por profesional y por sillón envía los dos filtros", async () => {
  const fetch = montar("RECEPCION", () => agenda("dia", HOY, HOY));

  await screen.findByRole("heading", { name: "Lunes 07/12/2026" });
  await userEvent.selectOptions(await screen.findByLabelText("Profesional"), "1");
  await userEvent.selectOptions(screen.getByLabelText("Sillón"), "2");

  await waitFor(() => {
    const consulta = ultimaConsulta(fetch);
    expect(consulta.searchParams.get("profesionalId")).toBe("1");
    expect(consulta.searchParams.get("sillonId")).toBe("2");
  });
  // Con el filtro de profesional, la grilla muestra solo su columna.
  expect(await screen.findByRole("region", { name: "Lucía Ferreyra" })).toBeInTheDocument();
  expect(screen.queryByRole("region", { name: "Matías Orellana" })).not.toBeInTheDocument();
});

it("Cada turno muestra el badge de su estado", async () => {
  const estados = [
    ["PROGRAMADO", "Reservado", "st-reservado", "Paula Programada"],
    ["CONFIRMADO", "Confirmado", "st-confirmado", "Carla Confirmada"],
    ["ATENDIDO", "Atendido", "st-atendido", "Andrés Atendido"],
    ["AUSENTE", "Ausente", "st-ausente", "Aldo Ausente"],
    ["CANCELADO", "Cancelado", "st-cancelado", "Ciro Cancelado"],
  ] as const;
  montar("RECEPCION", () =>
    agenda("dia", HOY, HOY, {
      turnos: estados.map(([estado, , , nombreCompleto], i) => {
        const [nombre, apellido] = nombreCompleto.split(" ");
        return turno({
          inicio: `${HOY}T${String(9 + i).padStart(2, "0")}:00:00-03:00`,
          fin: `${HOY}T${String(9 + i).padStart(2, "0")}:30:00-03:00`,
          estado,
          pacienteId: 20 + i,
          paciente: { nombre, apellido },
        });
      }),
    }),
  );

  await screen.findByRole("region", { name: "Lucía Ferreyra" });
  for (const [, texto, clase, paciente] of estados) {
    const bloque = bloqueCon(paciente);
    const badge = within(bloque).getByText(texto).closest(".badge");
    expect(badge).toHaveClass(clase);
    expect(bloque).toHaveClass(clase);
  }
  expect(bloqueCon("Ciro Cancelado")).toHaveClass("slot--cancelado");
  const leyenda = screen.getByRole("list", { name: "Estados de la agenda" });
  for (const texto of ["Reservado", "Confirmado", "Atendido", "Ausente", "Cancelado", "Bloqueo"]) {
    expect(within(leyenda).getByText(texto)).toBeInTheDocument();
  }
});

it("Un instante en UTC se muestra en hora de Buenos Aires", async () => {
  montar("RECEPCION", () =>
    agenda("dia", HOY, HOY, {
      turnos: [turno({ inicio: "2026-12-08T02:30:00Z", fin: "2026-12-08T02:55:00Z", duracionMin: 25 })],
    }),
  );

  await screen.findByRole("region", { name: "Lucía Ferreyra" });
  const bloque = bloqueCon("Ana Prueba");
  expect(within(bloque).getByText(/23:30 a 23:55/)).toBeInTheDocument();
  // La grilla empieza a las 23:00: la fila 2 es 23:00, la 4 es 23:30.
  expect(bloque.style.gridRow).toBe("4 / 6");
  expect(screen.getByText("23:00")).toBeInTheDocument();
});

it("Se abre la ficha básica del paciente de un turno", async () => {
  const fetch = montar("ODONTOLOGO", () =>
    agenda("dia", HOY, HOY, {
      turnos: [turno({ inicio: `${HOY}T10:00:00-03:00`, fin: `${HOY}T10:30:00-03:00` })],
    }),
  );

  await userEvent.click(await screen.findByRole("button", { name: "Ver ficha de Ana Prueba" }));

  const ficha = await screen.findByRole("region", { name: "Ficha de Ana Prueba" });
  expect(within(ficha).getByText("30111222")).toBeInTheDocument();
  expect(within(ficha).getByText("11 5555-0101")).toBeInTheDocument();
  expect(within(ficha).getByText("ana.prueba@example.test")).toBeInTheDocument();
  expect(within(ficha).getByText("Obra Social Ficticia")).toBeInTheDocument();
  expect(within(ficha).getByText("AF-123")).toBeInTheDocument();
  expect(within(ficha).queryAllByRole("textbox")).toHaveLength(0);
  expect(fetch.mock.calls.some(([url]) => url === "/api/pacientes/7")).toBe(true);

  await userEvent.click(within(ficha).getByRole("button", { name: "Cerrar" }));
  expect(screen.queryByRole("region", { name: "Ficha de Ana Prueba" })).not.toBeInTheDocument();
});

it("Un fallo al cargar la ficha muestra un error", async () => {
  montar("RECEPCION", () =>
    agenda("dia", HOY, HOY, {
      turnos: [turno({ inicio: `${HOY}T10:00:00-03:00`, fin: `${HOY}T10:30:00-03:00`, pacienteId: 99 })],
    }),
  );

  await userEvent.click(await screen.findByRole("button", { name: "Ver ficha de Ana Prueba" }));

  expect(await screen.findByText("No pudimos cargar la ficha del paciente.")).toBeInTheDocument();
});

it("Un turno de otro profesional no ofrece la ficha", async () => {
  montar("ODONTOLOGO", () =>
    agenda("dia", HOY, HOY, {
      turnos: [
        turno({ inicio: `${HOY}T10:00:00-03:00`, fin: `${HOY}T10:30:00-03:00`, profesionalId: 2, paciente: null }),
      ],
    }),
  );

  const matias = await screen.findByRole("region", { name: "Matías Orellana" });
  expect(within(matias).getByText("Paciente de otro profesional")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /Ver ficha/ })).not.toBeInTheDocument();
});

it("El odontólogo ve su agenda sin acciones de escritura", async () => {
  const fetch = montar("ODONTOLOGO", (url) =>
    url.searchParams.get("vista") === "semana"
      ? agenda("semana", HOY, "2026-12-13", {
          turnos: [turno({ inicio: `${HOY}T10:00:00-03:00`, fin: `${HOY}T10:30:00-03:00` })],
          horarios: [horario(1, HOY, "09:00", "13:00")],
          bloqueos: [bloqueo({ profesionalId: 1, inicio: `${HOY}T12:00:00-03:00`, fin: `${HOY}T13:00:00-03:00`, motivo: "Capacitación" })],
        })
      : agenda("dia", HOY, HOY, {
          turnos: [turno({ inicio: `${HOY}T10:00:00-03:00`, fin: `${HOY}T10:30:00-03:00` })],
          horarios: [horario(1, HOY, "09:00", "13:00")],
          bloqueos: [bloqueo({ profesionalId: 1, inicio: `${HOY}T12:00:00-03:00`, fin: `${HOY}T13:00:00-03:00`, motivo: "Capacitación" })],
        }),
  );
  const prohibidas = /nuevo|crear|agregar|reprogram|cancelar|asistencia|atendido|ausente|guardar|eliminar|bloque|horario|libre/i;
  const sinAccionesDeEscritura = () => {
    for (const boton of screen.getAllByRole("button")) {
      expect(boton.textContent ?? "").not.toMatch(prohibidas);
      expect(boton.getAttribute("aria-label") ?? "").not.toMatch(prohibidas);
    }
  };

  await userEvent.click(await screen.findByRole("button", { name: "Ver ficha de Ana Prueba" }));
  await screen.findByRole("region", { name: "Ficha de Ana Prueba" });
  sinAccionesDeEscritura();
  await userEvent.click(screen.getByRole("button", { name: "Semana" }));
  await screen.findByRole("heading", { name: "Semana del 07/12/2026 al 13/12/2026" });
  sinAccionesDeEscritura();

  expect(escrituras(fetch)).toHaveLength(0);
});
