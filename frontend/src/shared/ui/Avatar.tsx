import { claseColor, iniciales } from "./iniciales";
import "./ui.css";

type Props = Readonly<{
  nombre: string;
  /** Con un id, el color sale de la paleta de profesionales. */
  colorId?: number;
  tamano?: "sm" | "normal" | "lg";
  neutro?: boolean;
}>;

/** Círculo con iniciales; es decorativo porque el nombre siempre está escrito al lado. */
export function Avatar({ nombre, colorId, tamano = "normal", neutro = false }: Props) {
  const color = neutro ? "avatar--neutro" : colorId !== undefined ? claseColor(colorId) : "";
  const clases = ["avatar", color, tamano !== "normal" && `avatar--${tamano}`].filter(Boolean).join(" ");
  // Las iniciales se dibujan con CSS (::before) para que no se sumen al texto de la celda o del botón.
  return <span className={clases} aria-hidden="true" data-iniciales={iniciales(nombre)} />;
}
