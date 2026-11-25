import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AppRutas } from "../../app/AppRutas";
import { construirRegistro } from "../../app/rutas/registro";
import { registroDeFeatures } from "../../app/rutas/modulos";
import { simularSesionActiva, type ResponderSimulado } from "../../app/sesion.fixture";
import type { Rol } from "../../app/rutas/tipos";
import { PaginaConfiguracion } from "./PaginaConfiguracion";

type FetchSimulado = ReturnType<typeof simularSesionActiva>["fetch"];

const CONFIGURACION = {
  "cancelacion.plazo_horas": 24,
  "recordatorio.anticipacion_horas": 24,
  "reserva_online.horizonte_dias": 60,
  "reserva_online.anticipacion_minima_horas": 2,
  "reserva_online.max_turnos_activos_por_email": 3,
  "lista_espera.max_avisos_por_hueco": 10,
  "consultorio.nombre": "Consultorio Ficticio",
  "consultorio.domicilio": "Av. Ficticia 1234, Ciudad Autónoma de Buenos Aires",
  "consultorio.email_contacto": "contacto@consultorio-ficticio.example",
  "consultorio.retencion_anios": 2,
} as const;

const CAMPOS: ReadonlyArray<readonly [string, string]> = [
  ["Plazo de cancelación (horas)", "24"],
  ["Anticipación del recordatorio (horas)", "24"],
  ["Horizonte de reserva online (días)", "60"],
  ["Anticipación mínima de reserva online (horas)", "2"],
  ["Máximo de turnos activos por email", "3"],
  ["Máximo de avisos por hueco en lista de espera", "10"],
  ["Nombre del consultorio", "Consultorio Ficticio"],
  ["Domicilio del consultorio", "Av. Ficticia 1234, Ciudad Autónoma de Buenos Aires"],
  ["Email de contacto del consultorio", "contacto@consultorio-ficticio.example"],
  ["Retención de datos (años)", "2"],
];

const registro = construirRegistro({
  configuracion: { rutas: [{ ruta: "/configuracion", componente: PaginaConfiguracion }] },
});

function montar(rol: Rol, responder: ResponderSimulado, registroUsado = registro): FetchSimulado {
  const { fetch } = simularSesionActiva(rol, responder);
  render(
    <MemoryRouter initialEntries={["/configuracion"]}>
      <AppRutas registro={registroUsado} />
    </MemoryRouter>,
  );
  return fetch;
}

function escrituras(fetch: FetchSimulado) {
  return fetch.mock.calls.filter(([, init]) => ((init as RequestInit | undefined)?.method ?? "GET") !== "GET");
}

async function escribir(etiqueta: string, texto: string) {
  const campo = await screen.findByLabelText(etiqueta);
  await userEvent.clear(campo);
  if (texto) await userEvent.type(campo, texto);
}

beforeEach(() => {
  document.cookie = "XSRF-TOKEN=ficticio; Path=/";
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});

it("La configuración se precarga con las siete claves vigentes", async () => {
  montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  });

  expect(await screen.findByLabelText(CAMPOS[0][0])).toBeInTheDocument();
  for (const [etiqueta, valor] of CAMPOS) {
    expect(screen.getByLabelText(etiqueta)).toHaveValue(valor);
  }
});

it("Un fallo al consultar la configuración permite reintentar", async () => {
  let intentos = 0;
  montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") {
      return ++intentos === 1 ? new Response(null, { status: 500 }) : Response.json(CONFIGURACION);
    }
    return Response.json({});
  });

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos cargar la configuración.");
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(await screen.findByLabelText(CAMPOS[0][0])).toHaveValue("24");
});

it("La pantalla no envía escrituras al cargar", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  });

  await screen.findByLabelText(CAMPOS[0][0]);
  expect(fetch.mock.calls.some(([url, init]) => url === "/api/configuracion" && ((init as RequestInit | undefined)?.method ?? "GET") === "GET")).toBe(true);
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Un plazo de cancelación fuera de rango bloquea el guardado sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  });

  await escribir("Plazo de cancelación (horas)", "169");
  await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));

  expect(await screen.findByText("Hay valores fuera de los límites permitidos.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Una anticipación de recordatorio fuera de rango bloquea el guardado sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  });

  await escribir("Anticipación del recordatorio (horas)", "0");
  await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));

  expect(await screen.findByText("Hay valores fuera de los límites permitidos.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Un límite positivo en cero bloquea el guardado sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  });

  await escribir("Horizonte de reserva online (días)", "0");
  await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));

  expect(await screen.findByText("Hay valores fuera de los límites permitidos.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Un valor no entero bloquea el guardado sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  });

  await escribir("Anticipación mínima de reserva online (horas)", "2,5");
  await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));

  expect(await screen.findByText("Hay valores fuera de los límites permitidos.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Un nombre de consultorio en blanco bloquea el guardado sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  });

  await escribir("Nombre del consultorio", "");
  await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));

  expect(await screen.findByText("Hay valores fuera de los límites permitidos.")).toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Un domicilio o un email de contacto en blanco bloquea el guardado sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  });

  await escribir("Domicilio del consultorio", "");
  await escribir("Email de contacto del consultorio", "");
  await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));

  expect(await screen.findAllByText("Hay valores fuera de los límites permitidos.")).toHaveLength(2);
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Una retención fuera de rango bloquea el guardado sin llamar a la API", async () => {
  const fetch = montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  });

  for (const valor of ["0", "21"]) {
    await escribir("Retención de datos (años)", valor);
    await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));
    expect(await screen.findByText("Hay valores fuera de los límites permitidos.")).toBeInTheDocument();
  }
  expect(escrituras(fetch)).toHaveLength(0);
});

it("Los valores límite permitidos se aceptan", async () => {
  const fetch = montar("ADMIN", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    if (metodo === "PUT" && url === "/api/configuracion") return Response.json(cuerpo);
    return Response.json({});
  });

  await escribir("Plazo de cancelación (horas)", "0");
  await escribir("Anticipación del recordatorio (horas)", "168");
  await escribir("Retención de datos (años)", "20");
  await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));

  const put = await waitFor(() => {
    const encontrado = escrituras(fetch).find(([, init]) => (init as RequestInit).method === "PUT");
    expect(encontrado).toBeDefined();
    return encontrado;
  });
  expect(put?.[0]).toBe("/api/configuracion");
  expect(JSON.parse(String((put?.[1] as RequestInit).body))).toEqual({
    ...CONFIGURACION,
    "cancelacion.plazo_horas": 0,
    "recordatorio.anticipacion_horas": 168,
    "consultorio.retencion_anios": 20,
  });
});

it("El administrador guarda las siete claves y ve la confirmación", async () => {
  const fetch = montar("ADMIN", ({ url, metodo, cuerpo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    if (metodo === "PUT" && url === "/api/configuracion") return Response.json(cuerpo);
    return Response.json({});
  });

  await escribir("Plazo de cancelación (horas)", "48");
  await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));

  expect(await screen.findByText("Configuración guardada.")).toBeInTheDocument();
  const put = escrituras(fetch).find(([, init]) => (init as RequestInit).method === "PUT");
  expect(put?.[0]).toBe("/api/configuracion");
  const cuerpo = JSON.parse(String((put?.[1] as RequestInit).body)) as Record<string, unknown>;
  expect(Object.keys(cuerpo).sort()).toEqual(Object.keys(CONFIGURACION).sort());
  expect(cuerpo).toEqual({ ...CONFIGURACION, "cancelacion.plazo_horas": 48 });
});

it("Un 422 RN-36 del backend se muestra como alerta sin perder los valores ingresados", async () => {
  montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    if (metodo === "PUT" && url === "/api/configuracion") {
      return Response.json({ codigo: "RN-36", detail: "detalle interno" }, { status: 422 });
    }
    return Response.json({});
  });

  await escribir("Plazo de cancelación (horas)", "36");
  await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("Hay valores fuera de los límites permitidos.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(screen.getByLabelText("Plazo de cancelación (horas)")).toHaveValue("36");
});

it("Un 400 del backend se muestra como error de operación", async () => {
  montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    if (metodo === "PUT" && url === "/api/configuracion") {
      return Response.json({ detail: "detalle interno" }, { status: 400 });
    }
    return Response.json({});
  });

  await escribir("Plazo de cancelación (horas)", "36");
  await userEvent.click(screen.getByRole("button", { name: "Guardar configuración" }));

  const alerta = await screen.findByRole("alert");
  expect(alerta).toHaveTextContent("No pudimos guardar la configuración. Revisá los datos e intentá de nuevo.");
  expect(alerta).not.toHaveTextContent("Hay valores fuera de los límites permitidos.");
  expect(alerta).not.toHaveTextContent("detalle interno");
  expect(screen.getByLabelText("Plazo de cancelación (horas)")).toHaveValue("36");
});

it("El administrador puede editar y guardar", async () => {
  montar("ADMIN", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  }, registroDeFeatures);

  const campo = await screen.findByLabelText("Plazo de cancelación (horas)");
  expect(campo).toBeEnabled();
  expect(campo).not.toHaveAttribute("readonly");
  expect(screen.getByRole("button", { name: "Guardar configuración" })).toBeVisible();
});

it("Recepción consulta la configuración en solo lectura y no envía PUT", async () => {
  const fetch = montar("RECEPCION", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  }, registroDeFeatures);

  expect(await screen.findByLabelText(CAMPOS[0][0])).toBeInTheDocument();
  for (const [etiqueta] of CAMPOS) {
    expect(screen.getByLabelText(etiqueta)).toHaveAttribute("readonly");
  }
  expect(screen.queryByRole("button", { name: "Guardar configuración" })).not.toBeInTheDocument();
  expect(escrituras(fetch)).toHaveLength(0);
});

it("El odontólogo no puede abrir la configuración", async () => {
  const fetch = montar("ODONTOLOGO", ({ url, metodo }) => {
    if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
    return Response.json({});
  }, registroDeFeatures);

  expect(await screen.findByText("No tenés permiso")).toBeInTheDocument();
  expect(fetch.mock.calls.some(([url]) => url === "/api/configuracion")).toBe(false);
  expect(screen.queryByLabelText(CAMPOS[0][0])).not.toBeInTheDocument();
});

it("La pantalla de configuración aparece en el menú solo para ADMIN y RECEPCION", async () => {
  for (const rol of ["ADMIN", "RECEPCION", "ODONTOLOGO"] as const) {
    simularSesionActiva(rol, ({ url, metodo }) => {
      if (metodo === "GET" && url === "/api/configuracion") return Response.json(CONFIGURACION);
      return Response.json({});
    });
    const { unmount } = render(
      <MemoryRouter initialEntries={["/configuracion"]}>
        <AppRutas registro={registroDeFeatures} />
      </MemoryRouter>,
    );
    const navegacion = await screen.findByRole("navigation", { name: "Principal" });
    const enlace = within(navegacion).queryByRole("link", { name: "Configuración" });
    if (rol === "ODONTOLOGO") {
      expect(enlace).not.toBeInTheDocument();
    } else {
      expect(enlace).toHaveAttribute("href", "/configuracion");
    }
    unmount();
    vi.unstubAllGlobals();
  }
});
