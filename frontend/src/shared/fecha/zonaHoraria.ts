/**
 * Horarios en la zona del consultorio (RN-14). La API trabaja con instantes UTC; la SPA los muestra y
 * los construye en Buenos Aires sin asumir un desfase fijo: el desfase se calcula con Intl para cada fecha.
 */
export const ZONA_CONSULTORIO = "America/Argentina/Buenos_Aires";

/** Instante como texto ISO 8601 (con zona), milisegundos desde la época o Date. */
export type Instante = string | number | Date;

const FORMATO_PARTES = new Intl.DateTimeFormat("en-US", {
  timeZone: ZONA_CONSULTORIO,
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hourCycle: "h23",
});

type PartesLocales = Readonly<{
  anio: number;
  mes: number;
  dia: number;
  hora: number;
  minuto: number;
  segundo: number;
}>;

export function aMilisegundos(instante: Instante): number {
  const milis = instante instanceof Date ? instante.getTime()
    : typeof instante === "number" ? instante
    : Date.parse(instante);
  if (!Number.isFinite(milis)) {
    throw new RangeError("Instante inválido");
  }
  return milis;
}

function partesLocales(milis: number): PartesLocales {
  const valores: Record<string, number> = {};
  for (const parte of FORMATO_PARTES.formatToParts(milis)) {
    if (parte.type !== "literal") valores[parte.type] = Number(parte.value);
  }
  return {
    anio: valores.year,
    mes: valores.month,
    dia: valores.day,
    hora: valores.hour,
    minuto: valores.minute,
    segundo: valores.second,
  };
}

const dosDigitos = (n: number) => String(n).padStart(2, "0");

/** Fecha local del consultorio en formato dd/mm/aaaa. */
export function formatearFecha(instante: Instante): string {
  const { dia, mes, anio } = partesLocales(aMilisegundos(instante));
  return `${dosDigitos(dia)}/${dosDigitos(mes)}/${anio}`;
}

/** Hora local del consultorio en formato HH:mm (24 h). */
export function formatearHora(instante: Instante): string {
  const { hora, minuto } = partesLocales(aMilisegundos(instante));
  return `${dosDigitos(hora)}:${dosDigitos(minuto)}`;
}

/** Fecha local del consultorio en formato aaaa-mm-dd, el mismo que usa la API para las fechas civiles. */
export function fechaLocal(instante: Instante): string {
  const { dia, mes, anio } = partesLocales(aMilisegundos(instante));
  return `${anio}-${dosDigitos(mes)}-${dosDigitos(dia)}`;
}

/** Minutos transcurridos desde la medianoche del consultorio (0 a 1439). */
export function minutosDelDia(instante: Instante): number {
  const { hora, minuto } = partesLocales(aMilisegundos(instante));
  return hora * 60 + minuto;
}

/** Diferencia en milisegundos entre la hora local de la zona y UTC en ese instante. */
function desfase(milis: number): number {
  const segundoExacto = Math.floor(milis / 1000) * 1000;
  const p = partesLocales(segundoExacto);
  return Date.UTC(p.anio, p.mes - 1, p.dia, p.hora, p.minuto, p.segundo) - segundoExacto;
}

const PATRON_FECHA = /^(\d{4})-(\d{2})-(\d{2})$/;
const PATRON_HORA = /^([01]\d|2[0-3]):([0-5]\d)$/;

/**
 * Convierte una fecha (aaaa-mm-dd) y una hora (HH:mm) locales de Buenos Aires al instante UTC.
 * El segundo cálculo del desfase cubre el caso, hoy teórico, de que la regla de la zona cambie entre
 * la estimación y el resultado.
 */
export function aInstanteUtc(fecha: string, hora: string): Date {
  const f = PATRON_FECHA.exec(fecha);
  const h = PATRON_HORA.exec(hora);
  if (!f || !h) {
    throw new RangeError("Fecha u hora local mal formada");
  }
  const [anio, mes, dia] = [Number(f[1]), Number(f[2]), Number(f[3])];
  const estimado = Date.UTC(anio, mes - 1, dia, Number(h[1]), Number(h[2]));
  const control = new Date(estimado);
  if (control.getUTCFullYear() !== anio || control.getUTCMonth() !== mes - 1 || control.getUTCDate() !== dia) {
    throw new RangeError("La fecha local no existe");
  }
  const primerDesfase = desfase(estimado);
  let resultado = estimado - primerDesfase;
  const segundoDesfase = desfase(resultado);
  if (segundoDesfase !== primerDesfase) {
    resultado = estimado - segundoDesfase;
  }
  return new Date(resultado);
}
