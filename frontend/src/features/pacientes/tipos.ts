export type PacienteRespuesta = Readonly<{
  id: number;
  nombre: string;
  apellido: string;
  dni: string | null;
  email: string | null;
  telefono: string | null;
  obraSocial: string | null;
  nroAfiliado: string | null;
  notas: string | null;
  activo: boolean;
  creadoEn: string;
}>;

export type PacienteSolicitud = Readonly<{
  nombre: string;
  apellido: string;
  dni: string | null;
  email: string | null;
  telefono: string | null;
  obraSocial: string | null;
  nroAfiliado: string | null;
  notas: string | null;
}>;

export type FiltrosPacientes = Readonly<{
  apellido: string;
  nombre: string;
  dni: string;
  email: string;
}>;

export type CampoPaciente = keyof PacienteSolicitud;
