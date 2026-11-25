import type { ProfesionalVinculable, UsuarioRespuesta } from "./tipos";

/**
 * Profesionales que se pueden vincular a un odontólogo (D2): los activos que ningún otro usuario
 * tiene vinculados. En la edición se suma el profesional actual del usuario editado, aunque esté
 * inactivo, para no perder el valor precargado. Conserva el orden de la API.
 */
export function opcionesProfesional(
  profesionales: readonly ProfesionalVinculable[],
  usuarios: readonly UsuarioRespuesta[],
  usuarioEditadoId?: number,
): ProfesionalVinculable[] {
  const vinculados = new Set(
    usuarios.filter((u) => u.id !== usuarioEditadoId && u.profesionalId !== null).map((u) => u.profesionalId),
  );
  const actual = usuarios.find((u) => u.id === usuarioEditadoId)?.profesionalId ?? null;
  return profesionales.filter((p) => p.id === actual || (p.activo && !vinculados.has(p.id)));
}
