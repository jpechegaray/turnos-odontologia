export type PrestacionRespuesta = Readonly<{
  id: number;
  nombre: string;
  duracionDefaultMin: number;
  activa: boolean;
}>;

export type PrestacionSolicitud = Readonly<{
  nombre: string;
  duracionDefaultMin: number;
}>;

export type PrestacionModificacion = Readonly<{
  nombre: string;
  duracionDefaultMin: number;
  activa: boolean;
}>;
