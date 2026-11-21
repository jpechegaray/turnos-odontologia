import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "./clienteApi";
import { ErrorApi } from "./errorApi";

const fetchSimulado = vi.fn<Parameters<typeof fetch>, ReturnType<typeof fetch>>();

function json(cuerpo: unknown, estado = 200): Response {
  return new Response(JSON.stringify(cuerpo), { status: estado, headers: { "Content-Type": "application/json" } });
}

function ponerCookieCsrf(valor: string) {
  document.cookie = `XSRF-TOKEN=${valor}; path=/`;
}

function borrarCookieCsrf() {
  document.cookie = "XSRF-TOKEN=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
}

function cabeceras(llamada: number): Headers {
  return new Headers(fetchSimulado.mock.calls[llamada][1]?.headers);
}

beforeEach(() => {
  vi.stubGlobal("fetch", fetchSimulado);
  borrarCookieCsrf();
});

afterEach(() => {
  fetchSimulado.mockReset();
  vi.unstubAllGlobals();
  borrarCookieCsrf();
});

describe("Peticiones con la sesión del personal", () => {
  it("Una lectura usa la base de la API y envía la cookie de sesión", async () => {
    fetchSimulado.mockResolvedValueOnce(json([{ id: 1, nombre: "Consulta" }]));

    const respuesta = await api.get<{ id: number; nombre: string }[]>("/prestaciones");

    expect(respuesta).toEqual([{ id: 1, nombre: "Consulta" }]);
    const [url, opciones] = fetchSimulado.mock.calls[0];
    expect(url).toBe("/api/prestaciones");
    expect(opciones?.method).toBe("GET");
    expect(opciones?.credentials).toBe("same-origin");
    expect(cabeceras(0).get("Accept")).toBe("application/json");
  });

  it("Una respuesta 204 no devuelve cuerpo", async () => {
    ponerCookieCsrf("token-ficticio");
    fetchSimulado.mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(api.delete("/prestaciones/1")).resolves.toBeUndefined();
  });
});

describe("Token CSRF en las escrituras", () => {
  it("Una escritura envía la cabecera con el token vigente", async () => {
    ponerCookieCsrf("token-ficticio-1");
    fetchSimulado.mockResolvedValueOnce(json({ id: 7 }, 201));

    await api.post("/prestaciones", { nombre: "Limpieza", duracionDefaultMin: 45 });

    const [url, opciones] = fetchSimulado.mock.calls[0];
    expect(url).toBe("/api/prestaciones");
    expect(opciones?.method).toBe("POST");
    expect(cabeceras(0).get("X-XSRF-TOKEN")).toBe("token-ficticio-1");
    expect(cabeceras(0).get("Content-Type")).toBe("application/json");
    expect(opciones?.body).toBe(JSON.stringify({ nombre: "Limpieza", duracionDefaultMin: 45 }));
  });

  it("toma el token vigente aunque haya rotado entre dos escrituras", async () => {
    ponerCookieCsrf("antes-del-login");
    fetchSimulado.mockResolvedValue(new Response(null, { status: 204 }));
    await api.put("/prestaciones/1", { activa: true });
    ponerCookieCsrf("despues-del-login");
    await api.put("/prestaciones/1", { activa: true });

    expect(cabeceras(0).get("X-XSRF-TOKEN")).toBe("antes-del-login");
    expect(cabeceras(1).get("X-XSRF-TOKEN")).toBe("despues-del-login");
  });

  it("Sin cookie CSRF el cliente obtiene el token antes de escribir", async () => {
    fetchSimulado
      .mockImplementationOnce(async () => {
        ponerCookieCsrf("token-recibido");
        return new Response(null, { status: 204 });
      })
      .mockResolvedValueOnce(json({ id: 1 }, 201));

    await api.post("/prestaciones", { nombre: "Consulta", duracionDefaultMin: 30 });

    expect(fetchSimulado).toHaveBeenCalledTimes(2);
    const [urlCsrf, opcionesCsrf] = fetchSimulado.mock.calls[0];
    expect(urlCsrf).toBe("/api/auth/csrf");
    expect(opcionesCsrf?.method).toBe("GET");
    expect(opcionesCsrf?.credentials).toBe("same-origin");
    expect(fetchSimulado.mock.calls[1][0]).toBe("/api/prestaciones");
    expect(cabeceras(1).get("X-XSRF-TOKEN")).toBe("token-recibido");
  });

  it("dos escrituras simultáneas sin cookie piden el token una sola vez", async () => {
    fetchSimulado.mockImplementation(async (url) => {
      if (url === "/api/auth/csrf") {
        ponerCookieCsrf("token-compartido");
      }
      return new Response(null, { status: 204 });
    });

    await Promise.all([api.post("/a", {}), api.post("/b", {})]);

    expect(fetchSimulado.mock.calls.filter(([url]) => url === "/api/auth/csrf")).toHaveLength(1);
  });

  it("Una lectura no envía la cabecera CSRF", async () => {
    ponerCookieCsrf("token-ficticio");
    fetchSimulado.mockResolvedValueOnce(json([]));

    await api.get("/prestaciones");

    expect(cabeceras(0).has("X-XSRF-TOKEN")).toBe(false);
    expect(fetchSimulado).toHaveBeenCalledTimes(1);
  });
});

describe("Errores y fallo de red en el cliente", () => {
  it("rechaza con ErrorApi tipado ante una respuesta de error", async () => {
    fetchSimulado.mockResolvedValueOnce(
      new Response(JSON.stringify({ status: 404, detail: "No existe la prestación solicitada.", codigo: "PRESTACION-NO-ENCONTRADA" }), {
        status: 404,
        headers: { "Content-Type": "application/problem+json" },
      }),
    );

    const error = await api.get("/prestaciones/99").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ErrorApi);
    expect((error as ErrorApi).tipo).toBe("no-encontrado");
    expect((error as ErrorApi).codigo).toBe("PRESTACION-NO-ENCONTRADA");
  });

  it("rechaza con un error de red cuando fetch falla", async () => {
    fetchSimulado.mockRejectedValueOnce(new TypeError("Failed to fetch"));

    const error = await api.get("/prestaciones").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ErrorApi);
    expect((error as ErrorApi).tipo).toBe("red");
  });
});
