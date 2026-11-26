import type { ReactNode } from "react";
import "./ui.css";

export type TarjetaProps = Readonly<{
  titulo?: string;
  /** Texto o controles a la derecha del título. */
  extra?: ReactNode;
  className?: string;
  children: ReactNode;
}>;

/** El título es un h3: la marca es el h1 del layout y el título de cada pantalla, el h2. */
export function Tarjeta({ titulo, extra, className, children }: TarjetaProps) {
  return (
    <section className={["panel panel--plano", className].filter(Boolean).join(" ")}>
      {titulo && (
        <div className="panel__cabecera">
          <h3>{titulo}</h3>
          {extra}
        </div>
      )}
      <div className="panel__cuerpo">{children}</div>
    </section>
  );
}
