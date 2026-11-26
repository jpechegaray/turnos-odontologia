import { useEffect, useId, useRef, useState } from "react";
import { Alerta, Boton, Icono } from "../../shared/ui";
import { FormularioAlta } from "./FormularioAlta";
import { ListadoEntradas } from "./ListadoEntradas";
import { SugerenciasHueco } from "./SugerenciasHueco";
import { useCatalogosListaEspera } from "./useCatalogosListaEspera";
import "./lista-espera.css";

type AccionLista = "anotar" | "asignar";

/** Lista de espera para recepción y administración: anotar, ver, dar de baja y asignar un hueco (C-47). */
export function PaginaListaEspera() {
  const catalogos = useCatalogosListaEspera();
  // Cada alta o asignación vuelve a consultar el listado.
  const [version, cambiarVersion] = useState(0);
  const recargarListado = () => cambiarVersion((anterior) => anterior + 1);
  const [accion, cambiarAccion] = useState<AccionLista>();
  const id = useId();
  const panelAlta = useRef<HTMLDivElement>(null);
  const panelHueco = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (accion === "anotar") panelAlta.current?.focus();
    if (accion === "asignar") panelHueco.current?.focus();
  }, [accion]);

  function alternarAccion(siguiente: AccionLista) {
    cambiarAccion((actual) => actual === siguiente ? undefined : siguiente);
  }

  return (
    <section className="pagina-lista-espera">
      <div className="encabezado">
        <div>
          <h2>Lista de espera</h2>
          <p className="encabezado__bajada">Pacientes que esperan un hueco, de la entrada más antigua a la más nueva.</p>
        </div>
        <div className="encabezado__acciones">
          <Boton variante="secundaria" aria-expanded={accion === "anotar"} aria-controls={`${id}-alta`} onClick={() => alternarAccion("anotar")}>
            <Icono nombre="mas" />Anotar paciente
          </Boton>
          <Boton aria-expanded={accion === "asignar"} aria-controls={`${id}-hueco`} onClick={() => alternarAccion("asignar")}>
            Asignar hueco
          </Boton>
        </div>
      </div>
      <div id={`${id}-alta`} ref={panelAlta} tabIndex={-1} hidden={accion !== "anotar"}>
        <FormularioAlta catalogos={catalogos} onAnotado={() => { catalogos.recargarPacientes(); recargarListado(); }} />
      </div>
      <div id={`${id}-hueco`} ref={panelHueco} tabIndex={-1} hidden={accion !== "asignar"}>
        <SugerenciasHueco catalogos={catalogos} onAsignado={recargarListado} />
      </div>
      <ListadoEntradas catalogos={catalogos} version={version} />
      <Alerta tipo="info" titulo="Cómo funciona la lista">
        <span>Solo se puede anotar a pacientes con email, una vez por prestación mientras la entrada esté activa. Las entradas vencen solas al pasar la fecha de fin.</span>
      </Alerta>
    </section>
  );
}
