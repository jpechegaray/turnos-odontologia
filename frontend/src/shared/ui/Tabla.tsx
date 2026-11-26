import type { Key, ReactNode } from "react";
import "./ui.css";

export type ColumnaTabla<F> = Readonly<{
  clave: string;
  titulo: string;
  /** Alinea a la derecha con cifras tabulares (horas, duraciones). */
  numerica?: boolean;
  /** Columna de botones: se alinea a la derecha. */
  acciones?: boolean;
  celda: (fila: F) => ReactNode;
}>;

export type TablaProps<F> = Readonly<{
  titulo: string;
  columnas: readonly ColumnaTabla<F>[];
  filas: readonly F[];
  claveFila: (fila: F) => Key;
  mensajeVacio: string;
  /** El título queda solo para lectores de pantalla (la tabla ya está dentro de un panel con título). */
  tituloOculto?: boolean;
  /** Pie de la tabla: cantidad, paginación. */
  pie?: ReactNode;
}>;

function claseColumna<F>(c: ColumnaTabla<F>): string | undefined {
  return [c.numerica && "num", c.acciones && "tabla__acciones"].filter(Boolean).join(" ") || undefined;
}

export function Tabla<F>({ titulo, columnas, filas, claveFila, mensajeVacio, tituloOculto = false, pie }: TablaProps<F>) {
  return (
    <div className="table-wrap">
      <table className="tabla">
        <caption className={tituloOculto ? "sr-only" : undefined}>{titulo}</caption>
        <thead>
          <tr>
            {columnas.map((c) => (
              <th key={c.clave} scope="col" className={claseColumna(c)}>
                {c.titulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.length === 0 ? (
            <tr>
              <td colSpan={columnas.length} className="tabla__vacio">
                {mensajeVacio}
              </td>
            </tr>
          ) : (
            filas.map((fila) => (
              <tr key={claveFila(fila)}>
                {columnas.map((c) => (
                  <td key={c.clave} className={claseColumna(c)}>
                    {c.celda(fila)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
      {pie ? <div className="panel__pie">{pie}</div> : null}
    </div>
  );
}
