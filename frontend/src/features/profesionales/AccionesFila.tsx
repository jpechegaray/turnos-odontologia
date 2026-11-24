import type { ReactNode } from "react";
import { Boton } from "../../shared/ui";

export type AccionesFilaProps<T extends Readonly<{ activo: boolean }>> = Readonly<{
  elemento: T;
  confirmandoBaja: boolean;
  onEditar: (elemento: T) => void;
  onReactivar: (elemento: T) => void;
  onSolicitarBaja: (elemento: T) => void;
  onConfirmarBaja: (elemento: T) => void;
  onCancelarBaja: () => void;
  /** Botones adicionales (p. ej. «Prestaciones»); se muestran antes de las acciones de escritura. */
  extra?: ReactNode;
}>;

/** Acciones de una fila; la baja pide confirmación en la propia fila. */
export function AccionesFila<T extends Readonly<{ activo: boolean }>>({
  elemento,
  confirmandoBaja,
  onEditar,
  onReactivar,
  onSolicitarBaja,
  onConfirmarBaja,
  onCancelarBaja,
  extra,
}: AccionesFilaProps<T>) {
  if (confirmandoBaja) {
    return (
      <div>
        <Boton variante="peligro" tamano="chico" onClick={() => onConfirmarBaja(elemento)}>Confirmar baja</Boton>
        <Boton variante="secundaria" tamano="chico" onClick={onCancelarBaja}>Cancelar</Boton>
      </div>
    );
  }

  return (
    <div>
      {extra}
      <Boton variante="secundaria" tamano="chico" onClick={() => onEditar(elemento)}>Editar</Boton>
      {elemento.activo ? (
        <Boton variante="peligro-atenuado" tamano="chico" onClick={() => onSolicitarBaja(elemento)}>
          Dar de baja
        </Boton>
      ) : (
        <Boton variante="secundaria" tamano="chico" onClick={() => onReactivar(elemento)}>Reactivar</Boton>
      )}
    </div>
  );
}
