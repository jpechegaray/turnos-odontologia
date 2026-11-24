/** Prestación tal como la devuelven `GET /api/prestaciones` y `GET|PUT /api/profesionales/{id}/prestaciones`. */
export type PrestacionAsociable = Readonly<{
  id: number;
  nombre: string;
  duracionDefaultMin: number;
  activa: boolean;
}>;

/** Cuerpo del `PUT /api/profesionales/{id}/prestaciones`: el conjunto completo, sin ids repetidos. */
export type ReemplazoPrestaciones = Readonly<{ prestacionIds: readonly number[] }>;
