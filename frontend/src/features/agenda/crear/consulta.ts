import { aInstanteUtc } from "../../../shared/fecha/zonaHoraria";
import { sumarDias } from "../grilla";

export type HorarioSugerido = Readonly<{ inicio: string; fin: string; sillonId: number }>;

export function duracionValida(minutos: number): boolean {
  return minutos >= 5 && minutos <= 480 && minutos % 5 === 0;
}

export function rutaDeDisponibilidad(profesionalId: string, prestacionId: string, fecha: string, minutos: number): string {
  const params = new URLSearchParams({
    profesionalId,
    prestacionId,
    desde: aInstanteUtc(fecha, "00:00").toISOString(),
    hasta: aInstanteUtc(sumarDias(fecha, 1), "00:00").toISOString(),
    duracionMin: String(minutos),
  });
  return `/agenda/disponibilidad?${params}`;
}
