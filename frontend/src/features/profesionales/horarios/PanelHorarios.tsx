import { useId, useState } from "react";
import { api } from "../../../shared/api/clienteApi";
import { Alerta, Boton, Toast } from "../../../shared/ui";
import { ConfirmarEliminacion } from "../ConfirmarEliminacion";
import { usePuedeGestionarAgenda } from "../permisos";
import type { PanelProfesionalProps } from "../profesionales/PaginaProfesionales";
import { useListadoPanel } from "../useListadoPanel";
import { agruparPorDia } from "./dias";
import { errorDeTramo } from "./errores";
import { FormularioTramo } from "./FormularioTramo";
import type { TramoHorario, TramoSolicitud } from "./tipos";
import "../panel-agenda.css";

/**
 * Horarios de atención de un profesional agrupados por día (US-007). `ADMIN` y `RECEPCION` agregan,
 * modifican y eliminan tramos; `ODONTOLOGO` solo los ve (RN-33). Se monta con `key` por panel e id,
 * así que el estado se reinicia al cambiar de profesional.
 */
export function PanelHorarios({ profesional, refrescarLista, informarError }: PanelProfesionalProps) {
  const puedeEditar = usePuedeGestionarAgenda();
  const idTitulo = useId();
  const rutaHorarios = `/profesionales/${profesional.id}/horarios`;
  const { estado, cargar, escribir, guardando, aviso, error, version, limpiarMensajes } = useListadoPanel<TramoHorario>({
    ruta: rutaHorarios,
    traducirError: errorDeTramo,
    refrescarLista,
    informarError,
  });
  const [editando, actualizarEditando] = useState<TramoHorario>();

  async function guardar(cuerpo: TramoSolicitud) {
    if (editando) {
      await escribir(() => api.put<TramoHorario>(`${rutaHorarios}/${editando.id}`, cuerpo), "Tramo actualizado.");
    } else {
      await escribir(() => api.post<TramoHorario>(rutaHorarios, cuerpo), "Tramo agregado.");
    }
    actualizarEditando(undefined);
  }

  async function eliminar(tramo: TramoHorario) {
    await escribir(() => api.delete(`${rutaHorarios}/${tramo.id}`), "Tramo eliminado.");
    if (editando?.id === tramo.id) actualizarEditando(undefined);
  }

  function contenido() {
    switch (estado.tipo) {
      case "cargando":
        return <p role="status">Cargando horarios…</p>;
      case "error":
        return (
          <Alerta tipo="peligro" titulo="No pudimos cargar los horarios del profesional.">
            <Boton variante="secundaria" onClick={() => void cargar()}>Reintentar</Boton>
          </Alerta>
        );
      case "listo":
        return (
          <>
            <div className="agenda-dias">
              {agruparPorDia(estado.elementos).map(({ dia, nombre, tramos }) => (
                <div key={dia} role="group" aria-label={nombre} className="agenda-dia">
                  <span className="agenda-dia__nombre">{nombre}</span>
                  {tramos.length === 0 ? (
                    <span className="agenda-dia__vacio">No atiende</span>
                  ) : (
                    <ul className="agenda-lista">
                      {tramos.map((t) => (
                        <li key={t.id} className="agenda-item">
                          <span className="tramo__horas">{t.horaInicio} a {t.horaFin}</span>
                          {puedeEditar && (
                            <ConfirmarEliminacion deshabilitado={guardando} onConfirmar={() => eliminar(t)}>
                              <Boton
                                variante="secundaria"
                                tamano="chico"
                                onClick={() => {
                                  limpiarMensajes();
                                  actualizarEditando(t);
                                }}
                              >
                                Editar
                              </Boton>
                            </ConfirmarEliminacion>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              ))}
            </div>
            {puedeEditar && (
              <FormularioTramo
                key={editando ? `editar-${editando.id}` : `nuevo-${version}`}
                valorInicial={editando}
                existentes={estado.elementos}
                onEnviar={guardar}
                onCancelar={editando ? () => actualizarEditando(undefined) : undefined}
                deshabilitado={guardando}
              />
            )}
          </>
        );
    }
  }

  return (
    <section className="panel panel-agenda" aria-labelledby={idTitulo}>
      <h3 className="panel__titulo" id={idTitulo}>
        Horarios de {profesional.apellido}, {profesional.nombre}
      </h3>
      {aviso && <Toast mensaje={aviso} />}
      {error && <Alerta tipo="peligro" titulo={error} />}
      {contenido()}
    </section>
  );
}
