import { api } from "../../../shared/api/clienteApi";
import type { EstadoVisualTurno } from "../../../shared/ui";
import { esIntervalo, esObjeto, esTexto, respuestaInvalida } from "../validacionPublica";

export type EstadoTurno = Exclude<EstadoVisualTurno, "CANCELADO_TARDE" | "BLOQUEO">;

export type TurnoPorToken = Readonly<{
  inicio: string;
  fin: string;
  estado: EstadoTurno;
  profesional: string;
  prestacion: string;
  cancelacionSeriaTardia: boolean;
}>;

const ESTADOS: readonly EstadoTurno[] = ["PROGRAMADO", "CONFIRMADO", "ATENDIDO", "AUSENTE", "CANCELADO"];

function leerTurno(valor: unknown): TurnoPorToken {
  if (!esObjeto(valor) || !esIntervalo(valor) || !esTexto(valor.profesional) || !esTexto(valor.prestacion)
    || typeof valor.cancelacionSeriaTardia !== "boolean"
    || !ESTADOS.includes(valor.estado as EstadoTurno)) {
    throw respuestaInvalida();
  }
  return valor as TurnoPorToken;
}

/** El token solo arma la ruta: no se guarda ni se registra (RN-07, RN-25). */
function ruta(token: string, accion = "") {
  return `/public/turnos/${encodeURIComponent(token)}${accion}`;
}

export async function consultarTurno(token: string) {
  return leerTurno(await api.get<unknown>(ruta(token)));
}

export async function confirmarTurno(token: string) {
  return leerTurno(await api.post<unknown>(ruta(token, "/confirmar")));
}

export async function cancelarTurno(token: string) {
  await api.post<void>(ruta(token, "/cancelar"));
}

export function admiteConfirmar(estado: EstadoTurno) {
  return estado === "PROGRAMADO";
}

export function admiteCancelar(estado: EstadoTurno) {
  return estado === "PROGRAMADO" || estado === "CONFIRMADO";
}
