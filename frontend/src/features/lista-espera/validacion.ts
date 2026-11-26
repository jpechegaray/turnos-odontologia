/** Campos del formulario de alta con error propio; las claves coinciden con las de `SolicitudAlta`. */
export type CampoAlta = "pacienteId" | "prestacionId" | "fechaDesde" | "fechaHasta" | "franjaHasta";

export type DatosAlta = Readonly<{
  pacienteId: string;
  prestacionId: string;
  profesionalId: string;
  fechaDesde: string;
  fechaHasta: string;
  franjaDesde: string;
  franjaHasta: string;
}>;

export const ALTA_VACIA: DatosAlta = {
  pacienteId: "", prestacionId: "", profesionalId: "", fechaDesde: "", fechaHasta: "", franjaDesde: "", franjaHasta: "",
};

export type ErroresAlta = Partial<Record<CampoAlta, string>>;

export const MENSAJE_HORA_INVERTIDA = "La hora de fin debe ser posterior a la de inicio.";

/** Mismas reglas de forma que `SolicitudAlta` (RN-21); el resto lo decide el servidor. */
export function validarAlta(datos: DatosAlta): ErroresAlta {
  const errores: ErroresAlta = {};
  if (!datos.pacienteId) errores.pacienteId = "Elegí un paciente.";
  if (!datos.prestacionId) errores.prestacionId = "Elegí una prestación.";
  if (!datos.fechaHasta) errores.fechaHasta = "Indicá hasta qué fecha.";
  else if (datos.fechaDesde && datos.fechaHasta < datos.fechaDesde) {
    errores.fechaHasta = "La fecha de fin no puede ser anterior a la de inicio.";
  }
  if (Boolean(datos.franjaDesde) !== Boolean(datos.franjaHasta)) {
    errores.franjaHasta = "Completá las dos horas de la franja o ninguna.";
  } else if (datos.franjaDesde && datos.franjaHasta <= datos.franjaDesde) {
    errores.franjaHasta = MENSAJE_HORA_INVERTIDA;
  }
  return errores;
}

export type DatosHueco = Readonly<{ profesionalId: string; fecha: string; horaInicio: string; horaFin: string }>;
export type ErroresHueco = Partial<Record<keyof DatosHueco, string>>;

export const HUECO_VACIO: DatosHueco = { profesionalId: "", fecha: "", horaInicio: "", horaFin: "" };

export function validarHueco(datos: DatosHueco): ErroresHueco {
  const errores: ErroresHueco = {};
  if (!datos.profesionalId) errores.profesionalId = "Elegí un profesional.";
  if (!datos.fecha) errores.fecha = "Indicá la fecha del hueco.";
  if (!datos.horaInicio) errores.horaInicio = "Indicá la hora de inicio.";
  if (!datos.horaFin) errores.horaFin = "Indicá la hora de fin.";
  else if (datos.horaInicio && datos.horaFin <= datos.horaInicio) errores.horaFin = MENSAJE_HORA_INVERTIDA;
  return errores;
}

export function sinErrores(errores: Readonly<Record<string, string | undefined>>) {
  return Object.values(errores).every((error) => !error);
}
