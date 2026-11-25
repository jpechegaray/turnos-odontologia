import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../../app/AppRutas";
import { construirRegistro } from "../../../app/rutas/registro";
import { simularSesionActiva, type ResponderSimulado } from "../../../app/sesion.fixture";
import type { Rol } from "../../../app/rutas/tipos";
import { rutas } from "../profesionales/routes";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const registro = construirRegistro({ profesionales: { rutas } });

const PEREZ = { id: 1, nombre: "Ana", apellido: "Pérez", matricula: "MN-1001", especialidad: "Ortodoncia", activo: true };

const CONSULTA = { id: 10, nombre: "Consulta", duracionDefaultMin: 30, activa: true };
const LIMPIEZA = { id: 11, nombre: "Limpieza", duracionDefaultMin: 45, activa: true };
const BLANQUEAMIENTO = { id: 12, nombre: "Blanqueamiento", duracionDefaultMin: 60, activa: false };
const ORTODONCIA = { id: 13, nombre: "Ortodoncia", duracionDefaultMin: 90, activa: false };

const URL_LISTA = "/api/profesionales";
const URL_ASOCIADAS = `/api/profesionales/${PEREZ.id}/prestaciones`;
const URL_CATALOGO = "/api/prestaciones?soloActivas=false";

type Escenario = Readonly<{
  lista?: () => Response;
  asociadas?: () => Response | Promise<Response>;
  catalogo?: () => Response;
  reemplazo?: (cuerpo: unknown) => Response | Promise<Response>;
}>;

function responder(escenario: Escenario): ResponderSimulado {
  return ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url === URL_LISTA) return escenario.lista?.() ?? Response.json([PEREZ]);
    if (metodo === "GET" && url === URL_ASOCIADAS) return escenario.asociadas?.() ?? Response.json([]);
    if (metodo === "GET" && url === URL_CATALOGO) return escenario.catalogo?.() ?? Response.json([]);
    if (metodo === "PUT" && url === URL_ASOCIADAS && escenario.reemplazo) return escenario.reemplazo(cuerpo);
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

function cuerposDeReemplazo(fetch: FetchSimulado): unknown[] {
  return fetch.mock.calls
    .filter(([u, init]) => u === URL_ASOCIADAS && metodoDe(init) === "PUT")
    .map(([, init]) => JSON.parse(String((init as RequestInit).body)) as unknown);
}

/** Usa la acción «Prestaciones» de la fila de Pérez y devuelve el panel abierto. */
async function abrirPanel(): Promise<HTMLElement> {
  const fila = await screen.findByRole("row", { name: /Pérez/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Prestaciones" }));
  return screen.findByRole("region", { name: "Prestaciones de Pérez, Ana" });
}

/** Ítem de la lista de asociadas que empieza con el nombre de la prestación. */
function itemDe(contenedor: HTMLElement, nombre: string): HTMLElement {
  const item = within(contenedor).getAllByRole("listitem").find((li) => li.textContent?.startsWith(nombre));
  if (!item) throw new Error(`No se encontró la prestación ${nombre}`);
  return item;
}

function buscarItem(contenedor: HTMLElement, nombre: string): Promise<HTMLElement> {
  return waitFor(() => itemDe(contenedor, nombre));
}

function sinControlesDeEdicion(panel: HTMLElement) {
  expect(within(panel).queryByRole("checkbox")).not.toBeInTheDocument();
  expect(within(panel).queryByRole("button", { name: "Guardar prestaciones" })).not.toBeInTheDocument();
}

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

// --- 10. Consulta y solo lectura ---

it("El panel muestra las prestaciones asociadas con su duración y la marca de inactiva", async () => {
  const fetch = montar("RECEPCION", { asociadas: () => Response.json([BLANQUEAMIENTO, CONSULTA]) });

  const panel = await abrirPanel();
  const lista = await within(panel).findByRole("list", { name: "Prestaciones asociadas" });
  const consulta = itemDe(lista, "Consulta");
  expect(consulta).toHaveTextContent("30 min");
  expect(consulta).not.toHaveTextContent("inactiva");
  const blanqueamiento = itemDe(lista, "Blanqueamiento");
  expect(blanqueamiento).toHaveTextContent("60 min");
  expect(blanqueamiento).toHaveTextContent("inactiva");
  expect(consultasA(fetch, URL_ASOCIADAS)).toHaveLength(1);
});

it("Un profesional sin prestaciones muestra un mensaje en el panel", async () => {
  montar("RECEPCION", { asociadas: () => Response.json([]) });

  const panel = await abrirPanel();
  expect(await within(panel).findByText("El profesional no tiene prestaciones asociadas.")).toBeInTheDocument();
  expect(within(panel).queryByRole("listitem")).not.toBeInTheDocument();
});

it("Un fallo al consultar las prestaciones del profesional permite reintentar", async () => {
  let intentos = 0;
  const fetch = montar("RECEPCION", {
    asociadas: () => (++intentos === 1 ? new Response(null, { status: 500 }) : Response.json([CONSULTA])),
  });

  const panel = await abrirPanel();
  const alerta = await within(panel).findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos cargar las prestaciones del profesional.");
  await userEvent.click(within(alerta).getByRole("button", { name: "Reintentar" }));
  expect(await buscarItem(panel, "Consulta")).toBeInTheDocument();
  expect(consultasA(fetch, URL_ASOCIADAS)).toHaveLength(2);
});

it("Recepción consulta las prestaciones de un profesional sin controles de edición", async () => {
  const fetch = montar("RECEPCION", { asociadas: () => Response.json([CONSULTA]) });

  const panel = await abrirPanel();
  expect(await buscarItem(panel, "Consulta")).toBeInTheDocument();
  sinControlesDeEdicion(panel);
  expect(consultasA(fetch, URL_CATALOGO)).toHaveLength(0);
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El odontólogo consulta sus propias prestaciones sin controles de edición", async () => {
  const fetch = montar("ODONTOLOGO", { asociadas: () => Response.json([CONSULTA]) });

  const panel = await abrirPanel();
  expect(screen.getAllByRole("row")).toHaveLength(2);
  expect(await buscarItem(panel, "Consulta")).toBeInTheDocument();
  sinControlesDeEdicion(panel);
  expect(consultasA(fetch, URL_CATALOGO)).toHaveLength(0);
  expect(escrituras(fetch)).toHaveLength(0);
});

// --- 11. Opciones y reemplazo ---

function casilla(panel: HTMLElement, nombre: string): HTMLElement {
  return within(panel).getByRole("checkbox", { name: new RegExp(`^${nombre}`) });
}

it("El administrador ve preseleccionadas las asociadas y solo las activas como nuevas opciones", async () => {
  const fetch = montar("ADMIN", {
    asociadas: () => Response.json([BLANQUEAMIENTO, CONSULTA]),
    catalogo: () => Response.json([BLANQUEAMIENTO, CONSULTA, LIMPIEZA, ORTODONCIA]),
  });

  const panel = await abrirPanel();
  const grupo = await within(panel).findByRole("group", { name: "Prestaciones que realiza" });
  expect(casilla(grupo, "Consulta")).toBeChecked();
  expect(casilla(grupo, "Blanqueamiento")).toBeChecked();
  expect(casilla(grupo, "Blanqueamiento")).toHaveAccessibleName(/inactiva/);
  expect(casilla(grupo, "Limpieza")).not.toBeChecked();
  expect(casilla(grupo, "Consulta")).not.toHaveAccessibleName(/inactiva/);
  expect(within(grupo).queryByRole("checkbox", { name: /Ortodoncia/ })).not.toBeInTheDocument();
  expect(within(grupo).getAllByRole("checkbox")).toHaveLength(3);
  expect(consultasA(fetch, URL_CATALOGO)).toHaveLength(1);
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El administrador guarda un nuevo conjunto de prestaciones", async () => {
  const fetch = montar("ADMIN", {
    asociadas: () => Response.json([CONSULTA]),
    catalogo: () => Response.json([CONSULTA, LIMPIEZA]),
    reemplazo: () => Response.json([CONSULTA, LIMPIEZA]),
  });

  const panel = await abrirPanel();
  await waitFor(() => casilla(panel, "Limpieza"));
  await userEvent.click(casilla(panel, "Limpieza"));
  await userEvent.click(within(panel).getByRole("button", { name: "Guardar prestaciones" }));

  expect(await within(panel).findByText("Prestaciones actualizadas.")).toBeInTheDocument();
  expect(itemDe(panel, "Limpieza")).toBeInTheDocument();
  expect(itemDe(panel, "Consulta")).toBeInTheDocument();
  const cuerpos = cuerposDeReemplazo(fetch);
  expect(cuerpos).toHaveLength(1);
  expect(escrituras(fetch)).toHaveLength(1);
  const { prestacionIds } = cuerpos[0] as { prestacionIds: number[] };
  expect([...prestacionIds].sort()).toEqual([CONSULTA.id, LIMPIEZA.id]);
  expect(new Set(prestacionIds).size).toBe(prestacionIds.length);
});

it("Quitar todas las prestaciones envía una lista vacía", async () => {
  const fetch = montar("ADMIN", {
    asociadas: () => Response.json([CONSULTA]),
    catalogo: () => Response.json([CONSULTA]),
    reemplazo: () => Response.json([]),
  });

  const panel = await abrirPanel();
  await waitFor(() => casilla(panel, "Consulta"));
  await userEvent.click(casilla(panel, "Consulta"));
  await userEvent.click(within(panel).getByRole("button", { name: "Guardar prestaciones" }));

  expect(await within(panel).findByText("El profesional no tiene prestaciones asociadas.")).toBeInTheDocument();
  expect(cuerposDeReemplazo(fetch)).toEqual([{ prestacionIds: [] }]);
});

it("Cancelar la edición de prestaciones descarta los cambios sin escrituras", async () => {
  const fetch = montar("ADMIN", {
    asociadas: () => Response.json([CONSULTA]),
    catalogo: () => Response.json([CONSULTA, LIMPIEZA]),
  });

  const panel = await abrirPanel();
  await waitFor(() => casilla(panel, "Limpieza"));
  await userEvent.click(casilla(panel, "Limpieza"));
  expect(casilla(panel, "Limpieza")).toBeChecked();
  await userEvent.click(within(panel).getByRole("button", { name: "Cancelar" }));

  expect(casilla(panel, "Limpieza")).not.toBeChecked();
  expect(casilla(panel, "Consulta")).toBeChecked();
  const lista = within(panel).getByRole("list", { name: "Prestaciones asociadas" });
  expect(within(lista).getAllByRole("listitem")).toHaveLength(1);
  expect(itemDe(lista, "Consulta")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Guardar las prestaciones dos veces seguidas emite un solo reemplazo", async () => {
  let resolver: (respuesta: Response) => void = () => undefined;
  const fetch = montar("ADMIN", {
    asociadas: () => Response.json([CONSULTA]),
    catalogo: () => Response.json([CONSULTA, LIMPIEZA]),
    reemplazo: () => new Promise<Response>((r) => { resolver = r; }),
  });

  const panel = await abrirPanel();
  await waitFor(() => casilla(panel, "Limpieza"));
  await userEvent.click(casilla(panel, "Limpieza"));
  const guardar = within(panel).getByRole("button", { name: "Guardar prestaciones" });
  await userEvent.click(guardar);
  await userEvent.click(guardar);

  await waitFor(() => expect(guardar).toBeDisabled());
  expect(cuerposDeReemplazo(fetch)).toHaveLength(1);
  resolver(Response.json([CONSULTA, LIMPIEZA]));
  expect(await within(panel).findByText("Prestaciones actualizadas.")).toBeInTheDocument();
  expect(guardar).toBeEnabled();
  expect(cuerposDeReemplazo(fetch)).toHaveLength(1);
});

// --- 12. Errores del reemplazo ---

function problema(estado: number, codigo?: string): Response {
  return Response.json({ codigo, detail: "detalle interno" }, { status: estado });
}

/** Abre el panel como `ADMIN`, marca "Limpieza" y guarda; el `PUT` responde con `respuesta`. */
async function guardarConError(respuesta: () => Response, lista?: () => Response) {
  const fetch = montar("ADMIN", {
    lista,
    asociadas: () => Response.json([CONSULTA]),
    catalogo: () => Response.json([CONSULTA, LIMPIEZA]),
    reemplazo: respuesta,
  });
  const panel = await abrirPanel();
  await waitFor(() => casilla(panel, "Limpieza"));
  await userEvent.click(casilla(panel, "Limpieza"));
  await userEvent.click(within(panel).getByRole("button", { name: "Guardar prestaciones" }));
  return { fetch, panel };
}

async function esperarRecargaConSeleccion(fetch: FetchSimulado, panel: HTMLElement) {
  await waitFor(() => expect(consultasA(fetch, URL_ASOCIADAS)).toHaveLength(2));
  await waitFor(() => expect(consultasA(fetch, URL_CATALOGO)).toHaveLength(2));
  await waitFor(() => expect(casilla(panel, "Limpieza")).toBeChecked());
  expect(casilla(panel, "Consulta")).toBeChecked();
}

it("Una prestación dada de baja mientras se editaba se muestra como alerta", async () => {
  const { fetch, panel } = await guardarConError(() => problema(422, "PRESTACION_INACTIVA"));

  const alerta = await within(panel).findByRole("alert");
  expect(alerta).toHaveTextContent("No se puede asociar una prestación dada de baja.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  await esperarRecargaConSeleccion(fetch, panel);
});

it("Una prestación inexistente al guardar se muestra como alerta", async () => {
  const { fetch, panel } = await guardarConError(() => problema(422, "PRESTACION_INEXISTENTE"));

  const alerta = await within(panel).findByRole("alert");
  expect(alerta).toHaveTextContent("Alguna de las prestaciones elegidas ya no existe.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  await esperarRecargaConSeleccion(fetch, panel);
});

it("Un conflicto al guardar prestaciones pide reintentar", async () => {
  const { fetch, panel } = await guardarConError(() => problema(409, "PROFESIONAL_PRESTACIONES_CONFLICTO"));

  const alerta = await within(panel).findByRole("alert");
  expect(alerta).toHaveTextContent("Otra persona modificó estas prestaciones. Volvé a intentarlo.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  await esperarRecargaConSeleccion(fetch, panel);
});

it("Un 403 al guardar prestaciones se muestra como permiso insuficiente", async () => {
  const { fetch, panel } = await guardarConError(() => problema(403));

  const alerta = await within(panel).findByRole("alert");
  expect(alerta).toHaveTextContent("No tenés permiso para realizar esta acción.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(casilla(panel, "Limpieza")).toBeChecked();
  expect(screen.getByRole("navigation", { name: "Principal" })).toBeInTheDocument();
  expect(fetch.mock.calls.filter(([url]) => String(url).includes("/api/auth/logout"))).toHaveLength(0);
});

it("Guardar prestaciones de un profesional inexistente refresca la lista de profesionales", async () => {
  let existe = true;
  const { fetch } = await guardarConError(
    () => {
      existe = false;
      return problema(404, "PROFESIONAL_NO_ENCONTRADO");
    },
    () => Response.json(existe ? [PEREZ] : []),
  );

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("El profesional ya no existe.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  await waitFor(() => expect(consultasA(fetch, URL_LISTA)).toHaveLength(2));
  expect(await screen.findByText("No hay profesionales cargados.")).toBeInTheDocument();
  expect(screen.getByRole("alert")).toHaveTextContent("El profesional ya no existe.");
});
