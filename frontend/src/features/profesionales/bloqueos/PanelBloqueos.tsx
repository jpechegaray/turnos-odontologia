import { useId } from "react";
import { api } from "../../../shared/api/clienteApi";
import { formatearFecha, formatearHora } from "../../../shared/fecha/zonaHoraria";
import { Alerta, Boton, Toast } from "../../../shared/ui";
import { ConfirmarEliminacion } from "../ConfirmarEliminacion";
import { usePuedeGestionarAgenda } from "../permisos";
import type { PanelProfesionalProps } from "../profesionales/PaginaProfesionales";
import { useListadoPanel } from "../useListadoPanel";
import { errorDeBloqueo } from "./errores";
import { FormularioBloqueo } from "./FormularioBloqueo";
import type { Bloqueo, BloqueoSolicitud } from "./tipos";
import "../panel-agenda.css";

const fechaYHora = (instante: string) => `${formatearFecha(instante)} ${formatearHora(instante)}`;

/**
 * Bloqueos de agenda de un profesional en el orden de la API, con fechas en hora de Buenos Aires
 * (US-008). `ADMIN` y `RECEPCION` los crean y eliminan; `ODONTOLOGO` solo los ve (RN-33). El back no
 * permite modificarlos: se eliminan y se vuelven a crear.
 */
export function PanelBloqueos({ profesional, refrescarLista, informarError }: PanelProfesionalProps) {
  const puedeEditar = usePuedeGestionarAgenda();
  const idTitulo = useId();
  const rutaBloqueos = `/profesionales/${profesional.id}/bloqueos`;
  const { estado, cargar, escribir, guardando, aviso, error, version } = useListadoPanel<Bloqueo>({
    ruta: rutaBloqueos,
    traducirError: errorDeBloqueo,
    refrescarLista,
    informarError,
  });

  const crear = (cuerpo: BloqueoSolicitud) =>
    escribir(() => api.post<Bloqueo>(rutaBloqueos, cuerpo), "Bloqueo agregado.");

  const eliminar = (bloqueo: Bloqueo) =>
    escribir(() => api.delete(`${rutaBloqueos}/${bloqueo.id}`), "Bloqueo eliminado.");

  function contenido() {
    switch (estado.tipo) {
      case "cargando":
        return <p role="status">Cargando bloqueos…</p>;
      case "error":
        return (
          <Alerta tipo="peligro" titulo="No pudimos cargar los bloqueos del profesional.">
            <Boton variante="secundaria" onClick={() => void cargar()}>Reintentar</Boton>
          </Alerta>
        );
      case "listo":
        return (
          <>
            {estado.elementos.length === 0 ? (
              <p className="agenda-dia__vacio">El profesional no tiene bloqueos.</p>
            ) : (
              <ul className="agenda-lista agenda-lista--bloqueos" aria-label="Bloqueos">
                {estado.elementos.map((b) => (
                  <li key={b.id} className="agenda-item">
                    <span className="bloqueo__periodo">{fechaYHora(b.inicio)} al {fechaYHora(b.fin)}</span>
                    <span className="bloqueo__motivo">{b.motivo}</span>
                    {puedeEditar && <ConfirmarEliminacion deshabilitado={guardando} onConfirmar={() => eliminar(b)} />}
                  </li>
                ))}
              </ul>
            )}
            {puedeEditar && <FormularioBloqueo key={version} onEnviar={crear} deshabilitado={guardando} />}
          </>
        );
    }
  }

  return (
    <section className="panel panel-agenda" aria-labelledby={idTitulo}>
      <h3 className="panel__titulo" id={idTitulo}>
        Bloqueos de {profesional.apellido}, {profesional.nombre}
      </h3>
      {aviso && <Toast mensaje={aviso} />}
      {error && <Alerta tipo="peligro" titulo={error} />}
      {contenido()}
    </section>
  );
}
