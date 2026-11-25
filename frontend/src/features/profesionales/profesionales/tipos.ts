export type ProfesionalRespuesta = Readonly<{
  id: number;
  nombre: string;
  apellido: string;
  matricula: string;
  especialidad: string | null;
  activo: boolean;
}>;

/** Cuerpo del `POST /api/profesionales`: textos recortados y especialidad nula si queda vacía. */
export type ProfesionalSolicitud = Readonly<{
  nombre: string;
  apellido: string;
  matricula: string;
  especialidad: string | null;
}>;

/** Cuerpo del `PUT /api/profesionales/{id}`: siempre lleva `activo` explícito. */
export type ProfesionalModificacion = ProfesionalSolicitud & Readonly<{ activo: boolean }>;

/**
 * Cuerpo del `PUT` a partir de la representación actual: los cambios del formulario (si los hay)
 * pisan los datos vigentes y `activo` se toma del cambio pedido o del valor actual.
 */
export function cuerpoModificacion(
  actual: ProfesionalRespuesta,
  cambios: Partial<ProfesionalModificacion> = {},
): ProfesionalModificacion {
  return {
    nombre: cambios.nombre ?? actual.nombre,
    apellido: cambios.apellido ?? actual.apellido,
    matricula: cambios.matricula ?? actual.matricula,
    especialidad: cambios.especialidad !== undefined ? cambios.especialidad : actual.especialidad,
    activo: cambios.activo ?? actual.activo,
  };
}
