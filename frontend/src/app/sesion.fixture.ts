import { vi } from "vitest";
import type { Rol } from "./rutas/tipos";

export type SolicitudSimulada = Readonly<{
  url: string;
  metodo: string;
  cuerpo?: unknown;
  cabeceras: Headers;
}>;

export type ResponderSimulado = (solicitud: SolicitudSimulada) => Response | Promise<Response>;

function urlDe(entrada: RequestInfo | URL): string {
  if (typeof entrada === "string") return entrada;
  if (entrada instanceof URL) return entrada.href;
  return entrada.url;
}

/**
 * Simula una sesión activa: `/api/auth/me` responde la identidad con el rol indicado (por defecto
 * `ADMIN`) y el resto de las rutas se delegan al `responder` opcional. Conserva la firma sin
 * argumentos para los tests existentes.
 */
export function simularSesionActiva(rol: Rol = "ADMIN", responder?: ResponderSimulado) {
  const fetch = vi.fn(async (entrada: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    const url = urlDe(entrada);
    if (url === "/api/auth/me" || url.endsWith("/api/auth/me")) {
      return Response.json({ id: 1, email: "personal@example.test", rol });
    }
    if (responder) {
      return responder({
        url,
        metodo: init?.method ?? "GET",
        cuerpo: typeof init?.body === "string" ? (JSON.parse(init.body) as unknown) : undefined,
        cabeceras: new Headers(init?.headers),
      });
    }
    return Response.json({});
  });
  vi.stubGlobal("fetch", fetch);
  // Se devuelve un objeto (no la función) para que `beforeEach(simularSesionActiva)` no la confunda
  // con una función de limpieza.
  return { fetch };
}
