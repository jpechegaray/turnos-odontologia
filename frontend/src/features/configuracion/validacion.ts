import { CLAVES } from "./claves";
import type { ClaveConfiguracion, ConfiguracionRespuesta, ConfiguracionTexto } from "./tipos";

export const MENSAJE_RN36 = "Hay valores fuera de los límites permitidos.";

export type ErroresConfiguracion = Partial<Record<ClaveConfiguracion, string>>;

/**
 * Entero estricto sobre el texto recortado: evita que `Number("2,5")` o `Number("")` se cuelen como
 * valores válidos por accidente. Devuelve `undefined` si el texto no es un entero.
 */
export function enteroEstricto(texto: string): number | undefined {
  const recortado = texto.trim();
  return /^-?\d+$/u.test(recortado) ? Number(recortado) : undefined;
}

/**
 * RN-36: los siete enteros dentro de su rango y los tres textos del consultorio no en blanco. Devuelve un mensaje
 * por campo inválido; un resultado vacío significa que la configuración se puede enviar.
 */
export function validarConfiguracion(valores: ConfiguracionTexto): ErroresConfiguracion {
  const errores: ErroresConfiguracion = {};
  for (const def of CLAVES) {
    const texto = valores[def.clave];
    if (def.tipo === "texto") {
      if (texto.trim() === "") errores[def.clave] = MENSAJE_RN36;
      continue;
    }
    const valor = enteroEstricto(texto);
    if (
      valor === undefined ||
      (def.minimo !== undefined && valor < def.minimo) ||
      (def.maximo !== undefined && valor > def.maximo)
    ) {
      errores[def.clave] = MENSAJE_RN36;
    }
  }
  return errores;
}

/** Arma el `PUT` de reemplazo completo con exactamente las diez claves y sus tipos nativos. */
export function aSolicitud(valores: ConfiguracionTexto): ConfiguracionRespuesta {
  return {
    "cancelacion.plazo_horas": Number(valores["cancelacion.plazo_horas"].trim()),
    "recordatorio.anticipacion_horas": Number(valores["recordatorio.anticipacion_horas"].trim()),
    "reserva_online.horizonte_dias": Number(valores["reserva_online.horizonte_dias"].trim()),
    "reserva_online.anticipacion_minima_horas": Number(
      valores["reserva_online.anticipacion_minima_horas"].trim(),
    ),
    "reserva_online.max_turnos_activos_por_email": Number(
      valores["reserva_online.max_turnos_activos_por_email"].trim(),
    ),
    "lista_espera.max_avisos_por_hueco": Number(valores["lista_espera.max_avisos_por_hueco"].trim()),
    "consultorio.nombre": valores["consultorio.nombre"].trim(),
    "consultorio.domicilio": valores["consultorio.domicilio"].trim(),
    "consultorio.email_contacto": valores["consultorio.email_contacto"].trim(),
    "consultorio.retencion_anios": Number(valores["consultorio.retencion_anios"].trim()),
  };
}

/** Convierte la respuesta vigente a los valores de texto que edita el formulario. */
export function aTexto(configuracion: ConfiguracionRespuesta): ConfiguracionTexto {
  return {
    "cancelacion.plazo_horas": String(configuracion["cancelacion.plazo_horas"]),
    "recordatorio.anticipacion_horas": String(configuracion["recordatorio.anticipacion_horas"]),
    "reserva_online.horizonte_dias": String(configuracion["reserva_online.horizonte_dias"]),
    "reserva_online.anticipacion_minima_horas": String(
      configuracion["reserva_online.anticipacion_minima_horas"],
    ),
    "reserva_online.max_turnos_activos_por_email": String(
      configuracion["reserva_online.max_turnos_activos_por_email"],
    ),
    "lista_espera.max_avisos_por_hueco": String(configuracion["lista_espera.max_avisos_por_hueco"]),
    "consultorio.nombre": configuracion["consultorio.nombre"],
    "consultorio.domicilio": configuracion["consultorio.domicilio"],
    "consultorio.email_contacto": configuracion["consultorio.email_contacto"],
    "consultorio.retencion_anios": String(configuracion["consultorio.retencion_anios"]),
  };
}