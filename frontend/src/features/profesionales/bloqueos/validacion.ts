import { aInstanteUtc } from "../../../shared/fecha/zonaHoraria";
import type { BloqueoSolicitud, FormularioBloqueoValores } from "./tipos";

export const MENSAJE_FECHA_INVALIDA = "Ingresá una fecha y una hora válidas.";
export const MENSAJE_FIN_POSTERIOR = "El fin debe ser posterior al inicio.";
export const MENSAJE_MOTIVO_OBLIGATORIO = "Ingresá el motivo del bloqueo.";
export const MENSAJE_MOTIVO_LARGO = "El motivo no puede superar los 255 caracteres.";

const LARGO_MAXIMO_MOTIVO = 255;

export type ErroresBloqueo = Readonly<Partial<Record<"inicio" | "fin" | "motivo", string>>>;

export type ResultadoValidacion =
  | Readonly<{ valido: true; cuerpo: BloqueoSolicitud }>
  | Readonly<{ valido: false; errores: ErroresBloqueo }>;

/** Instante UTC de una fecha y hora de Buenos Aires, o `undefined` si están vacías o mal formadas. */
function instante(fecha: string, hora: string): Date | undefined {
  try {
    return aInstanteUtc(fecha, hora);
  } catch {
    return undefined;
  }
}

/**
 * Valida el formulario y, si es válido, arma el cuerpo del `POST` con los instantes en UTC calculados
 * en hora de Buenos Aires (DD-05). Devuelve todos los errores por campo a la vez.
 */
export function validarBloqueo(valores: FormularioBloqueoValores): ResultadoValidacion {
  const errores: { inicio?: string; fin?: string; motivo?: string } = {};
  const inicio = instante(valores.fechaInicio, valores.horaInicio);
  const fin = instante(valores.fechaFin, valores.horaFin);
  if (!inicio) errores.inicio = MENSAJE_FECHA_INVALIDA;
  if (!fin) errores.fin = MENSAJE_FECHA_INVALIDA;
  else if (inicio && fin.getTime() <= inicio.getTime()) errores.fin = MENSAJE_FIN_POSTERIOR;

  const motivo = valores.motivo.trim();
  if (!motivo) errores.motivo = MENSAJE_MOTIVO_OBLIGATORIO;
  else if (motivo.length > LARGO_MAXIMO_MOTIVO) errores.motivo = MENSAJE_MOTIVO_LARGO;

  if (!inicio || !fin || Object.keys(errores).length > 0) return { valido: false, errores };
  return { valido: true, cuerpo: { inicio: inicio.toISOString(), fin: fin.toISOString(), motivo } };
}
