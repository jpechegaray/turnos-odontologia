import { Link } from "react-router-dom";

export function PaginaNoEncontrada() {
  return (
    <section>
      <h2>Página no encontrada</h2>
      <p>La dirección que abriste no existe en el sistema.</p>
      <Link to="/">Volver al inicio</Link>
    </section>
  );
}
