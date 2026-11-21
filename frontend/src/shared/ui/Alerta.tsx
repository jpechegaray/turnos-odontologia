import type { ReactNode } from "react";
import { Icono } from "./Icono";
import "./ui.css";

export type AlertaProps = Readonly<{
  tipo: "peligro" | "info";
  titulo: string;
  children?: ReactNode;
}>;

/** La de peligro se anuncia de inmediato (`role="alert"`); la informativa no interrumpe. */
export function Alerta({ tipo, titulo, children }: AlertaProps) {
  const peligro = tipo === "peligro";
  return (
    <div className={`alert ${peligro ? "alert--danger" : "alert--info"}`} role={peligro ? "alert" : undefined}>
      <Icono nombre={peligro ? "alerta" : "info"} />
      <div>
        <strong>{titulo}</strong>
        {children}
      </div>
    </div>
  );
}
