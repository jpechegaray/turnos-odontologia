import { useId, type ReactNode } from "react";

/**
 * Panel con cabecera; su título es el nombre accesible, así cada sección es una región navegable.
 * Con `plana`, el contenido (una tabla) ocupa el panel de borde a borde.
 */
export function Seccion({ titulo, extra, plana = false, children }: Readonly<{
  titulo: string;
  extra?: ReactNode;
  plana?: boolean;
  children: ReactNode;
}>) {
  const id = useId();
  return (
    <section className="panel panel--plano lista-espera__seccion" aria-labelledby={id}>
      <div className="panel__cabecera">
        <h3 id={id}>{titulo}</h3>
        {extra}
      </div>
      <div className={plana ? "lista-espera__plana" : "panel__cuerpo lista-espera__cuerpo"}>{children}</div>
    </section>
  );
}
