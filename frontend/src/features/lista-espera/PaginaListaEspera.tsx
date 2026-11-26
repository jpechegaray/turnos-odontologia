import { useState } from "react";
import { FormularioAlta } from "./FormularioAlta";
import { ListadoEntradas } from "./ListadoEntradas";
import { SugerenciasHueco } from "./SugerenciasHueco";
import { useCatalogosListaEspera } from "./useCatalogosListaEspera";
import "./lista-espera.css";

/** Lista de espera para recepción y administración: anotar, ver, dar de baja y asignar un hueco (C-47). */
export function PaginaListaEspera() {
  const catalogos = useCatalogosListaEspera();
  // Cada alta o asignación vuelve a consultar el listado.
  const [version, cambiarVersion] = useState(0);
  const recargarListado = () => cambiarVersion((anterior) => anterior + 1);

  return (
    <section className="pagina-lista-espera">
      <h2>Lista de espera</h2>
      <FormularioAlta catalogos={catalogos} onAnotado={() => { catalogos.recargarPacientes(); recargarListado(); }} />
      <ListadoEntradas catalogos={catalogos} version={version} />
      <SugerenciasHueco catalogos={catalogos} onAsignado={recargarListado} />
    </section>
  );
}
