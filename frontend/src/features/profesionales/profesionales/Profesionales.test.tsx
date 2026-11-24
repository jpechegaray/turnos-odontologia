import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../../app/AppRutas";
import { construirRegistro } from "../../../app/rutas/registro";
import { simularSesionActiva, type ResponderSimulado } from "../../../app/sesion.fixture";
import type { Rol } from "../../../app/rutas/tipos";
import { rutas } from "./routes";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const registro = construirRegistro({ profesionales: { rutas } });

const PEREZ = { id: 1, nombre: "Ana", apellido: "Pérez", matricula: "MN-1001", especialidad: "Ortodoncia", activo: true };
const GOMEZ = { id: 2, nombre: "Luis", apellido: "Gómez", matricula: "MN-1002", especialidad: null, activo: false };

function montar(rol: Rol, responder: ResponderSimulado): FetchSimulado {
  const { fetch } = simularSesionActiva(rol, responder);
  render(
    <MemoryRouter initialEntries={["/profesionales"]}>
      <AppRutas registro={registro} />
    </MemoryRouter>,
  );
  return fetch;
}

function responderLista(lista: readonly unknown[]): ResponderSimulado {
  return ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/profesionales") return Response.json(lista);
    return Response.json({});
  };
}

function escrituras(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(([, init]) => ((init as RequestInit | undefined)?.method ?? "GET") !== "GET");
}

function consultas(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(
    ([url, init]) => url === "/api/profesionales" && ((init as RequestInit | undefined)?.method ?? "GET") === "GET",
  );
}

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

it("El listado muestra los profesionales con su matrícula y estado", async () => {
  const fetch = montar("ADMIN", responderLista([PEREZ, GOMEZ]));

  const filaPerez = await screen.findByRole("row", { name: /Pérez/ });
  expect(within(filaPerez).getByRole("cell", { name: "Pérez, Ana" })).toBeInTheDocument();
  expect(within(filaPerez).getByRole("cell", { name: "MN-1001" })).toBeInTheDocument();
  expect(within(filaPerez).getByRole("cell", { name: "Ortodoncia" })).toBeInTheDocument();
  expect(within(filaPerez).getByRole("cell", { name: "Activo" })).toBeInTheDocument();
  const filaGomez = screen.getByRole("row", { name: /Gómez/ });
  expect(within(filaGomez).getByRole("cell", { name: "Gómez, Luis" })).toBeInTheDocument();
  expect(within(filaGomez).getByRole("cell", { name: "MN-1002" })).toBeInTheDocument();
  expect(within(filaGomez).getByRole("cell", { name: "—" })).toBeInTheDocument();
  expect(within(filaGomez).getByRole("cell", { name: "Inactivo" })).toBeInTheDocument();
  const filas = screen.getAllByRole("row").slice(1);
  expect(filas[0]).toBe(filaPerez);
  expect(filas[1]).toBe(filaGomez);
  expect(consultas(fetch)).toHaveLength(1);
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El listado vacío de profesionales muestra un mensaje", async () => {
  montar("ADMIN", responderLista([]));

  expect(await screen.findByText("No hay profesionales cargados.")).toBeInTheDocument();
  expect(screen.queryByRole("cell", { name: /Pérez/ })).not.toBeInTheDocument();
});

it("Un fallo al listar profesionales permite reintentar", async () => {
  let intentos = 0;
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/profesionales") {
      return ++intentos === 1 ? new Response(null, { status: 500 }) : Response.json([PEREZ]);
    }
    return Response.json({});
  });

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos cargar los profesionales.");
  expect(screen.queryByRole("table")).not.toBeInTheDocument();
  await userEvent.click(within(alerta).getByRole("button", { name: "Reintentar" }));
  expect(await screen.findByRole("cell", { name: "Pérez, Ana" })).toBeInTheDocument();
  expect(consultas(fetch)).toHaveLength(2);
});

type DatosFormulario = Readonly<{ nombre?: string; apellido?: string; matricula?: string; especialidad?: string }>;

/** Reemplaza el contenido de los campos indicados; los omitidos quedan como están. */
async function completar(datos: DatosFormulario) {
  const campos: readonly [keyof DatosFormulario, string][] = [
    ["nombre", "Nombre"],
    ["apellido", "Apellido"],
    ["matricula", "Matrícula"],
    ["especialidad", "Especialidad"],
  ];
  for (const [clave, etiqueta] of campos) {
    const valor = datos[clave];
    if (valor === undefined) continue;
    const campo = await screen.findByLabelText(etiqueta);
    await userEvent.clear(campo);
    if (valor) await userEvent.type(campo, valor);
  }
}

it("Una matrícula en blanco bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", responderLista([]));

  await completar({ nombre: "Ana", apellido: "Pérez", matricula: "   " });
  await userEvent.click(screen.getByRole("button", { name: "Agregar profesional" }));

  expect(await screen.findByText("La matrícula es obligatoria.")).toBeInTheDocument();
  expect(screen.getByLabelText("Matrícula")).toHaveAttribute("aria-invalid", "true");
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Un nombre o apellido en blanco bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", responderLista([]));

  await completar({ nombre: "  ", apellido: "", matricula: "MN-2000" });
  await userEvent.click(screen.getByRole("button", { name: "Agregar profesional" }));

  expect(await screen.findByText("El nombre es obligatorio.")).toBeInTheDocument();
  expect(screen.getByText("El apellido es obligatorio.")).toBeInTheDocument();
  expect(screen.getByLabelText("Nombre")).toHaveAttribute("aria-invalid", "true");
  expect(screen.getByLabelText("Apellido")).toHaveAttribute("aria-invalid", "true");
  expect(screen.getByLabelText("Matrícula")).not.toHaveAttribute("aria-invalid");
  expect(escrituras(fetch)).toHaveLength(0);

  await userEvent.type(screen.getByLabelText("Nombre"), "Ana");
  expect(screen.queryByText("El nombre es obligatorio.")).not.toBeInTheDocument();
  expect(screen.getByText("El apellido es obligatorio.")).toBeInTheDocument();
});

it("Un texto de más de 255 caracteres bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", responderLista([]));

  await completar({ nombre: "Ana", apellido: "Pérez", matricula: "MN-2000" });
  // `type` con 256 caracteres es lento; se pega el texto de una vez.
  await userEvent.click(screen.getByLabelText("Especialidad"));
  await userEvent.paste("a".repeat(256));
  await userEvent.click(screen.getByRole("button", { name: "Agregar profesional" }));

  expect(await screen.findByText("Máximo 255 caracteres.")).toBeInTheDocument();
  expect(screen.getByLabelText("Especialidad")).toHaveAttribute("aria-invalid", "true");
  expect(escrituras(fetch)).toHaveLength(0);
});

type Cuerpo = Readonly<Record<string, unknown>>;

function cuerpoDe(llamada: Parameters<FetchSimulado> | undefined): Cuerpo {
  return JSON.parse(String((llamada?.[1] as RequestInit | undefined)?.body)) as Cuerpo;
}

function metodoDe(llamada: Parameters<FetchSimulado>): string {
  return (llamada[1] as RequestInit | undefined)?.method ?? "GET";
}

/** API en memoria: `GET` devuelve la lista vigente y `POST`/`PUT` la actualizan. */
function responderCatalogo(inicial: readonly (typeof PEREZ | typeof GOMEZ)[]): ResponderSimulado {
  let lista: Cuerpo[] = [...inicial];
  return ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url === "/api/profesionales") return Response.json(lista);
    if (metodo === "POST" && url === "/api/profesionales") {
      const nuevo = { ...(cuerpo as Cuerpo), id: 10, activo: true };
      lista = [...lista, nuevo];
      return Response.json(nuevo, { status: 201 });
    }
    const id = /^\/api\/profesionales\/(\d+)$/u.exec(url)?.[1];
    if (metodo === "PUT" && id) {
      lista = lista.map((p) => (String(p.id) === id ? { ...(cuerpo as Cuerpo), id: p.id } : p));
      return Response.json(lista.find((p) => String(p.id) === id));
    }
    return Response.json({});
  };
}

it("El administrador da de alta un profesional y la lista se refresca", async () => {
  const fetch = montar("ADMIN", responderCatalogo([PEREZ]));

  await completar({ nombre: "  Marta ", apellido: " Ruiz  ", matricula: " MN-3000 ", especialidad: "   " });
  await userEvent.click(screen.getByRole("button", { name: "Agregar profesional" }));

  expect(await screen.findByRole("cell", { name: "Ruiz, Marta" })).toBeInTheDocument();
  const post = escrituras(fetch).find((llamada) => metodoDe(llamada) === "POST");
  expect(post?.[0]).toBe("/api/profesionales");
  expect(cuerpoDe(post)).toEqual({ nombre: "Marta", apellido: "Ruiz", matricula: "MN-3000", especialidad: null });
  expect(consultas(fetch).length).toBeGreaterThanOrEqual(2);
  expect(screen.getByLabelText("Nombre")).toHaveValue("");
  expect(screen.getByLabelText("Apellido")).toHaveValue("");
  expect(screen.getByLabelText("Matrícula")).toHaveValue("");
  expect(screen.getByLabelText("Especialidad")).toHaveValue("");
  expect(screen.getByRole("status")).toHaveTextContent("Profesional agregado.");
});

it("Una matrícula duplicada se muestra como alerta sin perder los datos", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/profesionales") return Response.json([PEREZ]);
    if (metodo === "POST" && url === "/api/profesionales") {
      return Response.json({ codigo: "PROFESIONAL_MATRICULA_DUPLICADA", detail: "detalle interno" }, { status: 409 });
    }
    return Response.json({});
  });

  await completar({ nombre: "Marta", apellido: "Ruiz", matricula: "MN-1001", especialidad: "Endodoncia" });
  await userEvent.click(screen.getByRole("button", { name: "Agregar profesional" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("Ya existe un profesional con esa matrícula.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(screen.getByLabelText("Nombre")).toHaveValue("Marta");
  expect(screen.getByLabelText("Apellido")).toHaveValue("Ruiz");
  expect(screen.getByLabelText("Matrícula")).toHaveValue("MN-1001");
  expect(screen.getByLabelText("Especialidad")).toHaveValue("Endodoncia");
  expect(escrituras(fetch)).toHaveLength(1);
});

async function abrirEdicion(apellido: RegExp) {
  const fila = await screen.findByRole("row", { name: apellido });
  await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));
}

it("El administrador edita un profesional y la lista se refresca", async () => {
  const fetch = montar("ADMIN", responderCatalogo([PEREZ, GOMEZ]));

  await abrirEdicion(/Gómez/);
  expect(screen.getByRole("heading", { name: "Editar profesional" })).toBeInTheDocument();
  expect(screen.getByLabelText("Nombre")).toHaveValue("Luis");
  expect(screen.getByLabelText("Apellido")).toHaveValue("Gómez");
  expect(screen.getByLabelText("Matrícula")).toHaveValue("MN-1002");
  expect(screen.getByLabelText("Especialidad")).toHaveValue("");
  await completar({ especialidad: "Periodoncia" });
  await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

  const fila = await screen.findByRole("row", { name: /Periodoncia/ });
  expect(within(fila).getByRole("cell", { name: "Gómez, Luis" })).toBeInTheDocument();
  const put = escrituras(fetch).find((llamada) => metodoDe(llamada) === "PUT");
  expect(put?.[0]).toBe("/api/profesionales/2");
  expect(cuerpoDe(put)).toEqual({
    nombre: "Luis",
    apellido: "Gómez",
    matricula: "MN-1002",
    especialidad: "Periodoncia",
    activo: false,
  });
  expect(consultas(fetch).length).toBeGreaterThanOrEqual(2);
  expect(screen.getByRole("heading", { name: "Nuevo profesional" })).toBeInTheDocument();
});

it("Editar con una matrícula de otro profesional muestra una alerta", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/profesionales") return Response.json([PEREZ, GOMEZ]);
    if (metodo === "PUT" && url === "/api/profesionales/2") {
      return Response.json({ codigo: "PROFESIONAL_MATRICULA_DUPLICADA", detail: "detalle interno" }, { status: 409 });
    }
    return Response.json({});
  });

  await abrirEdicion(/Gómez/);
  await completar({ matricula: "MN-1001" });
  await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("Ya existe un profesional con esa matrícula.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(screen.getByRole("heading", { name: "Editar profesional" })).toBeInTheDocument();
  expect(screen.getByLabelText("Nombre")).toHaveValue("Luis");
  expect(screen.getByLabelText("Matrícula")).toHaveValue("MN-1001");
  expect(escrituras(fetch)).toHaveLength(1);
});

it("Editar un profesional inexistente muestra una alerta y refresca la lista", async () => {
  let lista: readonly unknown[] = [PEREZ, GOMEZ];
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/profesionales") return Response.json(lista);
    if (metodo === "PUT" && url === "/api/profesionales/2") {
      lista = [PEREZ];
      return Response.json({ codigo: "PROFESIONAL_NO_ENCONTRADO", detail: "detalle interno" }, { status: 404 });
    }
    return Response.json({});
  });

  await abrirEdicion(/Gómez/);
  await completar({ especialidad: "Periodoncia" });
  await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("El profesional ya no existe.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  await waitFor(() => expect(screen.queryByRole("row", { name: /Gómez/ })).not.toBeInTheDocument());
  expect(consultas(fetch)).toHaveLength(2);
});

it("El administrador da de baja un profesional y sigue visible como inactivo", async () => {
  const fetch = montar("ADMIN", responderCatalogo([PEREZ]));

  const fila = await screen.findByRole("row", { name: /Pérez/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Dar de baja" }));
  await userEvent.click(within(fila).getByRole("button", { name: "Confirmar baja" }));

  const filaActualizada = await screen.findByRole("row", { name: /Inactivo/ });
  expect(within(filaActualizada).getByRole("cell", { name: "Pérez, Ana" })).toBeInTheDocument();
  const put = escrituras(fetch).find((llamada) => metodoDe(llamada) === "PUT");
  expect(put?.[0]).toBe("/api/profesionales/1");
  expect(cuerpoDe(put)).toEqual({
    nombre: "Ana",
    apellido: "Pérez",
    matricula: "MN-1001",
    especialidad: "Ortodoncia",
    activo: false,
  });
  expect(escrituras(fetch).every((llamada) => metodoDe(llamada) !== "DELETE")).toBe(true);
  expect(consultas(fetch).length).toBeGreaterThanOrEqual(2);
});

it("Cancelar la confirmación de baja no emite escrituras", async () => {
  const fetch = montar("ADMIN", responderCatalogo([PEREZ]));

  const fila = await screen.findByRole("row", { name: /Pérez/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Dar de baja" }));
  await userEvent.click(within(fila).getByRole("button", { name: "Cancelar" }));

  expect(within(fila).getByRole("button", { name: "Editar" })).toBeInTheDocument();
  expect(within(fila).getByRole("button", { name: "Dar de baja" })).toBeInTheDocument();
  expect(within(fila).queryByRole("button", { name: "Confirmar baja" })).not.toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El administrador reactiva un profesional dado de baja", async () => {
  const fetch = montar("ADMIN", responderCatalogo([GOMEZ]));

  const fila = await screen.findByRole("row", { name: /Gómez/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Reactivar" }));

  expect(await screen.findByRole("cell", { name: "Activo" })).toBeInTheDocument();
  const put = escrituras(fetch).find((llamada) => metodoDe(llamada) === "PUT");
  expect(put?.[0]).toBe("/api/profesionales/2");
  expect(cuerpoDe(put)).toEqual({
    nombre: "Luis",
    apellido: "Gómez",
    matricula: "MN-1002",
    especialidad: null,
    activo: true,
  });
  expect(consultas(fetch).length).toBeGreaterThanOrEqual(2);
});

it("Un error al dar de baja un profesional se muestra como alerta", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/profesionales") return Response.json([PEREZ]);
    if (metodo === "PUT" && url === "/api/profesionales/1") {
      return Response.json({ codigo: "RN-37", detail: "detalle interno" }, { status: 422 });
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Pérez/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Dar de baja" }));
  await userEvent.click(within(fila).getByRole("button", { name: "Confirmar baja" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos completar la operación. Intentá de nuevo.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  await waitFor(() => expect(consultas(fetch)).toHaveLength(2));
  expect(await screen.findByRole("cell", { name: "Activo" })).toBeInTheDocument();
});

it("El administrador ve los controles de escritura de profesionales", async () => {
  montar("ADMIN", responderLista([PEREZ, GOMEZ]));

  expect(await screen.findByRole("button", { name: "Agregar profesional" })).toBeInTheDocument();
  expect(screen.getByRole("columnheader", { name: "Acciones" })).toBeInTheDocument();
  const filaPerez = screen.getByRole("row", { name: /Pérez/ });
  expect(within(filaPerez).getByRole("button", { name: "Editar" })).toBeInTheDocument();
  expect(within(filaPerez).getByRole("button", { name: "Dar de baja" })).toBeInTheDocument();
  const filaGomez = screen.getByRole("row", { name: /Gómez/ });
  expect(within(filaGomez).getByRole("button", { name: "Editar" })).toBeInTheDocument();
  expect(within(filaGomez).getByRole("button", { name: "Reactivar" })).toBeInTheDocument();
});

function sinControlesDeEscritura() {
  expect(screen.queryByRole("heading", { name: "Nuevo profesional" })).not.toBeInTheDocument();
  expect(screen.queryByLabelText("Matrícula")).not.toBeInTheDocument();
  expect(screen.queryByRole("columnheader", { name: "Acciones" })).not.toBeInTheDocument();
  for (const nombre of ["Agregar profesional", "Editar", "Dar de baja", "Reactivar"]) {
    expect(screen.queryByRole("button", { name: nombre })).not.toBeInTheDocument();
  }
}

it("Recepción consulta los profesionales sin controles de escritura", async () => {
  const fetch = montar("RECEPCION", responderLista([PEREZ, GOMEZ]));

  expect(await screen.findByRole("cell", { name: "Pérez, Ana" })).toBeInTheDocument();
  expect(screen.getByRole("cell", { name: "Gómez, Luis" })).toBeInTheDocument();
  sinControlesDeEscritura();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("La pantalla de profesionales aparece en el menú para los tres roles", async () => {
  for (const rol of ["ADMIN", "RECEPCION", "ODONTOLOGO"] as const) {
    simularSesionActiva(rol, responderLista([PEREZ]));
    const montado = render(
      <MemoryRouter initialEntries={["/profesionales"]}>
        <AppRutas registro={registro} />
      </MemoryRouter>,
    );
    const navegacion = await screen.findByRole("navigation", { name: "Principal" });
    expect(within(navegacion).getByRole("link", { name: "Profesionales" })).toHaveAttribute("href", "/profesionales");
    montado.unmount();
    vi.unstubAllGlobals();
  }
});

it("Un 403 al escribir un profesional se muestra como permiso insuficiente", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/profesionales") return Response.json([PEREZ]);
    if (metodo === "POST" && url === "/api/profesionales") {
      return Response.json({ detail: "detalle interno" }, { status: 403 });
    }
    return Response.json({});
  });

  await completar({ nombre: "Marta", apellido: "Ruiz", matricula: "MN-3000" });
  await userEvent.click(screen.getByRole("button", { name: "Agregar profesional" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No tenés permiso para realizar esta acción.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(screen.getByLabelText("Nombre")).toHaveValue("Marta");
  expect(screen.getByLabelText("Matrícula")).toHaveValue("MN-3000");
  expect(screen.getByRole("navigation", { name: "Principal" })).toBeInTheDocument();
  expect(screen.getByRole("cell", { name: "Pérez, Ana" })).toBeInTheDocument();
  expect(fetch.mock.calls.filter(([url]) => String(url).includes("/api/auth/logout"))).toHaveLength(0);
});

it("El odontólogo vinculado ve solo su registro sin controles de escritura", async () => {
  const fetch = montar("ODONTOLOGO", responderLista([PEREZ]));

  expect(await screen.findByRole("cell", { name: "Pérez, Ana" })).toBeInTheDocument();
  expect(screen.getAllByRole("row")).toHaveLength(2);
  sinControlesDeEscritura();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El odontólogo sin vínculo ve un aviso de falta de vínculo", async () => {
  const fetch = montar("ODONTOLOGO", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/profesionales") {
      return Response.json({ detail: "detalle interno" }, { status: 403 });
    }
    return Response.json({});
  });

  expect(
    await screen.findByText("Tu usuario no tiene un profesional vinculado. Pedile al administrador que lo vincule."),
  ).toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(screen.queryByRole("table")).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Reintentar" })).not.toBeInTheDocument();
  expect(screen.queryByText("detalle interno")).not.toBeInTheDocument();
  expect(screen.getByRole("navigation", { name: "Principal" })).toBeInTheDocument();
  sinControlesDeEscritura();
  expect(escrituras(fetch)).toHaveLength(0);
});
