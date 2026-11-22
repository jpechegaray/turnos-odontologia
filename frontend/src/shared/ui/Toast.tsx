import { Icono } from "./Icono";
import "./ui.css";

export type ToastProps = Readonly<{
  mensaje: string;
  accion?: { etiqueta: string; alHacerClic: () => void };
}>;

/** Aviso breve y no bloqueante: se anuncia como estado, sin interrumpir al lector de pantalla. */
export function Toast({ mensaje, accion }: ToastProps) {
  return (
    <div className="toast" role="status">
      <Icono nombre="check" />
      <span>{mensaje}</span>
      {accion && (
        <button type="button" onClick={accion.alHacerClic}>
          {accion.etiqueta}
        </button>
      )}
    </div>
  );
}
