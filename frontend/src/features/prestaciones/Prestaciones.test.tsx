import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../app/AppRutas";
import { construirRegistro } from "../../app/rutas/registro";
import { simularSesionActiva, type ResponderSimulado } from "../../app/sesion.fixture";
import type { Rol } from "../../app/rutas/tipos";
import { PaginaPrestaciones } from "./PaginaPrestaciones";
import { rutas as rutasPrestaciones } from "./routes";
import type { PrestacionRespuesta } from "./tipos";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const registro = construirRegistro({
  prestaciones: { rutas: [{ ruta: "/prestaciones", componente: PaginaPrestaciones }] },
});
const registroReal = construirRegistro({ prestaciones: { rutas: rutasPrestaciones } });

const LIMPIEZA = { id: 1, nombre: "Limpieza", duracionDefaultMin: 30, activa: true };
const EXTRACCION = { id: 2, nombre: "Extracción", duracionDefaultMin: 45, activa: false };

function montar(rol: Rol, responder: ResponderSimulado, registroUsado = registro): FetchSimulado {
  const { fetch } = simularSesionActiva(rol, responder);
  render(
    <MemoryRouter initialEntries={["/prestaciones"]}>
      <AppRutas registro={registroUsado} />
    </MemoryRouter>,
  );
  return fetch;
}

function escrituras(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(([, init]) => ((init as RequestInit | undefined)?.method ?? "GET") !== "GET");
}

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

it("El catálogo lista las prestaciones con su duración y estado", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json([LIMPIEZA, EXTRACCION]);
    return Response.json({});
  });

  expect(await screen.findByRole("cell", { name: "Limpieza" })).toBeInTheDocument();
  expect(screen.getByRole("cell", { name: "30" })).toBeInTheDocument();
  expect(screen.getByRole("cell", { name: "Activa" })).toBeInTheDocument();
  expect(screen.getByRole("cell", { name: "Extracción" })).toBeInTheDocument();
  expect(screen.getByRole("cell", { name: "45" })).toBeInTheDocument();
  expect(screen.getByRole("cell", { name: "Inactiva" })).toBeInTheDocument();
  expect(fetch.mock.calls.some(([url]) => url === "/api/prestaciones?soloActivas=false")).toBe(true);
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El catálogo vacío muestra un mensaje", async () => {
  montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json([]);
    return Response.json({});
  });

  expect(await screen.findByText("No hay prestaciones cargadas.")).toBeInTheDocument();
  expect(screen.queryByRole("cell", { name: "Limpieza" })).not.toBeInTheDocument();
});

it("Un fallo al listar permite reintentar", async () => {
  let intentos = 0;
  montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) {
      return ++intentos === 1 ? new Response(null, { status: 500 }) : Response.json([LIMPIEZA]);
    }
    return Response.json({});
  });

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos cargar las prestaciones.");
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(await screen.findByRole("cell", { name: "Limpieza" })).toBeInTheDocument();
});

it("Una duración que no es múltiplo de 5 bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json([]);
    return Response.json({});
  });

  await enviarPrestacion("Limpieza", "7");

  expect(await screen.findByText("La duración debe ser un múltiplo de 5 entre 5 y 480 minutos.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Una duración menor a 5 bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json([]);
    return Response.json({});
  });

  await enviarPrestacion("Limpieza", "0");

  expect(await screen.findByText("La duración debe ser un múltiplo de 5 entre 5 y 480 minutos.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Una duración mayor a 480 bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json([]);
    return Response.json({});
  });

  await enviarPrestacion("Limpieza", "481");

  expect(await screen.findByText("La duración debe ser un múltiplo de 5 entre 5 y 480 minutos.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Una duración no entera bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json([]);
    return Response.json({});
  });

  await enviarPrestacion("Limpieza", "7,5");

  expect(await screen.findByText("La duración debe ser un múltiplo de 5 entre 5 y 480 minutos.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Un nombre vacío bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json([]);
    return Response.json({});
  });

  await enviarPrestacion("", "30");

  expect(await screen.findByText("El nombre es obligatorio.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Las duraciones límite 5 y 480 se aceptan", async () => {
  const lista: PrestacionRespuesta[] = [];
  const fetch = montar("ADMIN", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json(lista);
    if (metodo === "POST" && url === "/api/prestaciones") {
      const datos = cuerpo as { nombre: string; duracionDefaultMin: number };
      const nueva = { id: lista.length + 1, nombre: datos.nombre, duracionDefaultMin: datos.duracionDefaultMin, activa: true };
      lista.push(nueva);
      return Response.json(nueva, { status: 201 });
    }
    return Response.json({});
  });

  await enviarPrestacion("Control", "5");
  expect(await screen.findByRole("cell", { name: "Control" })).toBeInTheDocument();

  await enviarPrestacion("Cirugía", "480");
  expect(await screen.findByRole("cell", { name: "Cirugía" })).toBeInTheDocument();

  const altas = escrituras(fetch).filter(([, init]) => (init as RequestInit).method === "POST");
  expect(altas.map(([, init]) => JSON.parse(String((init as RequestInit).body)))).toEqual([
    { nombre: "Control", duracionDefaultMin: 5 },
    { nombre: "Cirugía", duracionDefaultMin: 480 },
  ]);
});

it("El administrador da de alta una prestación y la lista se refresca", async () => {
  let lista = [LIMPIEZA];
  const fetch = montar("ADMIN", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json(lista);
    if (metodo === "POST" && url === "/api/prestaciones") {
      const datos = cuerpo as { nombre: string; duracionDefaultMin: number };
      const nueva = { id: 3, nombre: datos.nombre, duracionDefaultMin: datos.duracionDefaultMin, activa: true };
      lista = [...lista, nueva];
      return Response.json(nueva, { status: 201 });
    }
    return Response.json({});
  });

  await enviarPrestacion("Blanqueamiento", "60");

  expect(await screen.findByRole("cell", { name: "Blanqueamiento" })).toBeInTheDocument();
  const post = escrituras(fetch).find(([, init]) => (init as RequestInit).method === "POST");
  expect(post).toBeDefined();
  expect(JSON.parse(String((post?.[1] as RequestInit).body))).toEqual({
    nombre: "Blanqueamiento",
    duracionDefaultMin: 60,
  });
  const consultas = fetch.mock.calls.filter(
    ([url, init]) => url === "/api/prestaciones?soloActivas=false" && ((init as RequestInit | undefined)?.method ?? "GET") === "GET",
  );
  expect(consultas.length).toBeGreaterThanOrEqual(2);
});

it("Un nombre duplicado se muestra como alerta sin perder los datos", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json([LIMPIEZA]);
    if (metodo === "POST" && url === "/api/prestaciones") {
      return Response.json({ codigo: "PRESTACION-DUPLICADA", detail: "detalle interno" }, { status: 409 });
    }
    return Response.json({});
  });

  await enviarPrestacion("Limpieza", "30");

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("Ya existe una prestación con ese nombre.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(screen.getByLabelText("Nombre")).toHaveValue("Limpieza");
  expect(screen.getByLabelText("Duración (min)")).toHaveValue("30");
  expect(consultasPrestaciones(fetch)).toHaveLength(1);
});

it("Un 422 de duración del backend se muestra como alerta", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json([LIMPIEZA]);
    if (metodo === "POST" && url === "/api/prestaciones") {
      return Response.json({ codigo: "RN-09", detail: "detalle interno" }, { status: 422 });
    }
    return Response.json({});
  });

  await enviarPrestacion("Limpieza", "30");

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("La duración debe ser un múltiplo de 5 entre 5 y 480 minutos.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(screen.getByLabelText("Nombre")).toHaveValue("Limpieza");
  expect(consultasPrestaciones(fetch)).toHaveLength(1);
});

it("El administrador edita una prestación y la lista se refresca", async () => {
  let lista = [LIMPIEZA, EXTRACCION];
  const fetch = montar("ADMIN", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json(lista);
    if (metodo === "PUT" && url === "/api/prestaciones/1") {
      const datos = cuerpo as { nombre: string; duracionDefaultMin: number; activa: boolean };
      lista = lista.map((p) => (p.id === 1 ? { ...p, ...datos } : p));
      return Response.json(lista.find((p) => p.id === 1));
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Limpieza/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));

  const campoNombre = screen.getByLabelText("Nombre");
  const campoDuracion = screen.getByLabelText("Duración (min)");
  expect(campoNombre).toHaveValue("Limpieza");
  expect(campoDuracion).toHaveValue("30");

  await userEvent.clear(campoNombre);
  await userEvent.type(campoNombre, "Limpieza profunda");
  await userEvent.clear(campoDuracion);
  await userEvent.type(campoDuracion, "40");
  await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

  expect(await screen.findByRole("cell", { name: "Limpieza profunda" })).toBeInTheDocument();
  const put = escrituras(fetch).find(([, init]) => (init as RequestInit).method === "PUT");
  expect(put?.[0]).toBe("/api/prestaciones/1");
  expect(JSON.parse(String((put?.[1] as RequestInit).body))).toEqual({
    nombre: "Limpieza profunda",
    duracionDefaultMin: 40,
    activa: true,
  });
  expect(consultasPrestaciones(fetch).length).toBeGreaterThanOrEqual(2);
});

it("El administrador reactiva una prestación dada de baja", async () => {
  let lista: PrestacionRespuesta[] = [{ ...EXTRACCION }];
  const fetch = montar("ADMIN", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json(lista);
    if (metodo === "PUT" && url === "/api/prestaciones/2") {
      const datos = cuerpo as { nombre: string; duracionDefaultMin: number; activa: boolean };
      lista = lista.map((p) => (p.id === 2 ? { ...p, ...datos } : p));
      return Response.json(lista[0]);
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Extracción/ });
  expect(within(fila).getByRole("cell", { name: "Inactiva" })).toBeInTheDocument();
  await userEvent.click(within(fila).getByRole("button", { name: "Reactivar" }));

  const put = await waitFor(() => {
    const encontrado = escrituras(fetch).find(([, init]) => (init as RequestInit).method === "PUT");
    expect(encontrado).toBeDefined();
    return encontrado;
  });
  expect(put?.[0]).toBe("/api/prestaciones/2");
  expect(JSON.parse(String((put?.[1] as RequestInit).body))).toEqual({
    nombre: "Extracción",
    duracionDefaultMin: 45,
    activa: true,
  });
  expect(await screen.findByRole("cell", { name: "Activa" })).toBeInTheDocument();
});

it("Editar una prestación inexistente muestra una alerta y refresca la lista", async () => {
  let lista: PrestacionRespuesta[] = [LIMPIEZA];
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json(lista);
    if (metodo === "PUT" && url === "/api/prestaciones/1") {
      lista = [];
      return Response.json({ codigo: "PRESTACION-NO-ENCONTRADA", detail: "detalle interno" }, { status: 404 });
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Limpieza/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));
  const campoNombre = screen.getByLabelText("Nombre");
  await userEvent.clear(campoNombre);
  await userEvent.type(campoNombre, "Limpieza profunda");
  await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("La prestación ya no existe.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(consultasPrestaciones(fetch).length).toBeGreaterThanOrEqual(2);
});

it("El administrador da de baja una prestación y sigue visible como inactiva", async () => {
  let lista: PrestacionRespuesta[] = [{ ...LIMPIEZA }];
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json(lista);
    if (metodo === "DELETE" && url === "/api/prestaciones/1") {
      lista = lista.map((p) => (p.id === 1 ? { ...p, activa: false } : p));
      return new Response(null, { status: 204 });
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Limpieza/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Dar de baja" }));
  await userEvent.click(screen.getByRole("button", { name: "Confirmar baja" }));

  expect(await screen.findByRole("cell", { name: "Inactiva" })).toBeInTheDocument();
  const borrado = escrituras(fetch).find(([, init]) => (init as RequestInit).method === "DELETE");
  expect(borrado?.[0]).toBe("/api/prestaciones/1");
  expect(consultasPrestaciones(fetch).length).toBeGreaterThanOrEqual(2);
});

it("Dar de baja una prestación inexistente muestra una alerta", async () => {
  let lista: PrestacionRespuesta[] = [{ ...LIMPIEZA }];
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json(lista);
    if (metodo === "DELETE" && url === "/api/prestaciones/1") {
      lista = [];
      return Response.json({ codigo: "PRESTACION-NO-ENCONTRADA", detail: "detalle interno" }, { status: 404 });
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Limpieza/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Dar de baja" }));
  await userEvent.click(screen.getByRole("button", { name: "Confirmar baja" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("La prestación ya no existe.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(consultasPrestaciones(fetch).length).toBeGreaterThanOrEqual(2);
});

it("El administrador ve los controles de alta, edición y baja", async () => {
  montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/prestaciones")) {
      return Response.json([LIMPIEZA, { id: 3, nombre: "Ortodoncia", duracionDefaultMin: 60, activa: true }]);
    }
    return Response.json({});
  }, registroReal);

  expect(await screen.findByRole("button", { name: "Agregar prestación" })).toBeInTheDocument();
  expect(screen.getAllByRole("button", { name: "Editar" })).toHaveLength(2);
  expect(screen.getAllByRole("button", { name: "Dar de baja" })).toHaveLength(2);
});

const responderCatalogo: ResponderSimulado = ({ url, metodo }) => {
  if (metodo === "GET" && url.startsWith("/api/prestaciones")) return Response.json([LIMPIEZA, EXTRACCION]);
  return Response.json({});
};

it("Recepción consulta el catálogo sin controles de escritura", async () => {
  const fetch = montar("RECEPCION", responderCatalogo, registroReal);

  expect(await screen.findByRole("cell", { name: "Limpieza" })).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Agregar prestación" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Editar" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Dar de baja" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Reactivar" })).not.toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El odontólogo consulta el catálogo sin controles de escritura", async () => {
  const fetch = montar("ODONTOLOGO", responderCatalogo, registroReal);

  expect(await screen.findByRole("cell", { name: "Limpieza" })).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Agregar prestación" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Editar" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Dar de baja" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Reactivar" })).not.toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("La pantalla de prestaciones aparece en el menú para los tres roles", async () => {
  for (const rol of ["ADMIN", "RECEPCION", "ODONTOLOGO"] as const) {
    simularSesionActiva(rol, responderCatalogo);
    const { unmount } = render(
      <MemoryRouter initialEntries={["/prestaciones"]}>
        <AppRutas registro={registroReal} />
      </MemoryRouter>,
    );
    const navegacion = await screen.findByRole("navigation", { name: "Principal" });
    expect(within(navegacion).getByRole("link", { name: "Prestaciones" })).toHaveAttribute("href", "/prestaciones");
    unmount();
    vi.unstubAllGlobals();
  }
});

function consultasPrestaciones(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(
    ([url, init]) => url === "/api/prestaciones?soloActivas=false" && ((init as RequestInit | undefined)?.method ?? "GET") === "GET",
  );
}

async function enviarPrestacion(nombre: string, duracion: string) {
  const campoNombre = await screen.findByLabelText("Nombre");
  const campoDuracion = screen.getByLabelText("Duración (min)");
  await userEvent.clear(campoNombre);
  await userEvent.clear(campoDuracion);
  if (nombre) await userEvent.type(campoNombre, nombre);
  if (duracion) await userEvent.type(campoDuracion, duracion);
  await userEvent.click(screen.getByRole("button", { name: "Agregar prestación" }));
  await waitFor(() => expect(screen.getByRole("button", { name: "Agregar prestación" })).toBeEnabled());
}
