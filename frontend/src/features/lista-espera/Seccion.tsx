import { useId, type ReactNode } from "react";

/** Panel del kit con su título como nombre accesible: cada sección es una región navegable. */
export function Seccion({ titulo, children }: Readonly<{ titulo: string; children: ReactNode }>) {
  const id = useId();
  return (
    <section className="panel lista-espera__seccion" aria-labelledby={id}>
      <h3 id={id} className="panel__titulo">{titulo}</h3>
      {children}
    </section>
  );
}
