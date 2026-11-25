import { useState, type FormEvent } from "react";
import type { Rol } from "../../../app/rutas/tipos";
import { Alerta, Boton, Campo, Tarjeta } from "../../../shared/ui";
import { CampoSeleccion } from "./CampoSeleccion";
import { traducirErrorUsuario } from "./errores";
import { ETIQUETAS_ROL } from "./roles";
import type { ProfesionalVinculable, UsuarioRespuesta } from "./tipos";
import { tieneErrores, validarUsuario, type ErroresUsuario } from "./validacion";

const ROLES: readonly Rol[] = ["RECEPCION", "ODONTOLOGO", "ADMIN"];
const OPCIONES_ROL = ROLES.map((rol) => ({ valor: rol, etiqueta: ETIQUETAS_ROL[rol] }));

/** Lo que el formulario entrega: textos recortados y vínculo nulo salvo para el odontólogo. */
export type DatosFormularioUsuario = Readonly<{
  email: string;
  nombre: string;
  rol: Rol;
  profesionalId: number | null;
  /** Solo en el alta; la edición no cambia la contraseña (D3). */
  password?: string;
}>;

export type FormularioUsuarioProps = Readonly<{
  /** Con valor inicial el formulario edita: precarga los datos y no muestra la contraseña. */
  valorInicial?: UsuarioRespuesta;
  /** Profesionales que se pueden vincular a un odontólogo. */
  profesionales: readonly ProfesionalVinculable[];
  onEnviar: (datos: DatosFormularioUsuario) => Promise<void>;
  onCancelar?: () => void;
}>;

export function FormularioUsuario({ valorInicial, profesionales, onEnviar, onCancelar }: FormularioUsuarioProps) {
  const editando = valorInicial !== undefined;
  const [email, actualizarEmail] = useState(valorInicial?.email ?? "");
  const [nombre, actualizarNombre] = useState(valorInicial?.nombre ?? "");
  const [rol, actualizarRol] = useState<Rol>(valorInicial?.rol ?? "RECEPCION");
  const [password, actualizarPassword] = useState("");
  const [profesionalId, actualizarProfesionalId] = useState<number | null>(valorInicial?.profesionalId ?? null);
  const [errores, actualizarErrores] = useState<ErroresUsuario>({});
  const [errorRemoto, actualizarErrorRemoto] = useState<string>();
  const [pendiente, actualizarPendiente] = useState(false);
  const esOdontologo = rol === "ODONTOLOGO";

  function limpiarError(campo: keyof ErroresUsuario) {
    actualizarErrores((previos) => ({ ...previos, [campo]: undefined }));
  }

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (pendiente) return;
    actualizarErrorRemoto(undefined);
    const datos: DatosFormularioUsuario = {
      email: email.trim(),
      nombre: nombre.trim(),
      rol,
      profesionalId: esOdontologo ? profesionalId : null,
      ...(editando ? {} : { password }),
    };
    const encontrados = validarUsuario(datos);
    actualizarErrores(encontrados);
    if (tieneErrores(encontrados)) return;

    actualizarPendiente(true);
    try {
      await onEnviar(datos);
      actualizarEmail("");
      actualizarNombre("");
      actualizarRol("RECEPCION");
      actualizarProfesionalId(null);
      actualizarErrores({});
    } catch (error) {
      const traducido = traducirErrorUsuario(error);
      actualizarErrorRemoto(traducido.mensaje);
      actualizarErrores(traducido.campos);
    } finally {
      // La contraseña nunca queda en el formulario después de un envío, salga bien o mal.
      actualizarPassword("");
      actualizarPendiente(false);
    }
  }

  return (
    <Tarjeta titulo={editando ? "Editar usuario" : "Nuevo usuario"}>
      <form noValidate onSubmit={(evento) => void enviar(evento)}>
        <Campo
          etiqueta="Email"
          name="email"
          type="email"
          autoComplete="off"
          value={email}
          onChange={(evento) => {
            actualizarEmail(evento.target.value);
            limpiarError("email");
          }}
          error={errores.email}
        />
        <Campo
          etiqueta="Nombre"
          name="nombre"
          value={nombre}
          onChange={(evento) => {
            actualizarNombre(evento.target.value);
            limpiarError("nombre");
          }}
          error={errores.nombre}
        />
        <CampoSeleccion
          etiqueta="Rol"
          name="rol"
          opciones={OPCIONES_ROL}
          value={rol}
          onChange={(evento) => {
            // Solo el odontólogo tiene vínculo: al cambiar de rol se descarta el profesional elegido.
            actualizarRol(evento.target.value as Rol);
            actualizarProfesionalId(null);
            limpiarError("profesionalId");
          }}
        />
        {!editando && (
          <Campo
            etiqueta="Contraseña inicial"
            name="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(evento) => {
              actualizarPassword(evento.target.value);
              limpiarError("password");
            }}
            error={errores.password}
          />
        )}
        {esOdontologo && (
          <CampoSeleccion
            etiqueta="Profesional"
            name="profesionalId"
            vacia="Elegí un profesional"
            opciones={profesionales.map((p) => ({ valor: String(p.id), etiqueta: `${p.apellido}, ${p.nombre}` }))}
            value={profesionalId === null ? "" : String(profesionalId)}
            onChange={(evento) => {
              actualizarProfesionalId(evento.target.value === "" ? null : Number(evento.target.value));
              limpiarError("profesionalId");
            }}
            error={errores.profesionalId}
          />
        )}
        {errorRemoto && <Alerta tipo="peligro" titulo={errorRemoto} />}
        <Boton type="submit" disabled={pendiente}>{editando ? "Guardar cambios" : "Agregar usuario"}</Boton>
        {onCancelar && (
          <Boton variante="secundaria" onClick={onCancelar}>Cancelar</Boton>
        )}
      </form>
    </Tarjeta>
  );
}
