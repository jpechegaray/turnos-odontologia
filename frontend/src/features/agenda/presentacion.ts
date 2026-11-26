import { fechaLocal, formatearHora, type Instante } from "../../shared/fecha/zonaHoraria";
import type { CatalogosAgenda } from "./useCatalogosAgenda";
import type { TurnoAgenda } from "./tipos";

export type Nombres = Readonly<{
  profesional: (id: number) => string;
  sillon: (id: number) => string;
  prestacion: (id: number) => string;
}>;

/** Si un catálogo no cargó o no tiene el id, se usa un nombre de respaldo con el id. */
export function nombresDe({ profesionales, sillones, prestaciones }: CatalogosAgenda): Nombres {
  const porProfesional = new Map(profesionales.map((p) => [p.id, `${p.nombre} ${p.apellido}`]));
  const porSillon = new Map(sillones.map((s) => [s.id, s.nombre]));
  const porPrestacion = new Map(prestaciones.map((p) => [p.id, p.nombre]));
  return {
    profesional: (id) => porProfesional.get(id) ?? `Profesional ${id}`,
    sillon: (id) => porSillon.get(id) ?? `Sillón ${id}`,
    prestacion: (id) => porPrestacion.get(id) ?? `Prestación ${id}`,
  };
}

export function nombreDelPaciente(turno: TurnoAgenda): string | undefined {
  return turno.paciente ? `${turno.paciente.nombre} ${turno.paciente.apellido}` : undefined;
}

/** dd/mm de una fecha civil aaaa-mm-dd, con el año si se pide. */
export function fechaCorta(fecha: string, conAnio = false): string {
  const [anio, mes, dia] = fecha.split("-");
  return conAnio ? `${dia}/${mes}/${anio}` : `${dia}/${mes}`;
}

/** "10:00 a 10:30" en hora de Buenos Aires; con la fecha de cada extremo si caen en días distintos. */
export function rangoLegible(inicio: Instante, fin: Instante): string {
  const diaInicio = fechaLocal(inicio);
  const diaFin = fechaLocal(fin);
  if (diaInicio === diaFin) return `${formatearHora(inicio)} a ${formatearHora(fin)}`;
  return `${fechaCorta(diaInicio)} ${formatearHora(inicio)} a ${fechaCorta(diaFin)} ${formatearHora(fin)}`;
}

/** "08:00" a partir de los minutos del día. */
export function horaDeMinutos(minutos: number): string {
  const dos = (n: number) => String(n).padStart(2, "0");
  return `${dos(Math.floor(minutos / 60))}:${dos(minutos % 60)}`;
}

/** "Lunes 7 de diciembre de 2026" a partir de una fecha civil aaaa-mm-dd. */
export function fechaLarga(fecha: string): string {
  const [anio, mes, dia] = fecha.split("-").map(Number);
  const texto = new Intl.DateTimeFormat("es-AR", { weekday: "long", day: "numeric", month: "long", year: "numeric", timeZone: "UTC" })
    .format(new Date(Date.UTC(anio, mes - 1, dia)));
  return texto.charAt(0).toUpperCase() + texto.slice(1).replace(",", "");
}
