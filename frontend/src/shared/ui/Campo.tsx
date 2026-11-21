import { useId, type ComponentProps } from "react";
import { Icono } from "./Icono";
import "./ui.css";

export type CampoProps = Omit<ComponentProps<"input">, "id"> & {
  etiqueta: string;
  ayuda?: string;
  error?: string;
};

/** Campo de texto con etiqueta, ayuda y error conectados por aria (WCAG 1.3.1 y 3.3.1). */
export function Campo({ etiqueta, ayuda, error, className, ...props }: CampoProps) {
  const id = useId();
  const idAyuda = `${id}-ayuda`;
  const idError = `${id}-error`;
  const descripcion = [error && idError, ayuda && idAyuda].filter(Boolean).join(" ") || undefined;

  return (
    <div className="field">
      <label htmlFor={id}>{etiqueta}</label>
      <input
        id={id}
        className={["input", className].filter(Boolean).join(" ")}
        aria-invalid={error ? true : undefined}
        aria-describedby={descripcion}
        {...props}
      />
      {error && (
        <span id={idError} className="error">
          <Icono nombre="alerta" />
          {error}
        </span>
      )}
      {ayuda && (
        <span id={idAyuda} className="hint">
          {ayuda}
        </span>
      )}
    </div>
  );
}
