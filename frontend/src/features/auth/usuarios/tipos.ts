import type { Rol } from "../../../app/rutas/tipos";

/** Representación de `/api/usuarios`: nunca trae credenciales ni el estado de bloqueo. */
export type UsuarioRespuesta = Readonly<{
  id: number;
  email: string;
  nombre: string;
  rol: Rol;
  profesionalId: number | null;
  activo: boolean;
}>;

/** Cuerpo del `POST /api/usuarios`. La contraseña viaja tal como se escribió. */
export type UsuarioAlta = Readonly<{
  email: string;
  nombre: string;
  rol: Rol;
  password: string;
  profesionalId: number | null;
}>;

/** Cuerpo del `PUT /api/usuarios/{id}`: reemplazo completo, sin contraseña. */
export type UsuarioModificacion = Readonly<{
  email: string;
  nombre: string;
  rol: Rol;
  profesionalId: number | null;
  activo: boolean;
}>;

/** Lo que la pantalla necesita de `GET /api/profesionales` para mostrar y elegir el vínculo. */
export type ProfesionalVinculable = Readonly<{
  id: number;
  nombre: string;
  apellido: string;
  activo: boolean;
}>;
