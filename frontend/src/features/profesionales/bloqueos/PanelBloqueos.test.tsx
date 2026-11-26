import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppRutas } from "../../../app/AppRutas";
import { construirRegistro } from "../../../app/rutas/registro";
import { simularSesionActiva, type ResponderSimulado, type SolicitudSimulada } from "../../../app/sesion.fixture";
import type { Rol } from "../../../app/rutas/tipos";
import { rutas } from "../profesionales/routes";
import type { Bloqueo } from "./tipos";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const registro = construirRegistro({ profesionales: { rutas } });

const PEREZ = { id: 1, nombre: "Ana", apellido: "Pérez", matricula: "MN-1001", especialidad: "Ortodoncia", activo: true };

const URL_LISTA = "/api/profesionales";
const URL_BLOQUEOS = `/api/profesionales/${PEREZ.id}/bloqueos`;

const CONGRESO: Bloqueo = {
  id: 8,
  profesionalId: PEREZ.id,
  inicio: "2026-12-01T13:00:00Z",
  fin: "2026-12-01T21:00:00Z",
  motivo: "Congreso",
};

type Escenario = Readonly<{
  lista?: () => Response;
  bloqueos?: () => Response | Promise<Response>;
  /** Respuesta a `POST` o `DELETE` sobre los bloqueos. */
  escritura?: (solicitud: SolicitudSimulada) => Response | Promise<Response>;
}>;

function responder(escenario: Escenario): ResponderSimulado {
  return (solicitud) => {
    const { url, metodo } = solicitud;
    if (metodo === "GET" && url === URL_LISTA) return escenario.lista?.() ?? Response.json([PEREZ]);
    if (metodo === "GET" && url === URL_BLOQUEOS) return escenario.bloqueos?.() ?? Response.json([]);
    if (metodo !== "GET" && url.startsWith(URL_BLOQUEOS)) {
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

function problema(estado: number, codigo?: string): Response {
  return Response.json({ codigo, detail: "detalle interno" }, { status: estado });
}

/** Respuestas sucesivas del listado de bloqueos: la última se repite. */
function secuencia(...listas: readonly (readonly Bloqueo[])[]): () => Response {
  let llamada = 0;
  return () => Response.json(listas[Math.min(llamada++, listas.length - 1)]);
}

/** Abre la pestaña de bloqueos del profesional seleccionado. */
async function abrirPanel(): Promise<HTMLElement> {
  await userEvent.click(await screen.findByRole("tab", { name: "Bloqueos" }));
  return screen.findByRole("region", { name: "Bloqueos de Pérez, Ana" });
}

function itemDeBloqueo(panel: HTMLElement, motivo: string): Promise<HTMLElement> {
  return waitFor(() => {
    const item = within(panel)
      .getAllByRole("listitem")
      .find((li) => li.querySelector(".bloqueo__motivo")?.textContent === motivo);
    if (!item) throw new Error(`No está el bloqueo ${motivo}`);
    return item;
  });
}

type DatosBloqueo = Readonly<{
  fechaInicio: string;
  horaInicio: string;
  fechaFin: string;
  horaFin: string;
  motivo: string;
}>;

const DATOS_CONGRESO: DatosBloqueo = {
  fechaInicio: "2026-12-01",
  horaInicio: "10:00",
  fechaFin: "2026-12-01",
  horaFin: "18:00",
  motivo: " Congreso ",
};

async function agregarBloqueo(panel: HTMLElement, datos: DatosBloqueo) {
  const formulario = await within(panel).findByRole("form", { name: "Nuevo bloqueo" });
  const campos: readonly [string, string][] = [
    ["Fecha de inicio", datos.fechaInicio],
    ["Hora de inicio", datos.horaInicio],
    ["Fecha de fin", datos.fechaFin],
    ["Hora de fin", datos.horaFin],
    ["Motivo", datos.motivo],
  ];
  for (const [etiqueta, valor] of campos) {
    fireEvent.change(within(formulario).getByLabelText(etiqueta), { target: { value: valor } });
  }
  await userEvent.click(within(formulario).getByRole("button", { name: "Agregar bloqueo" }));
}

function sinControlesDeEdicion(panel: HTMLElement) {
  expect(within(panel).queryByRole("form")).not.toBeInTheDocument();
  for (const nombre of ["Agregar bloqueo", "Eliminar"]) {
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

describe("bloqueos: consulta", () => {
  it("El panel muestra los bloqueos en hora de Buenos Aires", async () => {
    const fetch = montar("RECEPCION", { bloqueos: () => Response.json([CONGRESO]) });

    const panel = await abrirPanel();
    const item = await itemDeBloqueo(panel, "Congreso");
    expect(item).toHaveTextContent("01/12/2026 10:00 al 01/12/2026 18:00");
    expect(consultasA(fetch, URL_BLOQUEOS)).toHaveLength(1);
  });

  it("Un profesional sin bloqueos muestra un mensaje", async () => {
    montar("RECEPCION", { bloqueos: () => Response.json([]) });

    const panel = await abrirPanel();
    expect(await within(panel).findByText("El profesional no tiene bloqueos.")).toBeInTheDocument();
    expect(within(panel).queryByRole("listitem")).not.toBeInTheDocument();
  });

  it("Un fallo al consultar los bloqueos permite reintentar", async () => {
    let intentos = 0;
    const fetch = montar("RECEPCION", {
      bloqueos: () => (++intentos === 1 ? new Response(null, { status: 500 }) : Response.json([CONGRESO])),
    });

    const panel = await abrirPanel();
    const alerta = await within(panel).findByRole("alert");
    expect(alerta).toHaveTextContent("No pudimos cargar los bloqueos del profesional.");
    await userEvent.click(within(alerta).getByRole("button", { name: "Reintentar" }));
    await itemDeBloqueo(panel, "Congreso");
    expect(consultasA(fetch, URL_BLOQUEOS)).toHaveLength(2);
  });

  it("El odontólogo ve sus bloqueos sin controles de edición", async () => {
    const fetch = montar("ODONTOLOGO", { bloqueos: () => Response.json([{ ...CONGRESO, motivo: "Vacaciones" }]) });

    const panel = await abrirPanel();
    await itemDeBloqueo(panel, "Vacaciones");
    sinControlesDeEdicion(panel);
    expect(escrituras(fetch)).toHaveLength(0);
  });
});

describe("bloqueos: alta y eliminación", () => {
  it("Recepción ve los controles de bloqueos", async () => {
    montar("RECEPCION", { bloqueos: () => Response.json([CONGRESO]) });

    const panel = await abrirPanel();
    const item = await itemDeBloqueo(panel, "Congreso");
    expect(within(panel).getByRole("form", { name: "Nuevo bloqueo" })).toBeInTheDocument();
    expect(within(panel).getByRole("button", { name: "Agregar bloqueo" })).toBeInTheDocument();
    expect(within(item).getByRole("button", { name: "Eliminar" })).toBeInTheDocument();
  });

  it("Agregar un bloqueo envía los instantes en UTC y lo muestra", async () => {
    const fetch = montar("RECEPCION", {
      bloqueos: secuencia([], [CONGRESO]),
      escritura: () => Response.json(CONGRESO, { status: 201 }),
    });

    const panel = await abrirPanel();
    await agregarBloqueo(panel, DATOS_CONGRESO);

    expect(await within(panel).findByRole("status")).toHaveTextContent("Bloqueo agregado.");
    await itemDeBloqueo(panel, "Congreso");
    expect(escrituraA(fetch, "POST")).toEqual([
      {
        url: URL_BLOQUEOS,
        cuerpo: { inicio: "2026-12-01T13:00:00.000Z", fin: "2026-12-01T21:00:00.000Z", motivo: "Congreso" },
      },
    ]);
    expect(consultasA(fetch, URL_BLOQUEOS)).toHaveLength(2);
    expect(within(panel).getByLabelText("Motivo")).toHaveValue("");
  });

  it("Un fin igual o anterior al inicio bloquea el envío sin llamar a la API", async () => {
    const fetch = montar("RECEPCION", { bloqueos: () => Response.json([]) });

    const panel = await abrirPanel();
    await agregarBloqueo(panel, { ...DATOS_CONGRESO, horaInicio: "18:00", horaFin: "10:00" });

    expect(await within(panel).findByText("El fin debe ser posterior al inicio.")).toBeInTheDocument();
    expect(within(panel).getByLabelText("Hora de fin")).toHaveAttribute("aria-invalid", "true");
    expect(escrituras(fetch)).toHaveLength(0);
  });

  it("Un motivo vacío o demasiado largo bloquea el envío sin llamar a la API", async () => {
    const fetch = montar("RECEPCION", { bloqueos: () => Response.json([]) });

    const panel = await abrirPanel();
    await agregarBloqueo(panel, { ...DATOS_CONGRESO, motivo: "   " });
    expect(await within(panel).findByText("Ingresá el motivo del bloqueo.")).toBeInTheDocument();

    await agregarBloqueo(panel, { ...DATOS_CONGRESO, motivo: "x".repeat(256) });
    expect(await within(panel).findByText("El motivo no puede superar los 255 caracteres.")).toBeInTheDocument();
    expect(within(panel).getByLabelText("Motivo")).toHaveAttribute("aria-invalid", "true");
    expect(escrituras(fetch)).toHaveLength(0);
  });

  it("Eliminar un bloqueo pide confirmación y lo retira", async () => {
    const fetch = montar("ADMIN", { bloqueos: secuencia([CONGRESO], []) });

    const panel = await abrirPanel();
    const item = await itemDeBloqueo(panel, "Congreso");
    await userEvent.click(within(item).getByRole("button", { name: "Eliminar" }));
    expect(escrituras(fetch)).toHaveLength(0);
    await userEvent.click(within(item).getByRole("button", { name: "Confirmar eliminación" }));

    expect(await within(panel).findByText("El profesional no tiene bloqueos.")).toBeInTheDocument();
    expect(escrituraA(fetch, "DELETE")).toEqual([{ url: `${URL_BLOQUEOS}/${CONGRESO.id}`, cuerpo: undefined }]);
  });

  it("Cancelar la eliminación de un bloqueo no escribe", async () => {
    const fetch = montar("RECEPCION", { bloqueos: () => Response.json([CONGRESO]) });

    const panel = await abrirPanel();
    const item = await itemDeBloqueo(panel, "Congreso");
    await userEvent.click(within(item).getByRole("button", { name: "Eliminar" }));
    await userEvent.click(within(item).getByRole("button", { name: "Cancelar" }));

    expect(within(item).getByRole("button", { name: "Eliminar" })).toBeInTheDocument();
    expect(escrituras(fetch)).toHaveLength(0);
  });
});

describe("bloqueos: errores de la API", () => {
  it("Un bloqueo que ya no existe avisa y recarga", async () => {
    const fetch = montar("RECEPCION", {
      bloqueos: secuencia([CONGRESO], []),
      escritura: () => problema(404, "BLOQUEO_NO_ENCONTRADO"),
    });

    const panel = await abrirPanel();
    const item = await itemDeBloqueo(panel, "Congreso");
    await userEvent.click(within(item).getByRole("button", { name: "Eliminar" }));
    await userEvent.click(within(item).getByRole("button", { name: "Confirmar eliminación" }));

    expect(await within(panel).findByRole("alert")).toHaveTextContent("El bloqueo ya no existe.");
    await waitFor(() => expect(consultasA(fetch, URL_BLOQUEOS)).toHaveLength(2));
    expect(await within(panel).findByText("El profesional no tiene bloqueos.")).toBeInTheDocument();
  });

  it("Un profesional que ya no existe refresca la lista al escribir un bloqueo", async () => {
    let listas = 0;
    const fetch = montar("RECEPCION", {
      lista: () => Response.json(++listas === 1 ? [PEREZ] : []),
      bloqueos: () => Response.json([]),
      escritura: () => problema(404, "PROFESIONAL_NO_ENCONTRADO"),
    });

    const panel = await abrirPanel();
    await agregarBloqueo(panel, DATOS_CONGRESO);

    expect(await screen.findByRole("alert")).toHaveTextContent("El profesional ya no existe.");
    await waitFor(() => expect(consultasA(fetch, URL_LISTA)).toHaveLength(2));
    await waitFor(() => expect(screen.queryByRole("region", { name: "Bloqueos de Pérez, Ana" })).not.toBeInTheDocument());
  });

  it("Un conflicto del servidor conserva lo ingresado", async () => {
    montar("RECEPCION", {
      bloqueos: () => Response.json([]),
      escritura: () => problema(409, "BLOQUEO_CON_TURNOS"),
    });

    const panel = await abrirPanel();
    await agregarBloqueo(panel, DATOS_CONGRESO);

    const alerta = await within(panel).findByRole("alert");
    expect(alerta).toHaveTextContent("No se puede crear el bloqueo en ese intervalo.");
    expect(alerta).not.toHaveTextContent("detalle interno");
    expect(within(panel).getByLabelText("Fecha de inicio")).toHaveValue("2026-12-01");
    expect(within(panel).getByLabelText("Hora de fin")).toHaveValue("18:00");
    expect(within(panel).getByLabelText("Motivo")).toHaveValue(" Congreso ");
  });

  it("Un 403 al escribir un bloqueo muestra permiso insuficiente sin cerrar la sesión", async () => {
    const fetch = montar("RECEPCION", {
      bloqueos: () => Response.json([CONGRESO]),
      escritura: () => problema(403),
    });

    const panel = await abrirPanel();
    const item = await itemDeBloqueo(panel, "Congreso");
    await userEvent.click(within(item).getByRole("button", { name: "Eliminar" }));
    await userEvent.click(within(item).getByRole("button", { name: "Confirmar eliminación" }));

    const alerta = await within(panel).findByRole("alert");
    expect(alerta).toHaveTextContent("No tenés permiso para realizar esta acción.");
    expect(alerta).not.toHaveTextContent("detalle interno");
    expect(screen.getByRole("navigation", { name: "Principal" })).toBeInTheDocument();
    expect(fetch.mock.calls.filter(([url]) => String(url).includes("/api/auth/logout"))).toHaveLength(0);
  });
});
