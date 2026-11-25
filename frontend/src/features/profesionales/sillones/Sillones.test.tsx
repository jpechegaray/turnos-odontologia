import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../../app/AppRutas";
import { construirRegistro } from "../../../app/rutas/registro";
import type { Rol } from "../../../app/rutas/tipos";
import { simularSesionActiva, type ResponderSimulado } from "../../../app/sesion.fixture";
import { rutas } from "./routes";
import type { SillonRespuesta } from "./tipos";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const registro = construirRegistro({ profesionales: { rutas } });

const SILLON_2 = { id: 2, nombre: "Sillón 2", descripcion: "Junto a la ventana", activo: true };
const SILLON_1 = { id: 1, nombre: "Sillón 1", descripcion: null, activo: false };

function montar(rol: Rol, responder: ResponderSimulado): FetchSimulado {
  const { fetch } = simularSesionActiva(rol, responder);
  render(
    <MemoryRouter initialEntries={["/sillones"]}>
      <AppRutas registro={registro} />
    </MemoryRouter>,
  );
  return fetch;
}

function escrituras(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(([, init]) => ((init as RequestInit | undefined)?.method ?? "GET") !== "GET");
}

function consultasSillones(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(
    ([url, init]) => url === "/api/sillones" && ((init as RequestInit | undefined)?.method ?? "GET") === "GET",
  );
}

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

it("El listado muestra los sillones con su descripción y estado", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json([SILLON_2, SILLON_1]);
    return Response.json({});
  });

  const primera = await screen.findByRole("row", { name: /Sillón 1/ });
  const filas = screen.getAllByRole("row").slice(1);
  expect(filas.map((f) => within(f).getAllByRole("cell")[0]?.textContent)).toEqual(["Sillón 1", "Sillón 2"]);
  expect(within(primera).getByRole("cell", { name: "—" })).toBeInTheDocument();
  expect(within(primera).getByRole("cell", { name: "Inactivo" })).toBeInTheDocument();
  const segunda = screen.getByRole("row", { name: /Sillón 2/ });
  expect(within(segunda).getByRole("cell", { name: "Junto a la ventana" })).toBeInTheDocument();
  expect(within(segunda).getByRole("cell", { name: "Activo" })).toBeInTheDocument();
  expect(consultasSillones(fetch).length).toBeGreaterThanOrEqual(1);
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El listado vacío de sillones muestra un mensaje", async () => {
  montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json([]);
    return Response.json({});
  });

  expect(await screen.findByText("No hay sillones cargados.")).toBeInTheDocument();
  expect(screen.queryByRole("cell", { name: "Sillón 1" })).not.toBeInTheDocument();
});

it("Un fallo al listar sillones permite reintentar", async () => {
  let intentos = 0;
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/sillones") {
      return ++intentos === 1 ? new Response(null, { status: 500 }) : Response.json([SILLON_1]);
    }
    return Response.json({});
  });

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos cargar los sillones.");
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(await screen.findByRole("cell", { name: "Sillón 1" })).toBeInTheDocument();
  expect(consultasSillones(fetch)).toHaveLength(2);
});

it("Un nombre de sillón en blanco bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json([]);
    return Response.json({});
  });

  await completarFormulario("   ", "Con lámpara nueva");
  await userEvent.click(screen.getByRole("button", { name: "Agregar sillón" }));

  const campoNombre = screen.getByLabelText("Nombre");
  expect(await screen.findByText("El nombre es obligatorio.")).toBeInTheDocument();
  expect(campoNombre).toHaveAttribute("aria-invalid", "true");
  expect(escrituras(fetch)).toHaveLength(0);
});

async function completarFormulario(nombre: string, descripcion: string) {
  const campoNombre = await screen.findByLabelText("Nombre");
  const campoDescripcion = screen.getByLabelText("Descripción");
  await userEvent.clear(campoNombre);
  await userEvent.clear(campoDescripcion);
  if (nombre) await userEvent.type(campoNombre, nombre);
  if (descripcion) await userEvent.type(campoDescripcion, descripcion);
}

it("El administrador da de alta un sillón y la lista se refresca", async () => {
  let lista: SillonRespuesta[] = [SILLON_1];
  const fetch = montar("ADMIN", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json(lista);
    if (metodo === "POST" && url === "/api/sillones") {
      const datos = cuerpo as { nombre: string; descripcion: string | null };
      const nuevo = { id: 3, ...datos, activo: true };
      lista = [...lista, nuevo];
      return Response.json(nuevo, { status: 201 });
    }
    return Response.json({});
  });

  await completarFormulario(" Box 3 ", "");
  await userEvent.click(screen.getByRole("button", { name: "Agregar sillón" }));

  expect(await screen.findByRole("cell", { name: "Box 3" })).toBeInTheDocument();
  const post = escrituras(fetch).find(([, init]) => (init as RequestInit).method === "POST");
  expect(post?.[0]).toBe("/api/sillones");
  expect(JSON.parse(String((post?.[1] as RequestInit).body))).toEqual({ nombre: "Box 3", descripcion: null });
  expect(consultasSillones(fetch).length).toBeGreaterThanOrEqual(2);
  expect(screen.getByLabelText("Nombre")).toHaveValue("");
  expect(screen.getByLabelText("Descripción")).toHaveValue("");
  expect(await screen.findByText("Sillón agregado.")).toBeInTheDocument();
});

it("Un nombre de sillón duplicado se muestra como alerta sin perder los datos", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json([SILLON_1]);
    if (metodo === "POST" && url === "/api/sillones") {
      return Response.json({ codigo: "SILLON_NOMBRE_DUPLICADO", detail: "detalle interno" }, { status: 409 });
    }
    return Response.json({});
  });

  await completarFormulario("Sillón 1", "Otro box");
  await userEvent.click(screen.getByRole("button", { name: "Agregar sillón" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("Ya existe un sillón con ese nombre.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(screen.getByLabelText("Nombre")).toHaveValue("Sillón 1");
  expect(screen.getByLabelText("Descripción")).toHaveValue("Otro box");
  expect(escrituras(fetch)).toHaveLength(1);
});

it("El administrador edita un sillón y la lista se refresca", async () => {
  let lista: SillonRespuesta[] = [SILLON_1, SILLON_2];
  const fetch = montar("ADMIN", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json(lista);
    if (metodo === "PUT" && url === "/api/sillones/2") {
      const datos = cuerpo as { nombre: string; descripcion: string | null; activo: boolean };
      lista = lista.map((s) => (s.id === 2 ? { ...s, ...datos } : s));
      return Response.json(lista.find((s) => s.id === 2));
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Sillón 2/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));
  expect(screen.getByLabelText("Nombre")).toHaveValue("Sillón 2");
  expect(screen.getByLabelText("Descripción")).toHaveValue("Junto a la ventana");
  await completarFormulario("Sillón 2", "Con lámpara nueva");
  await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

  expect(await screen.findByRole("cell", { name: "Con lámpara nueva" })).toBeInTheDocument();
  const put = escrituras(fetch).find(([, init]) => (init as RequestInit).method === "PUT");
  expect(put?.[0]).toBe("/api/sillones/2");
  expect(JSON.parse(String((put?.[1] as RequestInit).body))).toEqual({
    nombre: "Sillón 2",
    descripcion: "Con lámpara nueva",
    activo: true,
  });
  expect(consultasSillones(fetch).length).toBeGreaterThanOrEqual(2);
});

it("Editar un sillón inexistente muestra una alerta y refresca la lista", async () => {
  let lista: SillonRespuesta[] = [SILLON_2];
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json(lista);
    if (metodo === "PUT" && url === "/api/sillones/2") {
      lista = [];
      return Response.json({ codigo: "SILLON_NO_ENCONTRADO", detail: "detalle interno" }, { status: 404 });
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Sillón 2/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));
  await completarFormulario("Sillón 2", "Con lámpara nueva");
  await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("El sillón ya no existe.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(consultasSillones(fetch).length).toBeGreaterThanOrEqual(2);
});

it("El administrador da de baja un sillón y sigue visible como inactivo", async () => {
  let lista: SillonRespuesta[] = [{ ...SILLON_2 }];
  const fetch = montar("ADMIN", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json(lista);
    if (metodo === "PUT" && url === "/api/sillones/2") {
      const datos = cuerpo as { nombre: string; descripcion: string | null; activo: boolean };
      lista = lista.map((s) => (s.id === 2 ? { ...s, ...datos } : s));
      return Response.json(lista[0]);
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Sillón 2/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Dar de baja" }));
  await userEvent.click(within(fila).getByRole("button", { name: "Confirmar baja" }));

  expect(await screen.findByRole("cell", { name: "Inactivo" })).toBeInTheDocument();
  expect(screen.getByRole("cell", { name: "Sillón 2" })).toBeInTheDocument();
  const put = escrituras(fetch).find(([, init]) => (init as RequestInit).method === "PUT");
  expect(put?.[0]).toBe("/api/sillones/2");
  expect(JSON.parse(String((put?.[1] as RequestInit).body))).toEqual({
    nombre: "Sillón 2",
    descripcion: "Junto a la ventana",
    activo: false,
  });
  expect(consultasSillones(fetch).length).toBeGreaterThanOrEqual(2);
});

it("El administrador reactiva un sillón dado de baja", async () => {
  let lista: SillonRespuesta[] = [{ ...SILLON_1 }];
  const fetch = montar("ADMIN", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json(lista);
    if (metodo === "PUT" && url === "/api/sillones/1") {
      const datos = cuerpo as { nombre: string; descripcion: string | null; activo: boolean };
      lista = lista.map((s) => (s.id === 1 ? { ...s, ...datos } : s));
      return Response.json(lista[0]);
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Sillón 1/ });
  expect(within(fila).getByRole("cell", { name: "Inactivo" })).toBeInTheDocument();
  await userEvent.click(within(fila).getByRole("button", { name: "Reactivar" }));

  expect(await screen.findByRole("cell", { name: "Activo" })).toBeInTheDocument();
  const put = escrituras(fetch).find(([, init]) => (init as RequestInit).method === "PUT");
  expect(put?.[0]).toBe("/api/sillones/1");
  expect(JSON.parse(String((put?.[1] as RequestInit).body))).toEqual({
    nombre: "Sillón 1",
    descripcion: null,
    activo: true,
  });
});

it("Un error al dar de baja un sillón se muestra como alerta", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json([SILLON_2]);
    if (metodo === "PUT" && url === "/api/sillones/2") {
      return Response.json({ codigo: "RN-37", detail: "detalle interno" }, { status: 422 });
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Sillón 2/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Dar de baja" }));
  await userEvent.click(within(fila).getByRole("button", { name: "Confirmar baja" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos completar la operación. Intentá de nuevo.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  await waitFor(() => expect(consultasSillones(fetch).length).toBeGreaterThanOrEqual(2));
});

const responderListado: ResponderSimulado = ({ url, metodo }) => {
  if (metodo === "GET" && url === "/api/sillones") return Response.json([SILLON_1, SILLON_2]);
  return Response.json({});
};

it("El administrador ve los controles de escritura de sillones", async () => {
  montar("ADMIN", responderListado);

  expect(await screen.findByRole("button", { name: "Agregar sillón" })).toBeInTheDocument();
  const inactivo = await screen.findByRole("row", { name: /Sillón 1/ });
  expect(within(inactivo).getByRole("button", { name: "Editar" })).toBeInTheDocument();
  expect(within(inactivo).getByRole("button", { name: "Reactivar" })).toBeInTheDocument();
  const activo = screen.getByRole("row", { name: /Sillón 2/ });
  expect(within(activo).getByRole("button", { name: "Editar" })).toBeInTheDocument();
  expect(within(activo).getByRole("button", { name: "Dar de baja" })).toBeInTheDocument();
});

it.each(["RECEPCION", "ODONTOLOGO"] as const)(
  "Recepción y odontología consultan los sillones sin controles de escritura",
  async (rol) => {
    const fetch = montar(rol, responderListado);

    expect(await screen.findByRole("cell", { name: "Sillón 1" })).toBeInTheDocument();
    expect(screen.queryByLabelText("Nombre")).not.toBeInTheDocument();
    for (const nombre of ["Agregar sillón", "Editar", "Dar de baja", "Reactivar"]) {
      expect(screen.queryByRole("button", { name: nombre })).not.toBeInTheDocument();
    }
    expect(screen.queryByRole("columnheader", { name: "Acciones" })).not.toBeInTheDocument();
    expect(escrituras(fetch)).toHaveLength(0);
  },
);

it("La pantalla de sillones aparece en el menú para los tres roles", async () => {
  for (const rol of ["ADMIN", "RECEPCION", "ODONTOLOGO"] as const) {
    simularSesionActiva(rol, responderListado);
    const { unmount } = render(
      <MemoryRouter initialEntries={["/sillones"]}>
        <AppRutas registro={registro} />
      </MemoryRouter>,
    );
    const navegacion = await screen.findByRole("navigation", { name: "Principal" });
    expect(within(navegacion).getByRole("link", { name: "Sillones" })).toHaveAttribute("href", "/sillones");
    unmount();
    vi.unstubAllGlobals();
  }
});

it("Un 403 al escribir un sillón se muestra como permiso insuficiente", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/sillones") return Response.json([SILLON_1]);
    if (metodo === "POST" && url === "/api/sillones") {
      return Response.json({ detail: "detalle interno" }, { status: 403 });
    }
    return Response.json({});
  });

  await completarFormulario("Box 4", "Nuevo");
  await userEvent.click(screen.getByRole("button", { name: "Agregar sillón" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No tenés permiso para realizar esta acción.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(screen.getByLabelText("Nombre")).toHaveValue("Box 4");
  expect(screen.getByLabelText("Descripción")).toHaveValue("Nuevo");
  expect(screen.getByRole("navigation", { name: "Principal" })).toBeInTheDocument();
  expect(fetch.mock.calls.filter(([url]) => url === "/api/auth/me")).toHaveLength(1);
});
