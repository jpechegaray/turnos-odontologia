import type { ComponentProps } from "react";
import "./ui.css";

export type VarianteBoton = "primaria" | "secundaria" | "peligro" | "peligro-atenuado";

const CLASE_VARIANTE: Readonly<Record<VarianteBoton, string>> = {
  primaria: "btn--primary",
  secundaria: "btn--secondary",
  peligro: "btn--danger",
  "peligro-atenuado": "btn--danger-ghost",
};

export type BotonProps = ComponentProps<"button"> & {
  variante?: VarianteBoton;
  tamano?: "normal" | "chico";
  anchoCompleto?: boolean;
};

/** `type="button"` por defecto para no enviar formularios por accidente. */
export function Boton({ variante = "primaria", tamano = "normal", anchoCompleto = false, className, type = "button", ...props }: BotonProps) {
  const clases = ["btn", CLASE_VARIANTE[variante], tamano === "chico" && "btn--sm", anchoCompleto && "btn--block", className]
    .filter(Boolean)
    .join(" ");
  return <button type={type} className={clases} {...props} />;
}
