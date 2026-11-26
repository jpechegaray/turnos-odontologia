import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../app/AppRutas";
import { construirRegistro } from "../../app/rutas/registro";
import type { Rol } from "../../app/rutas/tipos";
import { simularSesionActiva, type ResponderSimulado } from "../../app/sesion.fixture";
import {
  MENSAJE_CAMPO_INVALIDO,
  MENSAJE_DNI_DUPLICADO,
  MENSAJE_PACIENTE_NO_ENCONTRADO,
  MENSAJE_PERMISO,
} from "./errores";
import { rutas } from "./routes";
import type { PacienteRespuesta } from "./tipos";
import { MENSAJE_APELLIDO_OBLIGATORIO, MENSAJE_CONTACTO_OBLIGATORIO, MENSAJE_NOMBRE_OBLIGATORIO } from "./validacion";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const registro = construirRegistro({ pacientes: { rutas } });

function montar(rol: Rol, responder: ResponderSimulado): FetchSimulado {
  const { fetch } = simularSesionActiva(rol, responder);
  render(
    <MemoryRouter initialEntries={["/pacientes"]}>
      <AppRutas registro={registro} />
    </MemoryRouter>,
  );
  return fetch;
}

function escrituras(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(([, init]) => ((init as RequestInit | undefined)?.method ?? "GET") !== "GET");
}

function consultasPacientes(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(
    ([url, init]) =>
      String(url).startsWith("/api/pacientes") && ((init as RequestInit | undefined)?.method ?? "GET") === "GET",
  );
}

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

const responderListadoVacio: ResponderSimulado = ({ url, metodo }) => {
  if (metodo === "GET" && url.startsWith("/api/pacientes")) return Response.json([]);
  return Response.json({});
};

it("La pantalla de pacientes aparece en el menú para administración y recepción", async () => {
  for (const rol of ["ADMIN", "RECEPCION"] as const) {
    simularSesionActiva(rol, responderListadoVacio);
    const { unmount } = render(
      <MemoryRouter initialEntries={["/pacientes"]}>
        <AppRutas registro={registro} />
      </MemoryRouter>,
    );
    const navegacion = await screen.findByRole("navigation", { name: "Principal" });
    expect(within(navegacion).getByRole("link", { name: "Pacientes" })).toHaveAttribute("href", "/pacientes");
    unmount();
    vi.unstubAllGlobals();
  }
});

it("El odontólogo no ve la pantalla de pacientes", async () => {
  const fetch = montar("ODONTOLOGO", responderListadoVacio);

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No tenés permiso");
  const navegacion = screen.getByRole("navigation", { name: "Principal" });
  expect(within(navegacion).queryByRole("link", { name: "Pacientes" })).not.toBeInTheDocument();
  expect(consultasPacientes(fetch)).toHaveLength(0);
});

const ANA: PacienteRespuesta = {
  id: 1,
  nombre: "Ana",
  apellido: "Ficticia",
  dni: "30111222",
  email: "ana.ficticia@example.test",
  telefono: "1100000000",
  obraSocial: "Obra Social Ficticia",
  nroAfiliado: "A-100",
  notas: "Paciente de prueba",
  activo: true,
  creadoEn: "2026-01-10T12:00:00Z",
};

const BRUNO: PacienteRespuesta = {
  id: 2,
  nombre: "Bruno",
  apellido: "Inventado",
  dni: null,
  email: null,
  telefono: "1100000001",
  obraSocial: null,
  nroAfiliado: null,
  notas: null,
  activo: true,
  creadoEn: "2026-01-11T12:00:00Z",
};

it("Al abrir la pantalla se listan las fichas activas", async () => {
  const fetch = montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json([ANA, BRUNO]);
    return Response.json({});
  });

  const filaAna = await screen.findByRole("row", { name: /Ficticia/ });
  expect(screen.getAllByRole("row")).toHaveLength(3);
  expect(
    within(filaAna)
      .getAllByRole("cell")
      .slice(0, 4)
      .map((celda) => celda.textContent),
  ).toEqual(["Ficticia, Ana", "30111222", "1100000000ana.ficticia@example.test", "—"]);
  const filaBruno = screen.getByRole("row", { name: /Inventado/ });
  expect(within(filaBruno).getAllByRole("cell")[0]).toHaveTextContent("Bruno");
  expect(consultasPacientes(fetch).map(([url]) => url)).toEqual(["/api/pacientes"]);
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Un dato ausente de la ficha se muestra con una marca de vacío", async () => {
  montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json([BRUNO]);
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Inventado/ });
  const celdas = within(fila).getAllByRole("cell");
  expect(celdas[1]).toHaveTextContent("—");
  expect(celdas[2]).toHaveTextContent("—");
  expect(celdas[3]).toHaveTextContent("—");
  expect(celdas[2]).toHaveTextContent("1100000001");
});

it("Un fallo al consultar pacientes permite reintentar", async () => {
  let intentos = 0;
  const fetch = montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") {
      return ++intentos === 1 ? new Response(null, { status: 500 }) : Response.json([ANA]);
    }
    return Response.json({});
  });

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos cargar los pacientes.");
  expect(screen.queryByRole("table")).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(await screen.findByRole("cell", { name: /Ficticia/ })).toBeInTheDocument();
  expect(consultasPacientes(fetch)).toHaveLength(2);
});

async function buscar(filtros: Readonly<Partial<Record<"Apellido" | "Nombre" | "DNI" | "Email", string>>>) {
  await userEvent.click(await screen.findByText("Búsqueda avanzada"));
  const formulario = await screen.findByRole("search", { name: "Buscar pacientes" });
  for (const [etiqueta, valor] of Object.entries(filtros)) {
    await userEvent.type(within(formulario).getByLabelText(etiqueta), valor);
  }
  await userEvent.click(within(formulario).getByRole("button", { name: "Buscar" }));
}

it("La búsqueda por apellido envía solo ese filtro", async () => {
  const fetch = montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json([ANA, BRUNO]);
    if (metodo === "GET" && url === "/api/pacientes?apellido=G%C3%B3mez") {
      return Response.json([{ ...ANA, id: 3, apellido: "Gómez", nombre: "Gala" }]);
    }
    return Response.json({});
  });

  await buscar({ Apellido: "Gómez" });

  expect(await screen.findByRole("cell", { name: /Gómez/ })).toBeInTheDocument();
  expect(screen.queryByRole("cell", { name: /Inventado/ })).not.toBeInTheDocument();
  expect(consultasPacientes(fetch).map(([url]) => url)).toEqual(["/api/pacientes", "/api/pacientes?apellido=G%C3%B3mez"]);
});

it("Los filtros de búsqueda se combinan", async () => {
  const fetch = montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "GET" && url.startsWith("/api/pacientes")) return Response.json([ANA]);
    return Response.json({});
  });

  await buscar({ Nombre: "Ana", Email: "ana.ficticia@example.test" });

  await screen.findByRole("cell", { name: /Ficticia/ });
  const consultas = consultasPacientes(fetch).map(([url]) => String(url));
  expect(consultas).toHaveLength(2);
  const parametros = new URL(consultas[1] ?? "", "http://localhost").searchParams;
  expect(parametros.get("nombre")).toBe("Ana");
  expect(parametros.get("email")).toBe("ana.ficticia@example.test");
  expect(parametros.has("apellido")).toBe(false);
  expect(parametros.has("dni")).toBe(false);
});

it("La búsqueda por DNI envía el DNI recortado", async () => {
  const fetch = montar("RECEPCION", responderListadoVacio);

  await buscar({ DNI: " 30111222 " });

  await vi.waitFor(() => expect(consultasPacientes(fetch)).toHaveLength(2));
  expect(consultasPacientes(fetch)[1]?.[0]).toBe("/api/pacientes?dni=30111222");
});

it("Una búsqueda sin coincidencias muestra un mensaje", async () => {
  montar("RECEPCION", responderListadoVacio);

  await buscar({ Apellido: "Inexistente" });

  expect(await screen.findByText("No se encontraron pacientes.")).toBeInTheDocument();
  expect(screen.getAllByRole("row")).toHaveLength(2);
});

type EtiquetaFicha = "Nombre" | "Apellido" | "DNI" | "Email" | "Teléfono" | "Obra social" | "Número de afiliado" | "Notas";

async function formularioFicha() {
  if (!screen.queryByRole("form", { name: "Datos del paciente" })) {
    await userEvent.click(await screen.findByRole("button", { name: "Registrar paciente" }));
  }
  return screen.findByRole("form", { name: "Datos del paciente" });
}

async function completarFicha(valores: Readonly<Partial<Record<EtiquetaFicha, string>>>) {
  const formulario = await formularioFicha();
  for (const [etiqueta, valor] of Object.entries(valores)) {
    const campo = within(formulario).getByLabelText(etiqueta);
    await userEvent.clear(campo);
    if (valor) await userEvent.type(campo, valor);
  }
}

async function enviarFicha(etiqueta = "Agregar paciente") {
  const formulario = await formularioFicha();
  await userEvent.click(within(formulario).getByRole("button", { name: etiqueta }));
}

it("Una ficha sin nombre o apellido bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("RECEPCION", responderListadoVacio);

  await completarFicha({ Email: "ana.ficticia@example.test" });
  await enviarFicha();

  const formulario = await formularioFicha();
  expect(await within(formulario).findByText(MENSAJE_NOMBRE_OBLIGATORIO)).toBeInTheDocument();
  expect(within(formulario).getByText(MENSAJE_APELLIDO_OBLIGATORIO)).toBeInTheDocument();
  expect(within(formulario).getByLabelText("Nombre")).toHaveAttribute("aria-invalid", "true");
  expect(within(formulario).getByLabelText("Apellido")).toHaveAttribute("aria-invalid", "true");
  expect(within(formulario).getByLabelText("Email")).not.toHaveAttribute("aria-invalid");
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Una ficha sin email ni teléfono bloquea el envío sin llamar a la API", async () => {
  const fetch = montar("RECEPCION", responderListadoVacio);

  await completarFicha({ Nombre: "Ana", Apellido: "Ficticia", DNI: "30111222" });
  await enviarFicha();

  const formulario = await formularioFicha();
  expect(await within(formulario).findAllByText(MENSAJE_CONTACTO_OBLIGATORIO)).toHaveLength(2);
  expect(within(formulario).getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
  expect(within(formulario).getByLabelText("Teléfono")).toHaveAttribute("aria-invalid", "true");
  expect(within(formulario).getByLabelText("Nombre")).not.toHaveAttribute("aria-invalid");
  expect(within(formulario).queryByText(MENSAJE_NOMBRE_OBLIGATORIO)).not.toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

function cuerpoDe(escritura: readonly unknown[] | undefined): unknown {
  return JSON.parse(String((escritura?.[1] as RequestInit).body)) as unknown;
}

it("Recepción da de alta un paciente y la lista se refresca", async () => {
  let lista = [BRUNO];
  const fetch = montar("RECEPCION", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json(lista);
    if (metodo === "POST" && url === "/api/pacientes") {
      lista = [...lista, { ...ANA, ...(cuerpo as object) }];
      return Response.json(lista[1], { status: 201 });
    }
    return Response.json({});
  });

  await completarFicha({
    Nombre: " Ana ",
    Apellido: "Ficticia",
    DNI: "30111222",
    Email: "ana.ficticia@example.test",
    Teléfono: "1100000000",
    "Obra social": "Obra Social Ficticia",
    "Número de afiliado": "A-100",
    Notas: "Paciente de prueba",
  });
  await enviarFicha();

  expect(await screen.findByText("Paciente agregado.")).toBeInTheDocument();
  const posts = escrituras(fetch);
  expect(posts).toHaveLength(1);
  expect(posts[0]?.[0]).toBe("/api/pacientes");
  expect((posts[0]?.[1] as RequestInit).method).toBe("POST");
  expect(cuerpoDe(posts[0])).toEqual({
    nombre: "Ana",
    apellido: "Ficticia",
    dni: "30111222",
    email: "ana.ficticia@example.test",
    telefono: "1100000000",
    obraSocial: "Obra Social Ficticia",
    nroAfiliado: "A-100",
    notas: "Paciente de prueba",
  });
  expect(await screen.findByRole("cell", { name: /Ficticia/ })).toBeInTheDocument();
  expect(consultasPacientes(fetch)).toHaveLength(2);
  const formulario = await formularioFicha();
  for (const etiqueta of ["Nombre", "Apellido", "DNI", "Email", "Teléfono", "Obra social", "Número de afiliado", "Notas"]) {
    expect(within(formulario).getByLabelText(etiqueta)).toHaveValue("");
  }
});

it("Una ficha con solo teléfono se acepta", async () => {
  const fetch = montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "POST" && url === "/api/pacientes") return Response.json(BRUNO, { status: 201 });
    return Response.json([]);
  });

  await completarFicha({ Nombre: "Bruno", Apellido: "Inventado", Teléfono: "1100000001" });
  await enviarFicha();

  expect(await screen.findByText("Paciente agregado.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(1);
  expect(cuerpoDe(escrituras(fetch)[0])).toEqual({
    nombre: "Bruno",
    apellido: "Inventado",
    dni: null,
    email: null,
    telefono: "1100000001",
    obraSocial: null,
    nroAfiliado: null,
    notas: null,
  });
});

it("La obra social se envía como texto libre", async () => {
  const fetch = montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "POST" && url === "/api/pacientes") return Response.json(ANA, { status: 201 });
    return Response.json([]);
  });

  await completarFicha({
    Nombre: "Ana",
    Apellido: "Ficticia",
    Email: "ana.ficticia@example.test",
    "Obra social": "Obra Social Ficticia Plan 210",
  });
  await enviarFicha();

  expect(await screen.findByText("Paciente agregado.")).toBeInTheDocument();
  expect(cuerpoDe(escrituras(fetch)[0])).toMatchObject({ obraSocial: "Obra Social Ficticia Plan 210" });
});

function problema(estado: number, cuerpo: Readonly<Record<string, unknown>>): Response {
  return Response.json(
    { status: estado, ...cuerpo },
    { status: estado, headers: { "Content-Type": "application/problem+json" } },
  );
}

const FICHA_VALIDA = {
  Nombre: "Ana",
  Apellido: "Ficticia",
  DNI: "30111222",
  Email: "ana.ficticia@example.test",
} as const;

it("Un DNI duplicado se muestra en el campo DNI sin perder los datos", async () => {
  const fetch = montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "POST" && url === "/api/pacientes") {
      return problema(409, { codigo: "RN-32", detail: "Detalle interno: la ficha 77 ya tiene ese DNI" });
    }
    return Response.json([]);
  });

  await completarFicha(FICHA_VALIDA);
  await enviarFicha();

  const formulario = await formularioFicha();
  expect(await within(formulario).findByText(MENSAJE_DNI_DUPLICADO)).toBeInTheDocument();
  const dni = within(formulario).getByLabelText("DNI");
  expect(dni).toHaveAttribute("aria-invalid", "true");
  expect(dni).toHaveValue("30111222");
  expect(within(formulario).getByLabelText("Nombre")).toHaveValue("Ana");
  expect(within(formulario).getByLabelText("Apellido")).toHaveValue("Ficticia");
  expect(within(formulario).getByLabelText("Email")).toHaveValue("ana.ficticia@example.test");
  expect(screen.queryByText(/Detalle interno/)).not.toBeInTheDocument();
  expect(screen.queryByText("Paciente agregado.")).not.toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(1);
});

it("Un contacto faltante del backend se muestra en los campos de contacto", async () => {
  montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "POST" && url === "/api/pacientes") {
      return problema(422, { codigo: "RN-32", detail: "Detalle interno de contacto" });
    }
    return Response.json([]);
  });

  await completarFicha(FICHA_VALIDA);
  await enviarFicha();

  const formulario = await formularioFicha();
  expect(await within(formulario).findAllByText(MENSAJE_CONTACTO_OBLIGATORIO)).toHaveLength(2);
  expect(within(formulario).getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
  expect(within(formulario).getByLabelText("Teléfono")).toHaveAttribute("aria-invalid", "true");
  expect(within(formulario).getByLabelText("Nombre")).toHaveValue("Ana");
  expect(within(formulario).getByLabelText("Email")).toHaveValue("ana.ficticia@example.test");
  expect(screen.queryByText(/Detalle interno/)).not.toBeInTheDocument();
});

it("Un campo inválido del backend se muestra en su campo", async () => {
  montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "POST" && url === "/api/pacientes") {
      return problema(400, {
        detail: "Detalle interno de validación",
        errores: [{ campo: "notas", mensaje: "tamaño debe estar entre 0 y 1000" }],
      });
    }
    return Response.json([]);
  });

  await completarFicha({ ...FICHA_VALIDA, Notas: "Nota de prueba" });
  await enviarFicha();

  const formulario = await formularioFicha();
  expect(await within(formulario).findByText(MENSAJE_CAMPO_INVALIDO)).toBeInTheDocument();
  expect(within(formulario).getByLabelText("Notas")).toHaveAttribute("aria-invalid", "true");
  expect(within(formulario).getByLabelText("Notas")).toHaveValue("Nota de prueba");
  for (const etiqueta of ["Nombre", "Apellido", "DNI", "Email", "Teléfono", "Obra social", "Número de afiliado"]) {
    expect(within(formulario).getByLabelText(etiqueta)).not.toHaveAttribute("aria-invalid");
  }
  expect(screen.queryByText(/tamaño debe estar/)).not.toBeInTheDocument();
  expect(screen.queryByText(/Detalle interno/)).not.toBeInTheDocument();
});

it("Un rechazo por permiso se muestra como alerta", async () => {
  montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "POST" && url === "/api/pacientes") {
      return problema(403, { detail: "Detalle interno de permiso" });
    }
    return Response.json([]);
  });

  await completarFicha(FICHA_VALIDA);
  await enviarFicha();

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent(MENSAJE_PERMISO);
  expect(alerta).not.toHaveTextContent("Detalle interno");
  const formulario = await formularioFicha();
  expect(within(formulario).getByLabelText("Nombre")).toHaveValue("Ana");
  expect(within(formulario).getByLabelText("DNI")).toHaveValue("30111222");
});

it("Recepción edita una ficha y la lista se refresca", async () => {
  let ficha = ANA;
  const fetch = montar("RECEPCION", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url.startsWith("/api/pacientes")) return Response.json([ficha]);
    if (metodo === "PUT" && url === "/api/pacientes/1") {
      ficha = { ...ficha, ...(cuerpo as object) };
      return Response.json(ficha);
    }
    return Response.json({});
  });

  await buscar({ Apellido: "Ficticia" });
  const fila = await screen.findByRole("row", { name: /Ficticia/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));

  const formulario = await formularioFicha();
  expect(within(formulario).getByLabelText("Nombre")).toHaveValue("Ana");
  expect(within(formulario).getByLabelText("DNI")).toHaveValue("30111222");
  expect(within(formulario).getByLabelText("Notas")).toHaveValue("Paciente de prueba");
  expect(screen.getByRole("heading", { name: "Editar paciente" })).toBeInTheDocument();
  await completarFicha({ Teléfono: "1155550000" });
  await enviarFicha("Guardar cambios");

  expect(await screen.findByText("Paciente actualizado.")).toBeInTheDocument();
  const puts = escrituras(fetch);
  expect(puts).toHaveLength(1);
  expect(puts[0]?.[0]).toBe("/api/pacientes/1");
  expect((puts[0]?.[1] as RequestInit).method).toBe("PUT");
  expect(cuerpoDe(puts[0])).toEqual({
    nombre: "Ana",
    apellido: "Ficticia",
    dni: "30111222",
    email: "ana.ficticia@example.test",
    telefono: "1155550000",
    obraSocial: "Obra Social Ficticia",
    nroAfiliado: "A-100",
    notas: "Paciente de prueba",
  });
  expect(await screen.findByRole("cell", { name: /1155550000/ })).toBeInTheDocument();
  expect(consultasPacientes(fetch).map(([url]) => url)).toEqual([
    "/api/pacientes",
    "/api/pacientes?apellido=Ficticia",
    "/api/pacientes?apellido=Ficticia",
  ]);
  expect(screen.queryByRole("heading", { name: "Editar paciente" })).not.toBeInTheDocument();
});

it("Cancelar la edición vuelve al formulario de alta", async () => {
  const fetch = montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json([ANA]);
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Ficticia/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));
  const formulario = await formularioFicha();
  expect(within(formulario).getByLabelText("Nombre")).toHaveValue("Ana");
  await userEvent.click(within(formulario).getByRole("button", { name: "Cancelar" }));

  const vacio = await formularioFicha();
  expect(screen.getByRole("heading", { name: "Nuevo paciente" })).toBeInTheDocument();
  for (const etiqueta of ["Nombre", "Apellido", "DNI", "Email", "Teléfono", "Obra social", "Número de afiliado", "Notas"]) {
    expect(within(vacio).getByLabelText(etiqueta)).toHaveValue("");
  }
  expect(within(vacio).getByRole("button", { name: "Agregar paciente" })).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Editar una ficha inexistente muestra una alerta y refresca la lista", async () => {
  const fetch = montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json([ANA]);
    if (metodo === "PUT" && url === "/api/pacientes/1") {
      return problema(404, { codigo: "PACIENTE_NO_ENCONTRADO", detail: "Detalle interno de ficha" });
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Ficticia/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Editar" }));
  await enviarFicha("Guardar cambios");

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent(MENSAJE_PACIENTE_NO_ENCONTRADO);
  expect(alerta).not.toHaveTextContent("Detalle interno");
  await vi.waitFor(() => expect(consultasPacientes(fetch)).toHaveLength(2));
});

it("El administrador da de baja una ficha y deja de aparecer en la lista", async () => {
  let lista = [ANA, BRUNO];
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json(lista);
    if (metodo === "DELETE" && url === "/api/pacientes/1") {
      lista = lista.filter((p) => p.id !== 1);
      return new Response(null, { status: 204 });
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Ficticia/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Dar de baja" }));
  expect(escrituras(fetch)).toHaveLength(0);
  await userEvent.click(within(fila).getByRole("button", { name: "Confirmar baja" }));

  expect(await screen.findByText("Paciente dado de baja.")).toBeInTheDocument();
  const bajas = escrituras(fetch);
  expect(bajas).toHaveLength(1);
  expect(bajas[0]?.[0]).toBe("/api/pacientes/1");
  expect((bajas[0]?.[1] as RequestInit).method).toBe("DELETE");
  await vi.waitFor(() => expect(screen.queryByRole("cell", { name: /Ficticia/ })).not.toBeInTheDocument());
  expect(screen.getByRole("cell", { name: /Inventado/ })).toBeInTheDocument();
  expect(consultasPacientes(fetch)).toHaveLength(2);
});

it("Cancelar la confirmación no da de baja la ficha", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json([ANA]);
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Ficticia/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Dar de baja" }));
  expect(within(fila).queryByRole("button", { name: "Editar" })).not.toBeInTheDocument();
  await userEvent.click(within(fila).getByRole("button", { name: "Cancelar" }));

  expect(within(fila).queryByRole("button", { name: "Confirmar baja" })).not.toBeInTheDocument();
  expect(within(fila).getByRole("button", { name: "Editar" })).toBeInTheDocument();
  expect(within(fila).getByRole("button", { name: "Dar de baja" })).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Dar de baja una ficha inexistente muestra una alerta", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json([ANA]);
    if (metodo === "DELETE" && url === "/api/pacientes/1") {
      return problema(404, { codigo: "PACIENTE_NO_ENCONTRADO", detail: "Detalle interno de baja" });
    }
    return Response.json({});
  });

  const fila = await screen.findByRole("row", { name: /Ficticia/ });
  await userEvent.click(within(fila).getByRole("button", { name: "Dar de baja" }));
  await userEvent.click(within(fila).getByRole("button", { name: "Confirmar baja" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent(MENSAJE_PACIENTE_NO_ENCONTRADO);
  expect(alerta).not.toHaveTextContent("Detalle interno");
  expect(screen.queryByText("Paciente dado de baja.")).not.toBeInTheDocument();
  await vi.waitFor(() => expect(consultasPacientes(fetch)).toHaveLength(2));
});

it("El administrador ve los controles de alta, edición y baja", async () => {
  montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json([ANA, BRUNO]);
    return Response.json({});
  });

  await screen.findByRole("row", { name: /Ficticia/ });
  expect(within(await formularioFicha()).getByRole("button", { name: "Agregar paciente" })).toBeInTheDocument();
  expect(screen.getAllByRole("button", { name: "Editar" })).toHaveLength(2);
  expect(screen.getAllByRole("button", { name: "Dar de baja" })).toHaveLength(2);
});

it("Recepción ve alta y edición pero no la baja", async () => {
  montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/pacientes") return Response.json([ANA, BRUNO]);
    return Response.json({});
  });

  await screen.findByRole("row", { name: /Ficticia/ });
  expect(within(await formularioFicha()).getByRole("button", { name: "Agregar paciente" })).toBeInTheDocument();
  expect(screen.getAllByRole("button", { name: "Editar" })).toHaveLength(2);
  expect(screen.queryByRole("button", { name: "Dar de baja" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Confirmar baja" })).not.toBeInTheDocument();
});
