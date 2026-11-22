import { obtenerApiBaseUrl } from "./apiBaseUrl";
import { asegurarTokenCsrf, CABECERA_CSRF, requiereCsrf } from "./csrf";
import { errorDeRed, errorDesdeRespuesta } from "./errorApi";

export type MetodoHttp = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

/**
 * Única forma de llamar a la API desde la SPA. Usa el mismo origen (proxy de Vite) para que viaje la
 * cookie de sesión `HttpOnly` (DD-03), agrega el token CSRF en las escrituras y rechaza con `ErrorApi`.
 *
 * @param ruta ruta relativa a la base de la API, por ejemplo `/prestaciones`
 */
export async function peticion<T>(metodo: MetodoHttp, ruta: string, cuerpo?: unknown): Promise<T> {
  const cabeceras = new Headers({ Accept: "application/json" });
  if (cuerpo !== undefined) {
    cabeceras.set("Content-Type", "application/json");
  }

  let respuesta: Response;
  try {
    if (requiereCsrf(metodo)) {
      const token = await asegurarTokenCsrf();
      if (token) cabeceras.set(CABECERA_CSRF, token);
    }
    respuesta = await fetch(`${obtenerApiBaseUrl()}${ruta}`, {
      method: metodo,
      credentials: "same-origin",
      headers: cabeceras,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    });
  } catch (causa) {
    throw errorDeRed(causa);
  }

  if (!respuesta.ok) {
    throw await errorDesdeRespuesta(respuesta);
  }
  if (respuesta.status === 204 || respuesta.headers.get("Content-Length") === "0") {
    return undefined as T;
  }
  return (await respuesta.json()) as T;
}

export const api = {
  get: <T>(ruta: string) => peticion<T>("GET", ruta),
  post: <T>(ruta: string, cuerpo?: unknown) => peticion<T>("POST", ruta, cuerpo),
  put: <T>(ruta: string, cuerpo?: unknown) => peticion<T>("PUT", ruta, cuerpo),
  patch: <T>(ruta: string, cuerpo?: unknown) => peticion<T>("PATCH", ruta, cuerpo),
  delete: <T = void>(ruta: string) => peticion<T>("DELETE", ruta),
};
