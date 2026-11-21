import type { Key, ReactNode } from "react";
import "./ui.css";

export type ColumnaTabla<F> = Readonly<{
  clave: string;
  titulo: string;
  /** Alinea a la derecha con cifras tabulares (horas, duraciones). */
  numerica?: boolean;
  celda: (fila: F) => ReactNode;
}>;

export type TablaProps<F> = Readonly<{
  titulo: string;
  columnas: readonly ColumnaTabla<F>[];
  filas: readonly F[];
  claveFila: (fila: F) => Key;
  mensajeVacio: string;
}>;

export function Tabla<F>({ titulo, columnas, filas, claveFila, mensajeVacio }: TablaProps<F>) {
  return (
    <div className="table-wrap">
      <table className="tabla">
        <caption>{titulo}</caption>
        <thead>
          <tr>
            {columnas.map((c) => (
              <th key={c.clave} scope="col" className={c.numerica ? "num" : undefined}>
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
                  <td key={c.clave} className={c.numerica ? "num" : undefined}>
                    {c.celda(fila)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
