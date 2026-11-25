import { useId, type ComponentProps } from "react";
import { Icono } from "./Icono";
import "./ui.css";

export type OpcionSeleccion = Readonly<{ valor: string; etiqueta: string }>;

export type CampoSeleccionProps = Omit<ComponentProps<"select">, "id" | "children"> & {
  etiqueta: string;
  opciones: readonly OpcionSeleccion[];
  /** Texto de la opción vacía inicial; sin él no se ofrece una opción vacía. */
  vacia?: string;
  error?: string;
};

/**
 * Selector con etiqueta y error conectados por aria, con las mismas clases `field` e `input` del kit
 * UI (`Campo`), así hereda los tokens sin valores fijos.
 */
export function CampoSeleccion({ etiqueta, opciones, vacia, error, className, ...props }: CampoSeleccionProps) {
  const id = useId();
  const idError = `${id}-error`;

  return (
    <div className="field">
      <label htmlFor={id}>{etiqueta}</label>
      <select
        id={id}
        className={["input", className].filter(Boolean).join(" ")}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? idError : undefined}
        {...props}
      >
        {vacia !== undefined && <option value="">{vacia}</option>}
        {opciones.map((opcion) => (
          <option key={opcion.valor} value={opcion.valor}>{opcion.etiqueta}</option>
        ))}
      </select>
      {error && (
        <span id={idError} className="error">
          <Icono nombre="alerta" />
          {error}
        </span>
      )}
    </div>
  );
}
