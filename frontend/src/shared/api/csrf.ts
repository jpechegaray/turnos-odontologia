import { obtenerApiBaseUrl } from "./apiBaseUrl";

/** Contrato de C-03: cookie legible por la SPA y cabecera que el backend compara con ella. */
export const COOKIE_CSRF = "XSRF-TOKEN";
export const CABECERA_CSRF = "X-XSRF-TOKEN";

const METODOS_SEGUROS = new Set(["GET", "HEAD", "OPTIONS"]);

export function requiereCsrf(metodo: string): boolean {
  return !METODOS_SEGUROS.has(metodo.toUpperCase());
}

/** Se lee en cada escritura: el backend rota el token al iniciar y cerrar sesión. */
export function leerTokenCsrf(): string | undefined {
  for (const par of document.cookie.split(";")) {
    const [nombre, ...resto] = par.trim().split("=");
    if (nombre === COOKIE_CSRF) {
      const valor = resto.join("=");
      return valor ? decodeURIComponent(valor) : undefined;
    }
  }
  return undefined;
}

let pedidoEnCurso: Promise<void> | null = null;

/**
 * Devuelve el token vigente; si no hay cookie, la pide una sola vez a `GET /auth/csrf` aunque haya
 * varias escrituras esperando. Si el backend no la entrega, la escritura sale sin cabecera y el
 * backend la rechazará con 401/403, que el cliente tipa como cualquier otro error.
 */
export async function asegurarTokenCsrf(): Promise<string | undefined> {
  const vigente = leerTokenCsrf();
  if (vigente) return vigente;
  pedidoEnCurso ??= fetch(`${obtenerApiBaseUrl()}/auth/csrf`, { method: "GET", credentials: "same-origin" })
    .then(() => undefined)
    .finally(() => {
      pedidoEnCurso = null;
    });
  await pedidoEnCurso;
  return leerTokenCsrf();
}
