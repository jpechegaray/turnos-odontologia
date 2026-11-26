import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppRutas } from "../../../app/AppRutas";
import { construirRegistro } from "../../../app/rutas/registro";
import { simularSesionActiva, type ResponderSimulado, type SolicitudSimulada } from "../../../app/sesion.fixture";
import type { Rol } from "../../../app/rutas/tipos";
import { rutas } from "../profesionales/routes";
import type { TramoHorario } from "./tipos";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const registro = construirRegistro({ profesionales: { rutas } });

const PEREZ = { id: 1, nombre: "Ana", apellido: "Pérez", matricula: "MN-1001", especialidad: "Ortodoncia", activo: true };

const URL_LISTA = "/api/profesionales";
const URL_HORARIOS = `/api/profesionales/${PEREZ.id}/horarios`;

const tramo = (id: number, diaSemana: number, horaInicio: string, horaFin: string): TramoHorario => ({
  id,
  profesionalId: PEREZ.id,
  diaSemana,
  horaInicio,
  horaFin,
});

const LUNES_MANANA = tramo(5, 1, "09:00", "13:00");

type Escenario = Readonly<{
  lista?: () => Response;
  horarios?: () => Response | Promise<Response>;
  /** Respuesta a `POST`, `PUT` o `DELETE` sobre los horarios. */
  escritura?: (solicitud: SolicitudSimulada) => Response | Promise<Response>;
}>;

function responder(escenario: Escenario): ResponderSimulado {
  return (solicitud) => {
    const { url, metodo } = solicitud;
    if (metodo === "GET" && url === URL_LISTA) return escenario.lista?.() ?? Response.json([PEREZ]);
    if (metodo === "GET" && url === URL_HORARIOS) return escenario.horarios?.() ?? Response.json([]);
    if (metodo !== "GET" && url.startsWith(URL_HORARIOS)) {
      return escenario.escritura?.(solicitud) ?? new Response(null, { status: 204 });
    }
    return Response.json({});
  };
}

function montar(rol: Rol, escenario: Escenario): FetchSimulado {
  const { fetch } = simularSesionActiva(rol, responder(escenario));
  render(
    <MemoryRouter initialEntries={["/profesionales"]}>
      <AppRutas registro={registro} />
    </MemoryRouter>,
  );
  return fetch;
}

function metodoDe(init: unknown): string {
  return (init as RequestInit | undefined)?.method ?? "GET";
}

function escrituras(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(([, init]) => metodoDe(init) !== "GET");
}

function consultasA(fetch: FetchSimulado, url: string) {
  return fetch.mock.calls.filter(([u, init]) => u === url && metodoDe(init) === "GET");
}

function escrituraA(fetch: FetchSimulado, metodo: string) {
  return fetch.mock.calls
    .filter(([, init]) => metodoDe(init) === metodo)
    .map(([u, init]) => ({
      url: String(u),
      cuerpo: (init as RequestInit).body ? (JSON.parse(String((init as RequestInit).body)) as unknown) : undefined,
    }));
}

/** El explorador muestra los horarios del primer profesional al abrirse. */
async function abrirPanel(): Promise<HTMLElement> {
  return screen.findByRole("region", { name: "Horarios de Pérez, Ana" });
}

function dia(panel: HTMLElement, nombre: string): HTMLElement {
  return within(panel).getByRole("group", { name: nombre });
}

function textosDeTramos(panel: HTMLElement, nombre: string): string[] {
  return within(dia(panel, nombre))
    .queryAllByRole("listitem")
    .map((li) => li.querySelector(".tramo__horas")?.textContent ?? "");
}

function sinControlesDeEdicion(panel: HTMLElement) {
  expect(within(panel).queryByRole("form")).not.toBeInTheDocument();
  for (const nombre of ["Agregar tramo", "Editar", "Eliminar"]) {
    expect(within(panel).queryByRole("button", { name: nombre })).not.toBeInTheDocument();
  }
}

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

describe("horarios: consulta", () => {
  it("El panel agrupa los tramos por día y los ordena por hora de inicio", async () => {
    const fetch = montar("RECEPCION", {
      horarios: () => Response.json([tramo(1, 1, "13:00", "17:00"), tramo(2, 3, "09:00", "13:00"), tramo(3, 1, "08:00", "12:00")]),
    });

    const panel = await abrirPanel();
    await waitFor(() => expect(textosDeTramos(panel, "Lunes")).toEqual(["08:00 a 12:00", "13:00 a 17:00"]));
    expect(textosDeTramos(panel, "Miércoles")).toEqual(["09:00 a 13:00"]);
    for (const nombre of ["Martes", "Jueves", "Viernes", "Sábado", "Domingo"]) {
      expect(dia(panel, nombre)).toHaveTextContent("No atiende");
    }
    expect(consultasA(fetch, URL_HORARIOS)).toHaveLength(1);
  });

  it("Un profesional sin tramos muestra todos los días sin atención", async () => {
    montar("RECEPCION", { horarios: () => Response.json([]) });

    const panel = await abrirPanel();
    await waitFor(() => expect(within(panel).getAllByRole("group")).toHaveLength(7));
    for (const grupo of within(panel).getAllByRole("group")) {
      expect(grupo).toHaveTextContent("No atiende");
    }
  });

  it("Un fallo al consultar los horarios permite reintentar", async () => {
    let intentos = 0;
    const fetch = montar("RECEPCION", {
      horarios: () => (++intentos === 1 ? new Response(null, { status: 500 }) : Response.json([LUNES_MANANA])),
    });

    const panel = await abrirPanel();
    const alerta = await within(panel).findByRole("alert");
    expect(alerta).toHaveTextContent("No pudimos cargar los horarios del profesional.");
    await userEvent.click(within(alerta).getByRole("button", { name: "Reintentar" }));
    await waitFor(() => expect(textosDeTramos(panel, "Lunes")).toEqual(["09:00 a 13:00"]));
    expect(consultasA(fetch, URL_HORARIOS)).toHaveLength(2);
  });

  it("El odontólogo ve sus horarios sin controles de edición", async () => {
    const fetch = montar("ODONTOLOGO", { horarios: () => Response.json([LUNES_MANANA]) });

    const panel = await abrirPanel();
    await waitFor(() => expect(textosDeTramos(panel, "Lunes")).toEqual(["09:00 a 13:00"]));
    sinControlesDeEdicion(panel);
    expect(escrituras(fetch)).toHaveLength(0);
  });
});

// --- Edición ---

function problema(estado: number, codigo?: string): Response {
  return Response.json({ codigo, detail: "detalle interno" }, { status: estado });
}

/** Respuestas sucesivas del listado de horarios: la última se repite. */
function secuencia(...listas: readonly (readonly TramoHorario[])[]): () => Response {
  let llamada = 0;
  return () => Response.json(listas[Math.min(llamada++, listas.length - 1)]);
}

type DatosTramo = Readonly<{ dia?: string; desde: string; hasta: string }>;

async function completarTramo(formulario: HTMLElement, { dia: nombreDia, desde, hasta }: DatosTramo) {
  if (nombreDia) await userEvent.selectOptions(within(formulario).getByLabelText("Día"), nombreDia);
  fireEvent.change(within(formulario).getByLabelText("Desde"), { target: { value: desde } });
  fireEvent.change(within(formulario).getByLabelText("Hasta"), { target: { value: hasta } });
}

async function agregarTramo(panel: HTMLElement, datos: DatosTramo) {
  const formulario = await within(panel).findByRole("form", { name: "Nuevo tramo" });
  await completarTramo(formulario, datos);
  await userEvent.click(within(formulario).getByRole("button", { name: "Agregar tramo" }));
}

async function itemDeTramo(panel: HTMLElement, nombreDia: string, horas: string): Promise<HTMLElement> {
  return waitFor(() => {
    const item = within(dia(panel, nombreDia))
      .getAllByRole("listitem")
      .find((li) => li.querySelector(".tramo__horas")?.textContent === horas);
    if (!item) throw new Error(`No está el tramo ${horas} del ${nombreDia}`);
    return item;
  });
}

describe("horarios: alta, modificación y eliminación", () => {
  it("Recepción ve los controles de edición de horarios", async () => {
    montar("RECEPCION", { horarios: () => Response.json([LUNES_MANANA]) });

    const panel = await abrirPanel();
    const item = await itemDeTramo(panel, "Lunes", "09:00 a 13:00");
    expect(within(panel).getByRole("form", { name: "Nuevo tramo" })).toBeInTheDocument();
    expect(within(panel).getByRole("button", { name: "Agregar tramo" })).toBeInTheDocument();
    expect(within(item).getByRole("button", { name: "Editar" })).toBeInTheDocument();
    expect(within(item).getByRole("button", { name: "Eliminar" })).toBeInTheDocument();
  });

  it("Agregar un tramo lo envía con el día ISO y lo muestra en su día", async () => {
    const fetch = montar("RECEPCION", {
      horarios: secuencia([], [tramo(9, 3, "09:00", "13:00")]),
      escritura: () => Response.json(tramo(9, 3, "09:00", "13:00"), { status: 201 }),
    });

    const panel = await abrirPanel();
    await agregarTramo(panel, { dia: "Miércoles", desde: "09:00", hasta: "13:00" });

    expect(await within(panel).findByRole("status")).toHaveTextContent("Tramo agregado.");
    await itemDeTramo(panel, "Miércoles", "09:00 a 13:00");
    expect(escrituraA(fetch, "POST")).toEqual([
      { url: URL_HORARIOS, cuerpo: { diaSemana: 3, horaInicio: "09:00", horaFin: "13:00" } },
    ]);
    expect(consultasA(fetch, URL_HORARIOS)).toHaveLength(2);
  });

  it("Un fin igual o anterior al inicio bloquea el envío sin llamar a la API", async () => {
    const fetch = montar("RECEPCION", { horarios: () => Response.json([]) });

    const panel = await abrirPanel();
    await agregarTramo(panel, { dia: "Lunes", desde: "12:00", hasta: "12:00" });
    expect(await within(panel).findByRole("alert")).toHaveTextContent("La hora de fin debe ser posterior a la de inicio.");

    await agregarTramo(panel, { dia: "Lunes", desde: "12:00", hasta: "09:00" });
    expect(within(panel).getByRole("alert")).toHaveTextContent("La hora de fin debe ser posterior a la de inicio.");
    expect(escrituras(fetch)).toHaveLength(0);
  });

  it("Un tramo superpuesto con otro del mismo día bloquea el envío sin llamar a la API", async () => {
    const fetch = montar("RECEPCION", { horarios: () => Response.json([LUNES_MANANA]) });

    const panel = await abrirPanel();
    await itemDeTramo(panel, "Lunes", "09:00 a 13:00");
    await agregarTramo(panel, { dia: "Lunes", desde: "12:00", hasta: "14:00" });

    expect(await within(panel).findByRole("alert")).toHaveTextContent("El tramo se superpone con el de 09:00 a 13:00.");
    expect(escrituras(fetch)).toHaveLength(0);
  });

  it("Un tramo contiguo o de otro día se acepta", async () => {
    const fetch = montar("RECEPCION", {
      horarios: () => Response.json([LUNES_MANANA]),
      escritura: ({ cuerpo }) => Response.json({ id: 20, profesionalId: PEREZ.id, ...(cuerpo as object) }, { status: 201 }),
    });

    const panel = await abrirPanel();
    await itemDeTramo(panel, "Lunes", "09:00 a 13:00");
    await agregarTramo(panel, { dia: "Lunes", desde: "13:00", hasta: "17:00" });
    await waitFor(() => expect(escrituraA(fetch, "POST")).toHaveLength(1));
    await agregarTramo(panel, { dia: "Martes", desde: "09:00", hasta: "13:00" });
    await waitFor(() => expect(escrituraA(fetch, "POST")).toHaveLength(2));

    expect(escrituraA(fetch, "POST").map((e) => e.cuerpo)).toEqual([
      { diaSemana: 1, horaInicio: "13:00", horaFin: "17:00" },
      { diaSemana: 2, horaInicio: "09:00", horaFin: "13:00" },
    ]);
    expect(within(panel).queryByText(/se superpone/)).not.toBeInTheDocument();
  });

  it("Modificar un tramo envía PUT y refleja los nuevos valores", async () => {
    const fetch = montar("ADMIN", {
      horarios: secuencia([LUNES_MANANA], [tramo(5, 1, "10:00", "14:00")]),
      escritura: () => Response.json(tramo(5, 1, "10:00", "14:00")),
    });

    const panel = await abrirPanel();
    const item = await itemDeTramo(panel, "Lunes", "09:00 a 13:00");
    await userEvent.click(within(item).getByRole("button", { name: "Editar" }));
    const formulario = within(panel).getByRole("form", { name: "Modificar tramo" });
    expect(within(formulario).getByLabelText("Desde")).toHaveValue("09:00");
    await completarTramo(formulario, { desde: "10:00", hasta: "14:00" });
    await userEvent.click(within(formulario).getByRole("button", { name: "Guardar cambios" }));

    await itemDeTramo(panel, "Lunes", "10:00 a 14:00");
    expect(escrituraA(fetch, "PUT")).toEqual([
      { url: `${URL_HORARIOS}/5`, cuerpo: { diaSemana: 1, horaInicio: "10:00", horaFin: "14:00" } },
    ]);
    expect(await within(panel).findByRole("status")).toHaveTextContent("Tramo actualizado.");
    expect(within(panel).getByRole("form", { name: "Nuevo tramo" })).toBeInTheDocument();
  });

  it("Eliminar un tramo pide confirmación y lo retira", async () => {
    const fetch = montar("RECEPCION", { horarios: secuencia([LUNES_MANANA], []) });

    const panel = await abrirPanel();
    const item = await itemDeTramo(panel, "Lunes", "09:00 a 13:00");
    await userEvent.click(within(item).getByRole("button", { name: "Eliminar" }));
    expect(escrituras(fetch)).toHaveLength(0);
    await userEvent.click(within(item).getByRole("button", { name: "Confirmar eliminación" }));

    await waitFor(() => expect(dia(panel, "Lunes")).toHaveTextContent("No atiende"));
    expect(escrituraA(fetch, "DELETE")).toEqual([{ url: `${URL_HORARIOS}/5`, cuerpo: undefined }]);
  });

  it("Cancelar la eliminación no escribe", async () => {
    const fetch = montar("RECEPCION", { horarios: () => Response.json([LUNES_MANANA]) });

    const panel = await abrirPanel();
    const item = await itemDeTramo(panel, "Lunes", "09:00 a 13:00");
    await userEvent.click(within(item).getByRole("button", { name: "Eliminar" }));
    await userEvent.click(within(item).getByRole("button", { name: "Cancelar" }));

    expect(within(item).getByRole("button", { name: "Eliminar" })).toBeInTheDocument();
    expect(textosDeTramos(panel, "Lunes")).toEqual(["09:00 a 13:00"]);
    expect(escrituras(fetch)).toHaveLength(0);
  });
});

describe("horarios: errores de la API", () => {
  it("Un 409 del servidor muestra la superposición y recarga los tramos", async () => {
    const fetch = montar("RECEPCION", {
      horarios: () => Response.json([]),
      escritura: () => problema(409, "HORARIO_SUPERPUESTO"),
    });

    const panel = await abrirPanel();
    await agregarTramo(panel, { dia: "Lunes", desde: "09:00", hasta: "13:00" });

    const alerta = await within(panel).findByRole("alert");
    expect(alerta).toHaveTextContent("El tramo se superpone con otro del mismo día.");
    expect(alerta).not.toHaveTextContent("detalle interno");
    await waitFor(() => expect(consultasA(fetch, URL_HORARIOS)).toHaveLength(2));
  });

  it("Un tramo que ya no existe avisa y recarga", async () => {
    const fetch = montar("RECEPCION", {
      horarios: secuencia([LUNES_MANANA], []),
      escritura: () => problema(404, "HORARIO_NO_ENCONTRADO"),
    });

    const panel = await abrirPanel();
    const item = await itemDeTramo(panel, "Lunes", "09:00 a 13:00");
    await userEvent.click(within(item).getByRole("button", { name: "Eliminar" }));
    await userEvent.click(within(item).getByRole("button", { name: "Confirmar eliminación" }));

    expect(await within(panel).findByRole("alert")).toHaveTextContent("El tramo ya no existe.");
    await waitFor(() => expect(consultasA(fetch, URL_HORARIOS)).toHaveLength(2));
    await waitFor(() => expect(dia(panel, "Lunes")).toHaveTextContent("No atiende"));
  });

  it("Un profesional que ya no existe refresca la lista de profesionales", async () => {
    let listas = 0;
    const fetch = montar("RECEPCION", {
      lista: () => Response.json(++listas === 1 ? [PEREZ] : []),
      horarios: () => Response.json([]),
      escritura: () => problema(404, "PROFESIONAL_NO_ENCONTRADO"),
    });

    const panel = await abrirPanel();
    await agregarTramo(panel, { dia: "Lunes", desde: "09:00", hasta: "13:00" });

    expect(await screen.findByRole("alert")).toHaveTextContent("El profesional ya no existe.");
    await waitFor(() => expect(consultasA(fetch, URL_LISTA)).toHaveLength(2));
    await waitFor(() => expect(screen.queryByRole("region", { name: "Horarios de Pérez, Ana" })).not.toBeInTheDocument());
  });

  it("Un 403 al escribir un tramo muestra permiso insuficiente sin cerrar la sesión", async () => {
    const fetch = montar("RECEPCION", {
      horarios: () => Response.json([]),
      escritura: () => problema(403),
    });

    const panel = await abrirPanel();
    await agregarTramo(panel, { dia: "Lunes", desde: "09:00", hasta: "13:00" });

    const alerta = await within(panel).findByRole("alert");
    expect(alerta).toHaveTextContent("No tenés permiso para realizar esta acción.");
    expect(alerta).not.toHaveTextContent("detalle interno");
    expect(screen.getByRole("navigation", { name: "Principal" })).toBeInTheDocument();
    expect(fetch.mock.calls.filter(([url]) => String(url).includes("/api/auth/logout"))).toHaveLength(0);
  });
});
