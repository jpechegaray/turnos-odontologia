export type SillonRespuesta = Readonly<{
  id: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
}>;

export type SillonSolicitud = Readonly<{
  nombre: string;
  descripcion: string | null;
}>;

export type SillonModificacion = Readonly<{
  nombre: string;
  descripcion: string | null;
  activo: boolean;
}>;
