import { useState, type ReactNode } from "react";
import { Boton } from "../../shared/ui";

export type ConfirmarEliminacionProps = Readonly<{
  /** Se espera la promesa; si rechaza, el error ya lo informó quien escribe. */
  onConfirmar: () => Promise<void>;
  deshabilitado: boolean;
  /** Acciones que se muestran antes de «Eliminar» mientras no se pide confirmación. */
  children?: ReactNode;
}>;

/** «Eliminar» con confirmación en el propio ítem, como la baja de `AccionesFila`. */
export function ConfirmarEliminacion({ onConfirmar, deshabilitado, children }: ConfirmarEliminacionProps) {
  const [confirmando, actualizarConfirmando] = useState(false);

  if (confirmando) {
    return (
      <>
        <Boton
          variante="peligro"
          tamano="chico"
          disabled={deshabilitado}
          onClick={() => {
            // El ítem puede desmontarse al recargar; el estado se cierra igual.
            void onConfirmar()
              .catch(() => undefined)
              .finally(() => actualizarConfirmando(false));
          }}
        >
          Confirmar eliminación
        </Boton>
        <Boton variante="secundaria" tamano="chico" onClick={() => actualizarConfirmando(false)}>Cancelar</Boton>
      </>
    );
  }

  return (
    <>
      {children}
      <Boton variante="peligro-atenuado" tamano="chico" onClick={() => actualizarConfirmando(true)}>Eliminar</Boton>
    </>
  );
}
