import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../app/AppRutas";
import { construirRegistro } from "../../app/rutas/registro";
import type { Rol } from "../../app/rutas/tipos";
import { simularSesionActiva, type ResponderSimulado } from "../../app/sesion.fixture";
import { rutas } from "./routes";
import {
  MENSAJE_ERROR_CARGA,
  MENSAJE_PERMISO,
  MENSAJE_SIN_COINCIDENCIAS,
  MENSAJE_SIN_ENVIOS,
  MENSAJE_TURNO_INVALIDO,
} from "./textos";
import type { NotificacionRespuesta, PaginaNotificaciones } from "./tipos";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const registro = construirRegistro({ notificaciones: { rutas } });

const BASE: NotificacionRespuesta = {
  id: 1,
  tipo: "RECORDATORIO",
  estado: "ENVIADA",
  canal: "EMAIL",
  destinatario: "ana.ficticia@example.test",
  turnoId: 42,
  ofertaHuecoId: null,
  pacienteId: 5,
  inicioReferencia: "2026-12-04T13:00:00Z",
  intentos: 1,
  ultimoError: null,
  programadaPara: null,
  enviadaEn: "2026-12-03T13:05:00Z",
};

function pagina(contenido: NotificacionRespuesta[], extra: Partial<PaginaNotificaciones> = {}): PaginaNotificaciones {
  return {
    contenido,
    pagina: 0,
    tamanio: 20,
    totalElementos: contenido.length,
    totalPaginas: contenido.length === 0 ? 0 : 1,
    ...extra,
  };
}

function responderCon(respuesta: PaginaNotificaciones | ((url: string) => Response)): ResponderSimulado {
  return ({ url }) => {
    if (!url.startsWith("/api/notificaciones")) return Response.json({});
    return typeof respuesta === "function" ? respuesta(url) : Response.json(respuesta);
  };
}

function montar(rol: Rol, responder: ResponderSimulado): FetchSimulado {
  const { fetch } = simularSesionActiva(rol, responder);
  render(
    <MemoryRouter initialEntries={["/notificaciones"]}>
      <AppRutas registro={registro} />
    </MemoryRouter>,
  );
  return fetch;
}

function consultas(fetch: FetchSimulado): string[] {
  return fetch.mock.calls.map(([url]) => String(url)).filter((url) => url.startsWith("/api/notificaciones"));
}

async function tabla() {
  return screen.findByRole("table", { name: "Registro de notificaciones" });
}

async function filaCon(texto: string) {
  const celda = await within(await tabla()).findByText(texto);
  const fila = celda.closest("tr");
  if (!fila) throw new Error(`No hay fila con ${texto}`);
  return within(fila);
}

afterEach(() => {
  vi.unstubAllGlobals();
});

// --- Acceso ---

it("La pantalla de notificaciones aparece en el menú para administración y recepción", async () => {
  for (const rol of ["ADMIN", "RECEPCION"] as const) {
    simularSesionActiva(rol, responderCon(pagina([])));
    const { unmount } = render(
      <MemoryRouter initialEntries={["/notificaciones"]}>
        <AppRutas registro={registro} />
      </MemoryRouter>,
    );
    const navegacion = await screen.findByRole("navigation", { name: "Principal" });
    expect(within(navegacion).getByRole("link", { name: "Notificaciones" })).toHaveAttribute(
      "href",
      "/notificaciones",
    );
    unmount();
    vi.unstubAllGlobals();
  }
});

it("El odontólogo no ve la pantalla de notificaciones", async () => {
  const fetch = montar("ODONTOLOGO", responderCon(pagina([])));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No tenés permiso");
  const navegacion = screen.getByRole("navigation", { name: "Principal" });
  expect(within(navegacion).queryByRole("link", { name: "Notificaciones" })).not.toBeInTheDocument();
  expect(consultas(fetch)).toHaveLength(0);
});

// --- Listado ---

it("Al abrir la pantalla se lista la primera página del registro", async () => {
  const fetch = montar(
    "RECEPCION",
    responderCon(
      pagina([
        { ...BASE, id: 3, estado: "PENDIENTE", destinatario: "c@example.test", enviadaEn: null },
        { ...BASE, id: 2, estado: "ENVIADA", destinatario: "b@example.test" },
        { ...BASE, id: 1, estado: "FALLIDA", destinatario: "a@example.test", ultimoError: "Error ficticio" },
      ]),
    ),
  );

  const filas = within(await tabla()).getAllByRole("row").slice(1);
  expect(consultas(fetch)).toEqual(["/api/notificaciones?pagina=0&tamanio=20"]);
  expect(filas.map((fila) => within(fila).getAllByRole("cell")[2].textContent)).toEqual([
    "c@example.test",
    "b@example.test",
    "a@example.test",
  ]);
  expect(filas.map((fila) => within(fila).getAllByRole("cell")[1].textContent)).toEqual([
    "Pendiente",
    "Enviada",
    "Fallida",
  ]);
});

it("Los datos del envío se muestran en español y en hora de Buenos Aires", async () => {
  montar("RECEPCION", responderCon(pagina([BASE])));

  const fila = await filaCon("#42");
  expect(fila.getByText("Recordatorio")).toBeInTheDocument();
  expect(fila.getByText("04/12/2026 10:00")).toBeInTheDocument();
  expect(fila.getByText("1")).toBeInTheDocument();
  expect(fila.getByText("03/12/2026 10:05")).toBeInTheDocument();
});

it("Un aviso de hueco sin turno se identifica como tal", async () => {
  montar("RECEPCION", responderCon(pagina([{ ...BASE, tipo: "OFERTA_HUECO", turnoId: null, ofertaHuecoId: 7 }])));

  const fila = await filaCon(BASE.destinatario);
  expect(fila.getAllByText("Aviso de hueco")).toHaveLength(2);
});

it("Un envío pendiente muestra la marca de vacío en la fecha de envío", async () => {
  montar("RECEPCION", responderCon(pagina([{ ...BASE, estado: "PENDIENTE", enviadaEn: null }])));

  const fila = await filaCon("Pendiente");
  const celdas = fila.getAllByRole("cell");
  expect(celdas[6]).toHaveTextContent("—");
});

it("Un envío fallido muestra su último error", async () => {
  montar(
    "RECEPCION",
    responderCon(
      pagina([
        {
          ...BASE,
          estado: "FALLIDA",
          intentos: 3,
          enviadaEn: null,
          ultimoError: "No se pudo enviar la notificación por email",
        },
      ]),
    ),
  );

  const fila = await filaCon("Fallida");
  expect(fila.getByText("3")).toBeInTheDocument();
  expect(fila.getByText("No se pudo enviar la notificación por email")).toBeInTheDocument();
});

it("Un envío enviado no muestra error", async () => {
  montar("RECEPCION", responderCon(pagina([{ ...BASE, ultimoError: "no debería verse" }])));

  const fila = await filaCon("Enviada");
  expect(fila.getAllByRole("cell")[7]).toHaveTextContent("—");
  expect(fila.queryByText("no debería verse")).not.toBeInTheDocument();
});

// --- Filtros ---

it("Filtrar por estado envía solo ese filtro", async () => {
  const usuario = userEvent.setup();
  const fetch = montar("RECEPCION", responderCon(pagina([BASE])));
  await tabla();

  await usuario.selectOptions(screen.getByLabelText("Estado"), "Fallida");
  await usuario.click(screen.getByRole("button", { name: "Buscar" }));

  await vi.waitFor(() =>
    expect(consultas(fetch).at(-1)).toBe("/api/notificaciones?estado=FALLIDA&pagina=0&tamanio=20"),
  );
});

it("Los filtros de estado y turno se combinan", async () => {
  const usuario = userEvent.setup();
  const fetch = montar("RECEPCION", responderCon(pagina([BASE])));
  await tabla();

  await usuario.selectOptions(screen.getByLabelText("Estado"), "Enviada");
  await usuario.type(screen.getByLabelText("Turno"), " 42 ");
  await usuario.click(screen.getByRole("button", { name: "Buscar" }));

  await vi.waitFor(() =>
    expect(consultas(fetch).at(-1)).toBe("/api/notificaciones?estado=ENVIADA&turnoId=42&pagina=0&tamanio=20"),
  );
});

it("Un número de turno inválido no consulta", async () => {
  const usuario = userEvent.setup();
  const fetch = montar("RECEPCION", responderCon(pagina([BASE])));
  await tabla();

  for (const texto of ["abc", "0"]) {
    await usuario.clear(screen.getByLabelText("Turno"));
    await usuario.type(screen.getByLabelText("Turno"), texto);
    await usuario.click(screen.getByRole("button", { name: "Buscar" }));

    expect(await screen.findByText(MENSAJE_TURNO_INVALIDO)).toBeInTheDocument();
    expect(consultas(fetch)).toHaveLength(1);
  }
});

it("Buscar vuelve a la primera página", async () => {
  const usuario = userEvent.setup();
  const fetch = montar(
    "RECEPCION",
    responderCon((url) => {
      const numero = Number(new URL(url, "http://local").searchParams.get("pagina"));
      return Response.json(pagina([{ ...BASE, id: numero + 1 }], { pagina: numero, totalPaginas: 3, totalElementos: 45 }));
    }),
  );
  await tabla();
  await usuario.click(screen.getByRole("button", { name: "Siguiente" }));
  await screen.findByText("Página 2 de 3 · 45 envíos");

  await usuario.selectOptions(screen.getByLabelText("Estado"), "Pendiente");
  await usuario.click(screen.getByRole("button", { name: "Buscar" }));

  await vi.waitFor(() =>
    expect(consultas(fetch).at(-1)).toBe("/api/notificaciones?estado=PENDIENTE&pagina=0&tamanio=20"),
  );
});

it("Una búsqueda sin coincidencias muestra un mensaje", async () => {
  const usuario = userEvent.setup();
  montar(
    "RECEPCION",
    responderCon((url) => Response.json(url.includes("estado=") ? pagina([]) : pagina([BASE]))),
  );
  await tabla();

  await usuario.selectOptions(screen.getByLabelText("Estado"), "Fallida");
  await usuario.click(screen.getByRole("button", { name: "Buscar" }));

  expect(await screen.findByText(MENSAJE_SIN_COINCIDENCIAS)).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Siguiente" })).not.toBeInTheDocument();
});

it("Sin envíos registrados se muestra un mensaje", async () => {
  montar("RECEPCION", responderCon(pagina([])));

  expect(await screen.findByText(MENSAJE_SIN_ENVIOS)).toBeInTheDocument();
});

// --- Paginación ---

it("Siguiente consulta la página contigua con los filtros vigentes", async () => {
  const usuario = userEvent.setup();
  const fetch = montar(
    "RECEPCION",
    responderCon((url) => {
      const numero = Number(new URL(url, "http://local").searchParams.get("pagina"));
      return Response.json(pagina([{ ...BASE, id: numero + 1 }], { pagina: numero, totalPaginas: 2, totalElementos: 25 }));
    }),
  );
  await tabla();
  await usuario.selectOptions(screen.getByLabelText("Estado"), "Fallida");
  await usuario.click(screen.getByRole("button", { name: "Buscar" }));
  await screen.findByText("Página 1 de 2 · 25 envíos");

  await usuario.click(screen.getByRole("button", { name: "Siguiente" }));

  expect(await screen.findByText("Página 2 de 2 · 25 envíos")).toBeInTheDocument();
  expect(consultas(fetch).at(-1)).toBe("/api/notificaciones?estado=FALLIDA&pagina=1&tamanio=20");
  expect(screen.getByRole("button", { name: "Siguiente" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Anterior" })).toBeEnabled();
});

it("Los botones se deshabilitan en los extremos", async () => {
  montar("RECEPCION", responderCon(pagina([BASE, { ...BASE, id: 2 }, { ...BASE, id: 3 }])));

  expect(await screen.findByText("Página 1 de 1 · 3 envíos")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Siguiente" })).toBeDisabled();
});

// --- Errores ---

it("Un error de carga ofrece reintentar", async () => {
  const usuario = userEvent.setup();
  let llamadas = 0;
  const fetch = montar(
    "RECEPCION",
    responderCon(() => {
      llamadas += 1;
      return llamadas === 1 ? new Response(null, { status: 500 }) : Response.json(pagina([BASE]));
    }),
  );

  expect(await screen.findByText(MENSAJE_ERROR_CARGA)).toBeInTheDocument();
  expect(screen.queryByRole("table")).not.toBeInTheDocument();
  await usuario.click(screen.getByRole("button", { name: "Reintentar" }));

  await tabla();
  expect(consultas(fetch)).toEqual([
    "/api/notificaciones?pagina=0&tamanio=20",
    "/api/notificaciones?pagina=0&tamanio=20",
  ]);
  expect(screen.queryByText(MENSAJE_ERROR_CARGA)).not.toBeInTheDocument();
});

it("Un 403 de la API muestra el aviso de permiso", async () => {
  montar(
    "RECEPCION",
    responderCon(() =>
      Response.json({ status: 403, title: "Forbidden" }, { status: 403, headers: { "Content-Type": "application/problem+json" } }),
    ),
  );

  expect(await screen.findByText(MENSAJE_PERMISO)).toBeInTheDocument();
  expect(screen.queryByRole("table")).not.toBeInTheDocument();
});
