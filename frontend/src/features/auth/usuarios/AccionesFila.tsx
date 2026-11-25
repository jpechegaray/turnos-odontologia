import { Boton } from "../../../shared/ui";
import type { UsuarioRespuesta } from "./tipos";

export type AccionesFilaProps = Readonly<{
  usuario: UsuarioRespuesta;
  confirmandoDesactivacion: boolean;
  onEditar: (usuario: UsuarioRespuesta) => void;
  onReactivar: (usuario: UsuarioRespuesta) => void;
  onSolicitarDesactivacion: (usuario: UsuarioRespuesta) => void;
  onConfirmarDesactivacion: (usuario: UsuarioRespuesta) => void;
  onCancelarDesactivacion: () => void;
}>;

/** Acciones de una fila de usuario; la desactivación pide confirmación en la propia fila (D6). */
export function AccionesFila({
  usuario,
  confirmandoDesactivacion,
  onEditar,
  onReactivar,
  onSolicitarDesactivacion,
  onConfirmarDesactivacion,
  onCancelarDesactivacion,
}: AccionesFilaProps) {
  if (confirmandoDesactivacion) {
    return (
      <div>
        <Boton variante="peligro" tamano="chico" onClick={() => onConfirmarDesactivacion(usuario)}>
          Confirmar desactivación
        </Boton>
        <Boton variante="secundaria" tamano="chico" onClick={onCancelarDesactivacion}>Cancelar</Boton>
      </div>
    );
  }

  return (
    <div>
      <Boton variante="secundaria" tamano="chico" onClick={() => onEditar(usuario)}>Editar</Boton>
      {usuario.activo ? (
        <Boton variante="peligro-atenuado" tamano="chico" onClick={() => onSolicitarDesactivacion(usuario)}>
          Desactivar
        </Boton>
      ) : (
        <Boton variante="secundaria" tamano="chico" onClick={() => onReactivar(usuario)}>Reactivar</Boton>
      )}
    </div>
  );
}
