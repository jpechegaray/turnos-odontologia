import type { Rol } from "../../../app/rutas/tipos";
import { ErrorApi } from "../../../shared/api/errorApi";
import type { TurnoAgenda } from "../tipos";
import { mensajeDelTurno } from "../crear/errores";

export type AccionTurno = "reprogramar" | "cancelar" | "asistencia" | "corregir";
export type MarcaAsistencia = "ATENDIDO" | "AUSENTE";

export type Gestion =
  | Readonly<{ accion: "reprogramar"; cuerpo: { inicio: string; profesionalId: number; sillonId: number; duracionMin: number } }>
  | Readonly<{ accion: "cancelar"; cuerpo: { motivo?: string } }>
  | Readonly<{ accion: "asistencia" | "corregir"; cuerpo: { estado: MarcaAsistencia } }>;

export type TurnoGestionado = Readonly<{
  inicio: string;
  profesionalId: number;
  sillonId: number;
  cancelacionTardia: boolean;
  reprogramadoTarde: boolean;
}>;

export const ETIQUETAS_GESTION: Readonly<Record<AccionTurno, string>> = {
  reprogramar: "Reprogramar",
  cancelar: "Cancelar turno",
  asistencia: "Marcar asistencia",
  corregir: "Corregir asistencia",
};

export function accionesDeTurno(turno: TurnoAgenda, rol: Rol, ahora: number): readonly AccionTurno[] {
  if (rol === "ODONTOLOGO") return [];
  const activo = turno.estado === "PROGRAMADO" || turno.estado === "CONFIRMADO";
  const inicio = Date.parse(turno.inicio);
  if (!Number.isFinite(inicio)) return [];
  if (activo) return inicio > ahora ? ["reprogramar", "cancelar"] : ["asistencia"];
  if (rol === "ADMIN" && inicio <= ahora && (turno.estado === "ATENDIDO" || turno.estado === "AUSENTE")) return ["corregir"];
  return [];
}

export function rutaDeGestion(turnoId: number, accion: AccionTurno): string {
  return `/turnos/${turnoId}/${accion === "corregir" ? "asistencia" : accion}`;
}

export function requiereRecarga(error: unknown): boolean {
  return error instanceof ErrorApi && (error.tipo === "no-encontrado"
    || (error.tipo === "conflicto" && error.codigo !== "RN-01"
      && error.propiedades.recurso !== "PROFESIONAL" && error.propiedades.recurso !== "SILLON"));
}

export function mensajeErrorGestion(error: unknown): string {
  if (requiereRecarga(error)) return "El turno cambió o ya no está disponible. Actualizá la agenda antes de continuar.";
  if (error instanceof ErrorApi && error.tipo === "no-procesable") {
    if (error.codigo === "RN-05") return "El turno ya empezó. Registrá la asistencia desde la agenda.";
    if (error.codigo === "RN-20") return "El turno todavía no empezó. Registrá la asistencia a partir de su horario.";
    if (error.codigo === "RN-09") return "Usá una hora y una duración en múltiplos de 5 minutos. La duración debe estar entre 5 y 480 minutos.";
    if (error.codigo === "RN-11") return "El profesional no realiza esta prestación. Elegí otro profesional.";
    if (error.codigo?.startsWith("PACIENTE_") || error.codigo?.startsWith("PRESTACION_")) {
      return "El paciente o la prestación ya no están disponibles. Revisá la ficha del turno y actualizá la agenda.";
    }
  }
  return mensajeDelTurno(error);
}

export function mensajeGestionada(gestion: Gestion, turno: TurnoGestionado): string {
  switch (gestion.accion) {
    case "cancelar": return turno.cancelacionTardia ? "Turno cancelado. Cancelación tardía registrada." : "Turno cancelado.";
    case "reprogramar": return turno.reprogramadoTarde
      ? "Turno reprogramado. Tiene marca de reprogramación tardía." : "Turno reprogramado.";
    case "asistencia": return gestion.cuerpo.estado === "ATENDIDO" ? "Turno marcado como atendido." : "Turno marcado como ausente.";
    case "corregir": return gestion.cuerpo.estado === "ATENDIDO" ? "Asistencia corregida a atendido." : "Asistencia corregida a ausente.";
  }
}
