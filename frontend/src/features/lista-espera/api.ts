import { api } from "../../shared/api/clienteApi";
import type { EntradaRespuesta, EstadoEntrada, HuecoConsultado, SolicitudAltaEntrada } from "./tipos";

export function listarEntradas(estado?: EstadoEntrada) {
  const filtro = estado ? `?${new URLSearchParams({ estado })}` : "";
  return api.get<EntradaRespuesta[]>(`/lista-espera${filtro}`);
}

export function crearEntrada(solicitud: SolicitudAltaEntrada) {
  return api.post<EntradaRespuesta>("/lista-espera", solicitud);
}

/** La baja no borra: la entrada pasa a CANCELADA. */
export function darDeBaja(id: number) {
  return api.delete(`/lista-espera/${id}`);
}

export function consultarCompatibles(hueco: HuecoConsultado) {
  const parametros = new URLSearchParams({
    profesionalId: String(hueco.profesionalId), inicio: hueco.inicio, fin: hueco.fin,
  });
  return api.get<EntradaRespuesta[]>(`/lista-espera/compatibles?${parametros}`);
}

/** El backend calcula el fin con la duración de la prestación de la entrada (C-43). */
export function asignarHueco(entradaId: number, hueco: HuecoConsultado) {
  return api.post<unknown>(`/lista-espera/${entradaId}/asignar`, {
    profesionalId: hueco.profesionalId, inicio: hueco.inicio,
  });
}
