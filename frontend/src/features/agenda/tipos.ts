/** Estados de un turno (RN-12) tal como los devuelve `GET /api/agenda`. */
export type EstadoTurno = "PROGRAMADO" | "CONFIRMADO" | "ATENDIDO" | "AUSENTE" | "CANCELADO";

export type Vista = "dia" | "semana";

/** Columnas de la vista diaria. */
export type Agrupacion = "profesional" | "sillon";

/** `paciente` es nulo cuando la API lo oculta: turnos de otro profesional para el odontólogo (RN-33). */
export type TurnoAgenda = Readonly<{
  id: number;
  inicio: string;
  fin: string;
  duracionMin: number;
  estado: EstadoTurno;
  profesionalId: number;
  sillonId: number;
  prestacionId: number;
  pacienteId: number;
  paciente: Readonly<{ nombre: string; apellido: string }> | null;
  notas: string | null;
}>;

/** Tramo de atención de un profesional proyectado en una fecha del período. */
export type HorarioAgenda = Readonly<{
  profesionalId: number;
  fecha: string;
  inicio: string;
  fin: string;
}>;

export type BloqueoAgenda = Readonly<{
  id: number;
  profesionalId: number;
  inicio: string;
  fin: string;
  motivo: string | null;
}>;

/** Respuesta de `GET /api/agenda` (C-28): `desde` y `hasta` son fechas locales inclusive. */
export type AgendaRespuesta = Readonly<{
  vista: Vista;
  desde: string;
  hasta: string;
  turnos: readonly TurnoAgenda[];
  horarios: readonly HorarioAgenda[];
  bloqueos: readonly BloqueoAgenda[];
}>;

/** Sin `fecha`, el servidor usa el día de hoy en Buenos Aires. */
export type ConsultaAgenda = Readonly<{
  vista: Vista;
  fecha?: string;
  profesionalId?: number;
  sillonId?: number;
}>;
