import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../../app/AppRutas";
import { construirRegistro } from "../../../app/rutas/registro";
import type { Rol } from "../../../app/rutas/tipos";
import { simularSesionActiva, type ResponderSimulado } from "../../../app/sesion.fixture";
import { rutas } from "../routes";
import type { ProfesionalVinculable, UsuarioRespuesta } from "./tipos";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const registro = construirRegistro({ auth: { rutas } });

function montar(rol: Rol, responder: ResponderSimulado, ruta = "/usuarios"): FetchSimulado {
  const { fetch } = simularSesionActiva(rol, responder);
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <AppRutas registro={registro} />
    </MemoryRouter>,
  );
  return fetch;
}

function consultasUsuarios(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(([url]) => String(url).startsWith("/api/usuarios"));
}

const PROF_GOMEZ: ProfesionalVinculable = { id: 10, nombre: "Ana", apellido: "Gómez", activo: true };
const PROF_RUIZ: ProfesionalVinculable = { id: 11, nombre: "Luis", apellido: "Ruiz", activo: true };
const PROF_INACTIVO: ProfesionalVinculable = { id: 12, nombre: "Eva", apellido: "Paz", activo: false };

const ODONTO: UsuarioRespuesta = {
  id: 2, email: "odonto1@consultorio.test", nombre: "Ana Gómez", rol: "ODONTOLOGO", profesionalId: 10, activo: true,
};
const RECEPCION: UsuarioRespuesta = {
  id: 3, email: "recepcion1@consultorio.test", nombre: "Rita Recepción", rol: "RECEPCION", profesionalId: null, activo: false,
};

function consultas(fetch: FetchSimulado, ruta: string) {
  return fetch.mock.calls.filter(
    ([url, init]) => url === ruta && ((init as RequestInit | undefined)?.method ?? "GET") === "GET",
  );
}

function escrituras(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(([, init]) => ((init as RequestInit | undefined)?.method ?? "GET") !== "GET");
}

/** Responde los dos listados; el resto se delega a `otras`. */
function responderCon(
  usuarios: () => readonly UsuarioRespuesta[],
  profesionales: readonly ProfesionalVinculable[] = [PROF_GOMEZ, PROF_RUIZ, PROF_INACTIVO],
  otras?: ResponderSimulado,
): ResponderSimulado {
  return (solicitud) => {
    if (solicitud.metodo === "GET" && solicitud.url === "/api/usuarios") return Response.json(usuarios());
    if (solicitud.metodo === "GET" && solicitud.url === "/api/profesionales") return Response.json(profesionales);
    return otras ? otras(solicitud) : Response.json({});
  };
}

const responderVacio: ResponderSimulado = ({ url, metodo }) => {
  if (metodo === "GET" && (url === "/api/usuarios" || url === "/api/profesionales")) return Response.json([]);
  return Response.json({});
};

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

it.each([
  ["ADMIN", true],
  ["RECEPCION", false],
  ["ODONTOLOGO", false],
] as const)("La sección de usuarios aparece en el menú solo para el administrador", async (rol, visible) => {
  montar(rol, responderVacio, "/");
  const navegacion = await screen.findByRole("navigation", { name: "Principal" });
  const enlace = within(navegacion).queryByRole("link", { name: "Usuarios" });
  if (visible) expect(enlace).toHaveAttribute("href", "/usuarios");
  else expect(enlace).not.toBeInTheDocument();
});

it.each(["RECEPCION", "ODONTOLOGO"] as const)(
  "Recepción o el odontólogo no acceden a usuarios por URL",
  async (rol) => {
    const fetch = montar(rol, responderVacio);
    expect(await screen.findByText("No tenés permiso")).toBeInTheDocument();
    expect(consultasUsuarios(fetch)).toHaveLength(0);
  },
);

it("El listado muestra los usuarios con su rol, vínculo y estado", async () => {
  const fetch = montar("ADMIN", responderCon(() => [ODONTO, RECEPCION]));

  const odonto = await screen.findByRole("row", { name: /odonto1@consultorio.test/ });
  expect(within(odonto).getByRole("cell", { name: "Ana Gómez" })).toBeInTheDocument();
  expect(within(odonto).getByRole("cell", { name: "Odontólogo" })).toBeInTheDocument();
  expect(within(odonto).getByRole("cell", { name: "Gómez, Ana" })).toBeInTheDocument();
  expect(within(odonto).getByRole("cell", { name: "Activo" })).toBeInTheDocument();
  const recepcion = screen.getByRole("row", { name: /recepcion1@consultorio.test/ });
  expect(within(recepcion).getByRole("cell", { name: "Recepción" })).toBeInTheDocument();
  expect(within(recepcion).getByRole("cell", { name: "—" })).toBeInTheDocument();
  expect(within(recepcion).getByRole("cell", { name: "Inactivo" })).toBeInTheDocument();
  const filas = screen.getAllByRole("row").slice(1);
  expect(filas.map((f) => within(f).getAllByRole("cell")[0]?.textContent)).toEqual([
    "odonto1@consultorio.test",
    "recepcion1@consultorio.test",
  ]);
  expect(consultas(fetch, "/api/usuarios").length).toBeGreaterThanOrEqual(1);
  expect(consultas(fetch, "/api/profesionales").length).toBeGreaterThanOrEqual(1);
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El listado vacío de usuarios muestra un mensaje", async () => {
  montar("ADMIN", responderCon(() => []));

  expect(await screen.findByText("No hay usuarios cargados.")).toBeInTheDocument();
  expect(screen.queryByRole("cell", { name: "odonto1@consultorio.test" })).not.toBeInTheDocument();
});

it("Un fallo al listar usuarios permite reintentar", async () => {
  let intentos = 0;
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/usuarios") {
      return ++intentos === 1 ? new Response(null, { status: 500 }) : Response.json([ODONTO]);
    }
    if (metodo === "GET" && url === "/api/profesionales") return Response.json([PROF_GOMEZ]);
    return Response.json({});
  });

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos cargar los usuarios.");
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(await screen.findByRole("cell", { name: "odonto1@consultorio.test" })).toBeInTheDocument();
  expect(consultas(fetch, "/api/usuarios")).toHaveLength(2);
  expect(consultas(fetch, "/api/profesionales")).toHaveLength(2);
});

type DatosFormulario = Partial<Readonly<{
  email: string;
  nombre: string;
  rol: string;
  password: string;
  profesional: string;
}>>;

/** Completa el formulario visible; los campos omitidos quedan como estaban. */
async function completarFormulario(datos: DatosFormulario) {
  if (datos.email !== undefined) {
    const campo = await screen.findByLabelText("Email");
    await userEvent.clear(campo);
    if (datos.email) await userEvent.type(campo, datos.email);
  }
  if (datos.nombre !== undefined) {
    const campo = await screen.findByLabelText("Nombre");
    await userEvent.clear(campo);
    if (datos.nombre) await userEvent.type(campo, datos.nombre);
  }
  if (datos.rol !== undefined) await userEvent.selectOptions(await screen.findByLabelText("Rol"), datos.rol);
  if (datos.password !== undefined) {
    const campo = await screen.findByLabelText("Contraseña inicial");
    await userEvent.clear(campo);
    if (datos.password) await userEvent.type(campo, datos.password);
  }
  if (datos.profesional !== undefined) {
    await userEvent.selectOptions(await screen.findByLabelText("Profesional"), datos.profesional);
  }
}

const CLAVE_FICTICIA = "clave-ficticia-1";

it("Un email inválido o un nombre en blanco bloquean el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", responderCon(() => []));

  await completarFormulario({ email: "sin-arroba", nombre: "   ", rol: "RECEPCION", password: CLAVE_FICTICIA });
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));

  expect(await screen.findByText("Ingresá un email válido.")).toBeInTheDocument();
  expect(screen.getByText("El nombre es obligatorio.")).toBeInTheDocument();
  expect(screen.getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
  expect(screen.getByLabelText("Nombre")).toHaveAttribute("aria-invalid", "true");
  expect(escrituras(fetch)).toHaveLength(0);
});

it.each([
  ["7 caracteres", "corta-7"],
  ["más de 72 bytes", "ñ".repeat(37)],
])("Una contraseña fuera de largo bloquea el alta sin llamar a la API", async (_caso, clave) => {
  const fetch = montar("ADMIN", responderCon(() => []));

  await completarFormulario({ email: "nuevo@consultorio.test", nombre: "Nuevo", rol: "RECEPCION", password: clave });
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));

  expect(await screen.findByText("La contraseña debe tener entre 8 caracteres y 72 bytes.")).toBeInTheDocument();
  expect(screen.getByLabelText("Contraseña inicial")).toHaveAttribute("aria-invalid", "true");
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Un odontólogo sin profesional bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("ADMIN", responderCon(() => []));

  await completarFormulario({
    email: "odonto9@consultorio.test", nombre: "Odonto", rol: "ODONTOLOGO", password: CLAVE_FICTICIA,
  });
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));

  expect(await screen.findByText("Elegí el profesional del odontólogo.")).toBeInTheDocument();
  expect(screen.getByLabelText("Profesional")).toHaveAttribute("aria-invalid", "true");
  expect(escrituras(fetch)).toHaveLength(0);
});

function opcionesDe(selector: HTMLElement): string[] {
  return within(selector).getAllByRole("option").map((opcion) => opcion.textContent ?? "");
}

it("El selector ofrece solo profesionales activos y libres", async () => {
  montar("ADMIN", responderCon(() => [ODONTO]));

  await screen.findByRole("cell", { name: "odonto1@consultorio.test" });
  await completarFormulario({ rol: "ODONTOLOGO" });

  expect(opcionesDe(screen.getByLabelText("Profesional"))).toEqual(["Elegí un profesional", "Ruiz, Luis"]);
});

function solicitudes(fetch: FetchSimulado, metodo: string, url: string) {
  return fetch.mock.calls
    .filter(([u, init]) => u === url && (init as RequestInit | undefined)?.method === metodo)
    .map(([, init]) => JSON.parse(String((init as RequestInit).body ?? "null")) as unknown);
}

it("El administrador da de alta un usuario de recepción y la lista se refresca", async () => {
  let lista: UsuarioRespuesta[] = [];
  const fetch = montar(
    "ADMIN",
    responderCon(() => lista, undefined, ({ url, metodo }) => {
      if (metodo === "POST" && url === "/api/usuarios") {
        const creado: UsuarioRespuesta = {
          id: 4, email: "recepcion2@consultorio.test", nombre: "Rosa", rol: "RECEPCION", profesionalId: null, activo: true,
        };
        lista = [creado];
        return Response.json(creado, { status: 201 });
      }
      return Response.json({});
    }),
  );

  await screen.findByText("No hay usuarios cargados.");
  await completarFormulario({
    email: " recepcion2@consultorio.test ", nombre: " Rosa ", rol: "RECEPCION", password: CLAVE_FICTICIA,
  });
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));

  expect(await screen.findByRole("cell", { name: "recepcion2@consultorio.test" })).toBeInTheDocument();
  expect(solicitudes(fetch, "POST", "/api/usuarios")).toEqual([
    { email: "recepcion2@consultorio.test", nombre: "Rosa", rol: "RECEPCION", password: CLAVE_FICTICIA, profesionalId: null },
  ]);
  expect(consultas(fetch, "/api/usuarios").length).toBeGreaterThanOrEqual(2);
  expect(screen.getByLabelText("Email")).toHaveValue("");
  expect(screen.getByLabelText("Nombre")).toHaveValue("");
  expect(screen.getByLabelText("Contraseña inicial")).toHaveValue("");
  expect(screen.getByRole("status")).toHaveTextContent("Usuario agregado.");
  expect(document.body).not.toHaveTextContent(CLAVE_FICTICIA);
});

/** Responde el `POST` con el usuario que pidió la SPA y lo suma a la lista. */
function responderAlta(lista: UsuarioRespuesta[]): ResponderSimulado {
  return ({ url, metodo, cuerpo }) => {
    if (metodo === "POST" && url === "/api/usuarios") {
      const alta = cuerpo as UsuarioRespuesta;
      const creado: UsuarioRespuesta = {
        id: 50 + lista.length, email: alta.email, nombre: alta.nombre, rol: alta.rol,
        profesionalId: alta.profesionalId, activo: true,
      };
      lista.push(creado);
      return Response.json(creado, { status: 201 });
    }
    return Response.json({});
  };
}

it("El administrador da de alta un odontólogo vinculado a su profesional", async () => {
  const lista: UsuarioRespuesta[] = [];
  const fetch = montar("ADMIN", responderCon(() => lista, undefined, responderAlta(lista)));

  await screen.findByText("No hay usuarios cargados.");
  await completarFormulario({
    email: "odonto2@consultorio.test", nombre: "Luis Ruiz", rol: "ODONTOLOGO", password: CLAVE_FICTICIA,
    profesional: "Ruiz, Luis",
  });
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));

  const fila = await screen.findByRole("row", { name: /odonto2@consultorio.test/ });
  expect(within(fila).getByRole("cell", { name: "Ruiz, Luis" })).toBeInTheDocument();
  expect(solicitudes(fetch, "POST", "/api/usuarios")).toEqual([
    expect.objectContaining({ rol: "ODONTOLOGO", profesionalId: 11 }),
  ]);
});

it("Cambiar a un rol que no es odontólogo descarta el vínculo", async () => {
  const lista: UsuarioRespuesta[] = [];
  const fetch = montar("ADMIN", responderCon(() => lista, undefined, responderAlta(lista)));

  await screen.findByText("No hay usuarios cargados.");
  await completarFormulario({
    email: "rol@consultorio.test", nombre: "Cambia de rol", rol: "ODONTOLOGO", password: CLAVE_FICTICIA,
    profesional: "Ruiz, Luis",
  });
  await completarFormulario({ rol: "RECEPCION" });

  expect(screen.queryByLabelText("Profesional")).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));
  await screen.findByRole("cell", { name: "rol@consultorio.test" });
  expect(solicitudes(fetch, "POST", "/api/usuarios")).toEqual([
    expect.objectContaining({ rol: "RECEPCION", profesionalId: null }),
  ]);
});

function problema(estado: number, codigo?: string, extra: Record<string, unknown> = {}): Response {
  return Response.json(
    { status: estado, detail: "detalle interno del servidor", ...(codigo ? { codigo } : {}), ...extra },
    { status: estado, headers: { "Content-Type": "application/problem+json" } },
  );
}

/** Responde con `respuesta` al `POST /api/usuarios`. */
function altaQueResponde(respuesta: () => Response): ResponderSimulado {
  return ({ url, metodo }) =>
    metodo === "POST" && url === "/api/usuarios" ? respuesta() : Response.json({});
}

it("Un email duplicado se muestra como alerta sin perder los datos", async () => {
  montar("ADMIN", responderCon(() => [], undefined, altaQueResponde(() => problema(409, "USUARIO_EMAIL_DUPLICADO"))));

  await screen.findByText("No hay usuarios cargados.");
  await completarFormulario({
    email: "odonto1@consultorio.test", nombre: "Repetido", rol: "ODONTOLOGO", password: CLAVE_FICTICIA,
    profesional: "Gómez, Ana",
  });
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("Ya hay un usuario con ese email.");
  expect(alerta).not.toHaveTextContent("detalle interno del servidor");
  expect(screen.getByLabelText("Email")).toHaveValue("odonto1@consultorio.test");
  expect(screen.getByLabelText("Nombre")).toHaveValue("Repetido");
  expect(screen.getByLabelText("Rol")).toHaveValue("ODONTOLOGO");
  expect(screen.getByLabelText("Profesional")).toHaveValue("10");
  expect(screen.getByLabelText("Contraseña inicial")).toHaveValue("");
});

const ALTA_ODONTOLOGO: DatosFormulario = {
  email: "odonto3@consultorio.test", nombre: "Odonto Tres", rol: "ODONTOLOGO", password: CLAVE_FICTICIA,
  profesional: "Ruiz, Luis",
};

it("Un profesional ya vinculado se muestra como alerta", async () => {
  const fetch = montar(
    "ADMIN",
    responderCon(() => [], undefined, altaQueResponde(() => problema(409, "USUARIO_PROFESIONAL_YA_VINCULADO"))),
  );

  await screen.findByText("No hay usuarios cargados.");
  const antes = consultas(fetch, "/api/profesionales").length;
  await completarFormulario(ALTA_ODONTOLOGO);
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("Ese profesional ya está vinculado a otro usuario.");
  expect(alerta).not.toHaveTextContent("detalle interno del servidor");
  await waitFor(() => expect(consultas(fetch, "/api/profesionales").length).toBe(antes + 1));
});

it("Un vínculo inválido se muestra como alerta", async () => {
  montar("ADMIN", responderCon(() => [], undefined, altaQueResponde(() => problema(422, "USUARIO_VINCULO_INVALIDO"))));

  await screen.findByText("No hay usuarios cargados.");
  await completarFormulario(ALTA_ODONTOLOGO);
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("El profesional elegido no es válido para ese rol.");
  expect(alerta).not.toHaveTextContent("detalle interno del servidor");
  expect(screen.getByLabelText("Email")).toHaveValue("odonto3@consultorio.test");
});

it("Una contraseña rechazada por la API marca el campo", async () => {
  const clave = "ficticia-rechazada";
  montar(
    "ADMIN",
    responderCon(() => [], undefined, altaQueResponde(() =>
      problema(400, undefined, { errores: [{ campo: "password", mensaje: `valor ${clave} inválido` }] }))),
  );

  await screen.findByText("No hay usuarios cargados.");
  await completarFormulario({ email: "nuevo@consultorio.test", nombre: "Nuevo", rol: "RECEPCION", password: clave });
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));

  expect(await screen.findByText("La contraseña no cumple el largo permitido.")).toBeInTheDocument();
  expect(screen.getByLabelText("Contraseña inicial")).toHaveAttribute("aria-invalid", "true");
  expect(document.body).not.toHaveTextContent(clave);
  expect(document.body).not.toHaveTextContent("detalle interno del servidor");
});

it("Un 403 al escribir un usuario se muestra como permiso insuficiente", async () => {
  const fetch = montar("ADMIN", responderCon(() => [], undefined, altaQueResponde(() => problema(403))));

  await screen.findByText("No hay usuarios cargados.");
  await completarFormulario({ email: "nuevo@consultorio.test", nombre: "Nuevo", rol: "RECEPCION", password: CLAVE_FICTICIA });
  await userEvent.click(screen.getByRole("button", { name: "Agregar usuario" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No tenés permiso para realizar esta acción.");
  expect(alerta).not.toHaveTextContent("detalle interno del servidor");
  expect(screen.getByLabelText("Email")).toHaveValue("nuevo@consultorio.test");
  expect(screen.getByRole("navigation", { name: "Principal" })).toBeInTheDocument();
  expect(fetch.mock.calls.filter(([url]) => url === "/api/auth/me")).toHaveLength(1);
});

const RECEPCION_ACTIVA: UsuarioRespuesta = { ...RECEPCION, activo: true };

it("El administrador cambia el rol de un usuario", async () => {
  let lista: UsuarioRespuesta[] = [RECEPCION_ACTIVA];
  const fetch = montar(
    "ADMIN",
    responderCon(() => lista, undefined, ({ url, metodo, cuerpo }) => {
      if (metodo === "PUT" && url === `/api/usuarios/${RECEPCION.id}`) {
        lista = [{ ...RECEPCION_ACTIVA, ...(cuerpo as Partial<UsuarioRespuesta>) }];
        return Response.json(lista[0]);
      }
      return Response.json({});
    }),
  );

  const fila = await screen.findByRole("row", { name: /recepcion1@consultorio.test/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));
  expect(screen.queryByLabelText("Contraseña inicial")).not.toBeInTheDocument();
  expect(screen.getByLabelText("Email")).toHaveValue("recepcion1@consultorio.test");
  await completarFormulario({ rol: "ADMIN" });
  await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

  const actualizada = await screen.findByRole("row", { name: /Administrador/ });
  expect(within(actualizada).getByRole("cell", { name: "recepcion1@consultorio.test" })).toBeInTheDocument();
  const [cuerpo] = solicitudes(fetch, "PUT", `/api/usuarios/${RECEPCION.id}`);
  expect(cuerpo).toEqual({
    email: "recepcion1@consultorio.test", nombre: "Rita Recepción", rol: "ADMIN", profesionalId: null, activo: true,
  });
  expect(cuerpo).not.toHaveProperty("password");
});

it("Editar un usuario inexistente muestra una alerta y refresca la lista", async () => {
  const fetch = montar(
    "ADMIN",
    responderCon(() => [RECEPCION_ACTIVA], undefined, ({ url, metodo }) =>
      metodo === "PUT" && url === `/api/usuarios/${RECEPCION.id}`
        ? problema(404, "USUARIO_NO_ENCONTRADO")
        : Response.json({})),
  );

  const fila = await screen.findByRole("row", { name: /recepcion1@consultorio.test/ });
  const antes = consultas(fetch, "/api/usuarios").length;
  await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));
  await completarFormulario({ nombre: "Otro nombre" });
  await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("El usuario ya no existe.");
  expect(alerta).not.toHaveTextContent("detalle interno del servidor");
  await waitFor(() => expect(consultas(fetch, "/api/usuarios").length).toBe(antes + 1));
});

/** Simula la API de escritura sobre una lista mutable: `DELETE` desactiva y `PUT` reemplaza. */
function responderEscrituras(lista: UsuarioRespuesta[]): ResponderSimulado {
  return ({ url, metodo, cuerpo }) => {
    const id = Number(url.split("/").at(-1));
    const indice = lista.findIndex((u) => u.id === id);
    const actual = lista[indice];
    if (actual && metodo === "DELETE") {
      lista[indice] = { ...actual, activo: false };
      return new Response(null, { status: 204 });
    }
    if (actual && metodo === "PUT") {
      lista[indice] = { ...actual, ...(cuerpo as Partial<UsuarioRespuesta>) };
      return Response.json(lista[indice]);
    }
    return Response.json({});
  };
}

it("El administrador desactiva un usuario y sigue visible como inactivo", async () => {
  const lista = [RECEPCION_ACTIVA];
  const fetch = montar("ADMIN", responderCon(() => lista, undefined, responderEscrituras(lista)));

  const fila = await screen.findByRole("row", { name: /recepcion1@consultorio.test/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Desactivar" }));
  await userEvent.click(within(fila).getByRole("button", { name: "Confirmar desactivación" }));

  const actualizada = await screen.findByRole("row", { name: /Inactivo/ });
  expect(within(actualizada).getByRole("cell", { name: "recepcion1@consultorio.test" })).toBeInTheDocument();
  expect(
    fetch.mock.calls.filter(
      ([url, init]) => url === `/api/usuarios/${RECEPCION.id}` && (init as RequestInit).method === "DELETE",
    ),
  ).toHaveLength(1);
});

it("Cancelar la confirmación de desactivación no emite escrituras", async () => {
  const lista = [RECEPCION_ACTIVA];
  const fetch = montar("ADMIN", responderCon(() => lista, undefined, responderEscrituras(lista)));

  const fila = await screen.findByRole("row", { name: /recepcion1@consultorio.test/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Desactivar" }));
  await userEvent.click(within(fila).getByRole("button", { name: "Cancelar" }));

  expect(within(fila).getByRole("button", { name: "Desactivar" })).toBeInTheDocument();
  expect(within(fila).getByRole("cell", { name: "Activo" })).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El administrador reactiva un usuario desactivado", async () => {
  const lista = [RECEPCION];
  const fetch = montar("ADMIN", responderCon(() => lista, undefined, responderEscrituras(lista)));

  const fila = await screen.findByRole("row", { name: /recepcion1@consultorio.test/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Reactivar" }));

  const actualizada = await screen.findByRole("row", { name: /Activo/ });
  expect(within(actualizada).getByRole("cell", { name: "recepcion1@consultorio.test" })).toBeInTheDocument();
  expect(solicitudes(fetch, "PUT", `/api/usuarios/${RECEPCION.id}`)).toEqual([
    { email: RECEPCION.email, nombre: RECEPCION.nombre, rol: "RECEPCION", profesionalId: null, activo: true },
  ]);
});

it.each([
  [
    "DELETE",
    async (fila: HTMLElement) => {
      await userEvent.click(within(fila).getByRole("button", { name: "Desactivar" }));
      await userEvent.click(within(fila).getByRole("button", { name: "Confirmar desactivación" }));
    },
  ],
  [
    "PUT",
    async (fila: HTMLElement) => {
      await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));
      await completarFormulario({ rol: "RECEPCION" });
      await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
    },
  ],
] as const)("No se puede desactivar al último administrador activo", async (metodoRechazado, intentar) => {
  const admin: UsuarioRespuesta = {
    id: 1, email: "admin@consultorio.test", nombre: "Admin", rol: "ADMIN", profesionalId: null, activo: true,
  };
  const fetch = montar(
    "ADMIN",
    responderCon(() => [admin], undefined, ({ metodo }) =>
      metodo === metodoRechazado ? problema(409, "USUARIO_ULTIMO_ADMIN") : Response.json({})),
  );

  const fila = await screen.findByRole("row", { name: /admin@consultorio.test/ });
  const antes = consultas(fetch, "/api/usuarios").length;
  await intentar(fila);

  if (metodoRechazado === "PUT") {
    expect(solicitudes(fetch, "PUT", `/api/usuarios/${admin.id}`)).toEqual([
      expect.objectContaining({ rol: "RECEPCION", activo: true }),
    ]);
  }
  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("Tiene que quedar al menos un administrador activo.");
  expect(alerta).not.toHaveTextContent("detalle interno del servidor");
  await waitFor(() => expect(consultas(fetch, "/api/usuarios").length).toBe(antes + 1));
  const recargada = await screen.findByRole("row", { name: /admin@consultorio.test/ });
  expect(within(recargada).getByRole("cell", { name: "Activo" })).toBeInTheDocument();
  expect(within(recargada).getByRole("cell", { name: "Administrador" })).toBeInTheDocument();
});
