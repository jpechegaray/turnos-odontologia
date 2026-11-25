/** Bloqueo tal como lo devuelve `GET /api/profesionales/{id}/bloqueos` (C-12), con instantes en UTC. */
export type Bloqueo = Readonly<{
  id: number;
  profesionalId: number;
  /** ISO 8601 en UTC; el bloqueo es `[inicio, fin)`. */
  inicio: string;
  fin: string;
  motivo: string;
}>;

/** Cuerpo de `POST`: instantes ISO 8601 en UTC y motivo recortado. */
export type BloqueoSolicitud = Readonly<{ inicio: string; fin: string; motivo: string }>;

/** Lo que se ingresa en el formulario: fecha `aaaa-mm-dd` y hora `HH:mm` en hora de Buenos Aires. */
export type FormularioBloqueoValores = Readonly<{
  fechaInicio: string;
  horaInicio: string;
  fechaFin: string;
  horaFin: string;
  motivo: string;
}>;
