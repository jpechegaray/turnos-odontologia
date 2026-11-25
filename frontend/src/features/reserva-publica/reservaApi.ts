import { api } from "../../shared/api/clienteApi";
import { aInstanteUtc, formatearFecha, formatearHora } from "../../shared/fecha/zonaHoraria";

export type PrestacionPublica = { id: number; nombre: string };
export type ProfesionalPublico = { id: number; nombre: string; apellido: string };
export type Horario = { inicio: string; fin: string };
export type Consultorio = {
  nombre: string;
  domicilio: string;
  emailContacto: string;
  retencionAnios: number;
  versionConsentimiento: string;
};
export type ReservaConfirmada = Horario & {
  prestacion: string;
  profesional: string;
  token: string;
};

export const DATOS_INICIALES = { nombre: "", apellido: "", email: "", telefono: "", obraSocial: "" };
export type DatosPaciente = typeof DATOS_INICIALES;
export type CampoPaciente = keyof DatosPaciente;

export function pedirReserva(pedido: DatosPaciente & {
  prestacionId: number;
  profesionalId: number;
  inicio: string;
  versionConsentimiento: string;
}) {
  return api.post<ReservaConfirmada>("/public/reservas", { ...pedido, aceptaTratamientoDatos: true });
}

export function fechaTrasDias(fecha: string, dias: number) {
  const civil = new Date(`${fecha}T00:00:00Z`);
  civil.setUTCDate(civil.getUTCDate() + dias);
  return civil.toISOString().slice(0, 10);
}

export function rutaDisponibilidad(prestacionId: number, profesionalId: number, fecha: string) {
  const parametros = new URLSearchParams({
    prestacionId: String(prestacionId),
    profesionalId: String(profesionalId),
    desde: aInstanteUtc(fecha, "00:00").toISOString(),
    hasta: aInstanteUtc(fechaTrasDias(fecha, 7), "00:00").toISOString(),
  });
  return `/public/disponibilidad?${parametros}`;
}

export function etiquetaHorario(inicio: string) {
  return `${formatearFecha(inicio)} · ${formatearHora(inicio)}`;
}

function esHorario(valor: unknown): valor is Horario {
  if (!valor || typeof valor !== "object" || !("inicio" in valor) || !("fin" in valor)) return false;
  if (typeof valor.inicio !== "string" || typeof valor.fin !== "string") return false;
  const inicio = Date.parse(valor.inicio);
  const fin = Date.parse(valor.fin);
  return Number.isFinite(inicio) && Number.isFinite(fin) && inicio < fin;
}

export function leerAlternativas(valor: unknown): Horario[] {
  return Array.isArray(valor) ? valor.filter(esHorario) : [];
}
