import { api } from "../../../shared/api/clienteApi";
import { ErrorApi } from "../../../shared/api/errorApi";
import { esIntervalo, esObjeto, esTexto, respuestaInvalida } from "../validacionPublica";

export type EstadoOferta = "DISPONIBLE" | "TOMADA" | "VENCIDA" | "NO_DISPONIBLE";

export type OfertaPublica = Readonly<{
  inicio: string;
  fin: string;
  profesional: string;
  prestacion: string;
  estado: EstadoOferta;
}>;

export type TurnoTomado = Readonly<{
  inicio: string;
  fin: string;
  profesional: string;
  prestacion: string;
  token: string;
}>;

const ESTADOS: readonly EstadoOferta[] = ["DISPONIBLE", "TOMADA", "VENCIDA", "NO_DISPONIBLE"];

/** La prestación puede faltar si se dio de baja (C-42 la devuelve `null`): se muestra vacía. */
function leerOferta(valor: unknown): OfertaPublica {
  if (!esObjeto(valor) || !esIntervalo(valor) || !esTexto(valor.profesional)
    || !(esTexto(valor.prestacion) || valor.prestacion === null)
    || !ESTADOS.includes(valor.estado as EstadoOferta)) {
    throw respuestaInvalida();
  }
  return { ...(valor as OfertaPublica), prestacion: valor.prestacion ?? "" };
}

function leerTurnoTomado(valor: unknown): TurnoTomado {
  if (!esObjeto(valor) || !esIntervalo(valor) || !esTexto(valor.profesional) || !esTexto(valor.prestacion)
    || !esTexto(valor.token) || valor.token === "") {
    throw respuestaInvalida();
  }
  return valor as TurnoTomado;
}

/** El token solo arma la ruta: no se guarda ni se registra (RN-38). */
function ruta(token: string, accion = "") {
  return `/public/ofertas/${encodeURIComponent(token)}${accion}`;
}

export async function consultarOferta(token: string) {
  return leerOferta(await api.get<unknown>(ruta(token)));
}

export async function tomarOferta(token: string) {
  return leerTurnoTomado(await api.post<unknown>(ruta(token, "/tomar")));
}

/** Estado en que queda la oferta tras un 409 de la toma; `undefined` si el error no es un rechazo de la toma. */
export function estadoTrasRechazo(error: unknown): Exclude<EstadoOferta, "DISPONIBLE"> | undefined {
  if (!(error instanceof ErrorApi) || error.estado !== 409) return undefined;
  if (error.codigo === "HUECO_YA_TOMADO") return "TOMADA";
  if (error.codigo === "OFERTA_VENCIDA") return "VENCIDA";
  return "NO_DISPONIBLE";
}

/** Un mismo texto para el estado de la consulta y para el rechazo de la toma, así no divergen. */
export const MENSAJE_ESTADO: Readonly<Record<Exclude<EstadoOferta, "DISPONIBLE">, string>> = {
  TOMADA: "Este turno ya fue tomado.",
  VENCIDA: "Esta oferta venció: el horario ya empezó.",
  NO_DISPONIBLE: "Esta oferta ya no está disponible.",
};
