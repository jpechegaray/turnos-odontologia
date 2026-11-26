import type { ClaveConfiguracion } from "./tipos";

export type DefinicionClave = Readonly<{
  clave: ClaveConfiguracion;
  etiqueta: string;
  tipo: "entero" | "texto";
  /** Límite inferior inclusivo de los enteros. */
  minimo?: number;
  /** Límite superior inclusivo de los enteros. */
  maximo?: number;
}>;

/**
 * Única fuente de verdad de las diez claves de configuración, sus etiquetas en español y sus rangos
 * (RN-36). Los rangos coinciden con los de C-08; el backend sigue siendo la autoridad final.
 */
export const CLAVES: readonly DefinicionClave[] = [
  {
    clave: "cancelacion.plazo_horas",
    etiqueta: "Plazo de cancelación (horas)",
    tipo: "entero",
    minimo: 0,
    maximo: 168,
  },
  {
    clave: "recordatorio.anticipacion_horas",
    etiqueta: "Anticipación del recordatorio (horas)",
    tipo: "entero",
    minimo: 1,
    maximo: 168,
  },
  {
    clave: "reserva_online.horizonte_dias",
    etiqueta: "Horizonte de reserva online (días)",
    tipo: "entero",
    minimo: 1,
  },
  {
    clave: "reserva_online.anticipacion_minima_horas",
    etiqueta: "Anticipación mínima de reserva online (horas)",
    tipo: "entero",
    minimo: 1,
  },
  {
    clave: "reserva_online.max_turnos_activos_por_email",
    etiqueta: "Máximo de turnos activos por email",
    tipo: "entero",
    minimo: 1,
  },
  {
    clave: "lista_espera.max_avisos_por_hueco",
    etiqueta: "Máximo de avisos por hueco en lista de espera",
    tipo: "entero",
    minimo: 1,
  },
  {
    clave: "consultorio.nombre",
    etiqueta: "Nombre del consultorio",
    tipo: "texto",
  },
  {
    clave: "consultorio.domicilio",
    etiqueta: "Domicilio del consultorio",
    tipo: "texto",
  },
  {
    clave: "consultorio.email_contacto",
    etiqueta: "Email de contacto del consultorio",
    tipo: "texto",
  },
  {
    clave: "consultorio.retencion_anios",
    etiqueta: "Retención de datos (años)",
    tipo: "entero",
    minimo: 1,
    maximo: 20,
  },
];
