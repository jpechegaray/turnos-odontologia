import { aInstanteUtc, aMilisegundos, fechaLocal as fechaLocalDe, type Instante } from "../../shared/fecha/zonaHoraria";

/** Cada fila de la grilla diaria representa 15 minutos (`--agenda-row-h` en tokens.css). */
export const MINUTOS_FILA = 15;
const MINUTOS_DIA = 24 * 60;
const MILIS_MINUTO = 60_000;
/** Rango por defecto de un día sin datos: de 08:00 a 20:00. */
const RANGO_VACIO = { desdeMin: 8 * 60, hastaMin: 20 * 60 };
/** La fila 1 es el encabezado de las columnas. */
const PRIMERA_FILA = 2;

/** Parte de un intervalo dentro de un día local, en minutos desde su medianoche: `[desdeMin, hastaMin)`. */
export type Tramo = Readonly<{ desdeMin: number; hastaMin: number }>;

const PATRON_FECHA = /^(\d{4})-(\d{2})-(\d{2})$/;
const NOMBRES_DIA = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"];

/** Fecha civil (aaaa-mm-dd) como medianoche UTC: sirve para contar días sin pasar por la zona. */
function fechaCivil(fecha: string): Date {
  const partes = PATRON_FECHA.exec(fecha);
  if (!partes) throw new RangeError("Fecha mal formada");
  return new Date(Date.UTC(Number(partes[1]), Number(partes[2]) - 1, Number(partes[3])));
}

export function sumarDias(fecha: string, dias: number): string {
  const civil = fechaCivil(fecha);
  civil.setUTCDate(civil.getUTCDate() + dias);
  return civil.toISOString().slice(0, 10);
}

/** Las siete fechas de la semana que empieza en `lunes`. */
export function diasDeLaSemana(lunes: string): string[] {
  return Array.from({ length: 7 }, (_, i) => sumarDias(lunes, i));
}

export function nombreDelDia(fecha: string): string {
  return NOMBRES_DIA[fechaCivil(fecha).getUTCDay()];
}

/**
 * Recorta `[inicio, fin)` al día local `fecha` (`[00:00, 24:00)` en Buenos Aires). Los bordes del día
 * se calculan como instantes, así que no se asume un desfase fijo (RN-14).
 */
export function tramoEnDia(inicio: Instante, fin: Instante, fecha: string): Tramo | undefined {
  const comienzoDia = aInstanteUtc(fecha, "00:00").getTime();
  const finDia = aInstanteUtc(sumarDias(fecha, 1), "00:00").getTime();
  const desde = Math.max(aMilisegundos(inicio), comienzoDia);
  const hasta = Math.min(aMilisegundos(fin), finDia);
  if (hasta <= desde) return undefined;
  return {
    desdeMin: Math.round((desde - comienzoDia) / MILIS_MINUTO),
    hastaMin: Math.min(Math.round((hasta - comienzoDia) / MILIS_MINUTO), MINUTOS_DIA),
  };
}

/** Desde la primera hora en punto con datos hasta la última; de 08:00 a 20:00 si no hay datos. */
export function rangoDeGrilla(tramos: readonly Tramo[]): Tramo {
  if (tramos.length === 0) return RANGO_VACIO;
  const desde = Math.min(...tramos.map((t) => t.desdeMin));
  const hasta = Math.max(...tramos.map((t) => t.hastaMin));
  return { desdeMin: Math.floor(desde / 60) * 60, hastaMin: Math.min(Math.ceil(hasta / 60) * 60, MINUTOS_DIA) };
}

export function cantidadDeFilas(rango: Tramo): number {
  return (rango.hastaMin - rango.desdeMin) / MINUTOS_FILA;
}

/** Filas de CSS grid (`grid-row: desde / hasta`) que ocupa un tramo; al menos una. */
export function filasDe(tramo: Tramo, rango: Tramo): Readonly<{ desde: number; hasta: number }> {
  const desde = PRIMERA_FILA + Math.floor((tramo.desdeMin - rango.desdeMin) / MINUTOS_FILA);
  const hasta = PRIMERA_FILA + Math.ceil((tramo.hastaMin - rango.desdeMin) / MINUTOS_FILA);
  return { desde, hasta: Math.max(hasta, desde + 1) };
}

/** Minutos desde la medianoche de Buenos Aires de un instante. */
export function minutosDelDia(instante: number): number {
  const comienzo = aInstanteUtc(fechaLocalDe(instante), "00:00").getTime();
  return Math.floor((instante - comienzo) / MILIS_MINUTO);
}
