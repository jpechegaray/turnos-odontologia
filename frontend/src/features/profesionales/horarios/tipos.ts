/** Tramo de atención tal como lo devuelve `GET /api/profesionales/{id}/horarios` (C-11). */
export type TramoHorario = Readonly<{
  id: number;
  profesionalId: number;
  /** ISO: 1 = lunes … 7 = domingo. */
  diaSemana: number;
  /** `HH:mm`. */
  horaInicio: string;
  /** `HH:mm`, posterior a `horaInicio`; el tramo es `[horaInicio, horaFin)`. */
  horaFin: string;
}>;

/** Cuerpo de `POST` y `PUT`. */
export type TramoSolicitud = Readonly<{ diaSemana: number; horaInicio: string; horaFin: string }>;
