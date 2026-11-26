import { ErrorApi } from "../../shared/api/errorApi";

/** Error para una respuesta que no tiene la forma esperada: se trata como un error de servidor recuperable. */
export function respuestaInvalida() {
  return new ErrorApi({ tipo: "servidor", estado: 0, detalle: "Respuesta inesperada del servidor." });
}

export function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null;
}

/** Intervalo `[inicio, fin)` con dos instantes ISO válidos y ordenados. */
export function esIntervalo(valor: Record<string, unknown>) {
  if (typeof valor.inicio !== "string" || typeof valor.fin !== "string") return false;
  const inicio = Date.parse(valor.inicio);
  const fin = Date.parse(valor.fin);
  return Number.isFinite(inicio) && Number.isFinite(fin) && inicio < fin;
}

export function esTexto(valor: unknown): valor is string {
  return typeof valor === "string";
}

/** 404 uniforme del enlace: inexistente, vencido o revocado. */
export function esEnlaceNoDisponible(error: unknown) {
  return error instanceof ErrorApi && error.estado === 404;
}

export const MENSAJE_REINTENTO = "No pudimos completar la operación. Intentá de nuevo en unos minutos.";
