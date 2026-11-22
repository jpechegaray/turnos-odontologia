import { aMilisegundos, type Instante } from "./zonaHoraria";

/** Intervalo semiabierto [inicio, fin) en milisegundos UTC, igual que Intervalo del backend (RN-02). */
export type Intervalo = Readonly<{ inicio: number; fin: number }>;

export function crearIntervalo(inicio: Instante, fin: Instante): Intervalo {
  const desde = aMilisegundos(inicio);
  const hasta = aMilisegundos(fin);
  if (hasta <= desde) {
    throw new RangeError("El fin del intervalo debe ser posterior a su inicio");
  }
  return Object.freeze({ inicio: desde, fin: hasta });
}

/** Un turno que empieza justo cuando termina otro no se solapa. */
export function seSolapan(a: Intervalo, b: Intervalo): boolean {
  return a.inicio < b.fin && b.inicio < a.fin;
}

export function contiene(intervalo: Intervalo, instante: Instante): boolean {
  const milis = aMilisegundos(instante);
  return intervalo.inicio <= milis && milis < intervalo.fin;
}
