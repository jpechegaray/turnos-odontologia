import { Boton } from "../../shared/ui";
import type { PrestacionRespuesta } from "./tipos";

export type AccionesFilaProps = Readonly<{
  prestacion: PrestacionRespuesta;
  confirmandoBaja: boolean;
  onEditar: (prestacion: PrestacionRespuesta) => void;
  onReactivar: (prestacion: PrestacionRespuesta) => void;
  onSolicitarBaja: (prestacion: PrestacionRespuesta) => void;
  onConfirmarBaja: (prestacion: PrestacionRespuesta) => void;
  onCancelarBaja: () => void;
}>;

/** Acciones de escritura de una fila; la baja pide confirmación en la propia fila. */
export function AccionesFila({
  prestacion,
  confirmandoBaja,
  onEditar,
  onReactivar,
  onSolicitarBaja,
  onConfirmarBaja,
  onCancelarBaja,
}: AccionesFilaProps) {
  if (confirmandoBaja) {
    return (
      <div>
        <Boton variante="peligro" tamano="chico" onClick={() => onConfirmarBaja(prestacion)}>Confirmar baja</Boton>
        <Boton variante="secundaria" tamano="chico" onClick={onCancelarBaja}>Cancelar</Boton>
      </div>
    );
  }

  return (
    <div>
      <Boton variante="secundaria" tamano="chico" onClick={() => onEditar(prestacion)}>Editar</Boton>
      {prestacion.activa ? (
        <Boton variante="peligro-atenuado" tamano="chico" onClick={() => onSolicitarBaja(prestacion)}>
          Dar de baja
        </Boton>
      ) : (
        <Boton variante="secundaria" tamano="chico" onClick={() => onReactivar(prestacion)}>Reactivar</Boton>
      )}
    </div>
  );
}
