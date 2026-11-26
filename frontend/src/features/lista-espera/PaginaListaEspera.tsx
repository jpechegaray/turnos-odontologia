import { useState } from "react";
import { Alerta } from "../../shared/ui";
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
      <div className="encabezado">
        <div>
          <h2>Lista de espera</h2>
          <p className="encabezado__bajada">Pacientes que esperan un hueco, de la entrada más antigua a la más nueva.</p>
        </div>
      </div>
      <ListadoEntradas catalogos={catalogos} version={version} />
      <div className="lista-espera__columnas">
        <FormularioAlta catalogos={catalogos} onAnotado={() => { catalogos.recargarPacientes(); recargarListado(); }} />
        <SugerenciasHueco catalogos={catalogos} onAsignado={recargarListado} />
      </div>
      <Alerta tipo="info" titulo="Cómo funciona la lista">
        <span>Solo se puede anotar a pacientes con email, una vez por prestación mientras la entrada esté activa. Las entradas vencen solas al pasar la fecha de fin.</span>
      </Alerta>
    </section>
  );
}
