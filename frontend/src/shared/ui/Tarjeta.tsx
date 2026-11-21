import type { ReactNode } from "react";
import "./ui.css";

export type TarjetaProps = Readonly<{
  titulo?: string;
  children: ReactNode;
}>;

/** El título es un h3: la marca es el h1 del layout y el título de cada pantalla, el h2. */
export function Tarjeta({ titulo, children }: TarjetaProps) {
  return (
    <section className="panel">
      {titulo && <h3 className="panel__titulo">{titulo}</h3>}
      {children}
    </section>
  );
}
