import { useCallback, useEffect, useId, useRef, useState, type FormEvent } from "react";
import { api } from "../../../shared/api/clienteApi";
import { Alerta, Boton, Toast } from "../../../shared/ui";
import { useEsAdministrador } from "../permisos";
import type { PanelProfesionalProps } from "../profesionales/PaginaProfesionales";
import { errorDeReemplazo } from "./errores";
import { construirOpciones, idsSeleccionados, seleccionInicial } from "./opciones";
import type { PrestacionAsociable, ReemplazoPrestaciones } from "./tipos";
import "./panel.css";

type EstadoPanel =
  | Readonly<{ tipo: "cargando" }>
  | Readonly<{ tipo: "error" }>
  | Readonly<{
      tipo: "listo";
      asociadas: readonly PrestacionAsociable[];
      /** Solo para `ADMIN`: `GET /api/prestaciones?soloActivas=false`. */
      catalogo?: readonly PrestacionAsociable[];
    }>;

/**
 * Prestaciones de un profesional. Todos los roles ven la lista de asociadas; solo `ADMIN` ve las
 * casillas y reemplaza el conjunto con un único `PUT`. Se monta con `key` por id, así que el estado
 * se reinicia al cambiar de profesional. Un contador descarta las respuestas viejas.
 */
export function PanelPrestaciones({ profesional, refrescarLista, informarError }: PanelProfesionalProps) {
  const esAdmin = useEsAdministrador();
  const idTitulo = useId();
  const [estado, actualizarEstado] = useState<EstadoPanel>({ tipo: "cargando" });
  const [seleccion, actualizarSeleccion] = useState<ReadonlySet<number>>(new Set());
  const [guardando, actualizarGuardando] = useState(false);
  const [aviso, actualizarAviso] = useState<string>();
  const [error, actualizarError] = useState<string>();
  const solicitud = useRef(0);
  const pendiente = useRef(false);
  const rutaAsociadas = `/profesionales/${profesional.id}/prestaciones`;

  /** Con `conservarSeleccion`, se recargan los datos sin pisar lo que eligió el administrador. */
  const cargar = useCallback(
    async (conservarSeleccion = false) => {
      const actual = ++solicitud.current;
      if (!conservarSeleccion) actualizarEstado({ tipo: "cargando" });
      try {
        const [asociadas, catalogo] = await Promise.all([
          api.get<PrestacionAsociable[]>(rutaAsociadas),
          esAdmin ? api.get<PrestacionAsociable[]>("/prestaciones?soloActivas=false") : Promise.resolve(undefined),
        ]);
        if (actual !== solicitud.current) return;
        actualizarEstado({ tipo: "listo", asociadas, catalogo });
        if (!conservarSeleccion) actualizarSeleccion(seleccionInicial(asociadas));
      } catch {
        if (actual === solicitud.current) actualizarEstado({ tipo: "error" });
      }
    },
    [rutaAsociadas, esAdmin],
  );

  useEffect(() => {
    void cargar();
  }, [cargar]);

  async function guardar(opciones: readonly PrestacionAsociable[], catalogo?: readonly PrestacionAsociable[]) {
    if (pendiente.current) return;
    pendiente.current = true;
    actualizarGuardando(true);
    actualizarAviso(undefined);
    actualizarError(undefined);
    const actual = ++solicitud.current;
    const cuerpo: ReemplazoPrestaciones = { prestacionIds: idsSeleccionados(opciones, seleccion) };
    try {
      const asociadas = await api.put<PrestacionAsociable[]>(rutaAsociadas, cuerpo);
      if (actual === solicitud.current) {
        actualizarEstado({ tipo: "listo", asociadas, catalogo });
        actualizarSeleccion(seleccionInicial(asociadas));
      }
      actualizarAviso("Prestaciones actualizadas.");
    } catch (causa) {
      const { mensaje, reaccion } = errorDeReemplazo(causa);
      if (reaccion === "refrescarLista") {
        // La página muestra la alerta: si el profesional ya no está, este panel se desmonta.
        informarError(mensaje);
        void refrescarLista();
      } else {
        actualizarError(mensaje);
        if (reaccion === "recargar") void cargar(true);
      }
    } finally {
      pendiente.current = false;
      actualizarGuardando(false);
    }
  }

  function alternar(id: number) {
    const nueva = new Set(seleccion);
    if (!nueva.delete(id)) nueva.add(id);
    actualizarSeleccion(nueva);
  }

  function contenido() {
    switch (estado.tipo) {
      case "cargando":
        return <p role="status">Cargando prestaciones…</p>;
      case "error":
        return (
          <Alerta tipo="peligro" titulo="No pudimos cargar las prestaciones del profesional.">
            <Boton variante="secundaria" onClick={() => void cargar()}>Reintentar</Boton>
          </Alerta>
        );
      case "listo": {
        const { asociadas, catalogo } = estado;
        const opciones = esAdmin && catalogo ? construirOpciones(catalogo, asociadas) : undefined;
        return (
          <>
            <ListaAsociadas asociadas={asociadas} />
            {opciones && (
              <form
                noValidate
                onSubmit={(evento: FormEvent) => {
                  evento.preventDefault();
                  void guardar(opciones, catalogo);
                }}
              >
                <fieldset className="panel-prestaciones__opciones">
                  <legend>Prestaciones que realiza</legend>
                  {opciones.length === 0 && <p>No hay prestaciones activas para asociar.</p>}
                  {opciones.map((p) => (
                    <label key={p.id} className="panel-prestaciones__opcion">
                      <input type="checkbox" checked={seleccion.has(p.id)} onChange={() => alternar(p.id)} />
                      <span>{p.nombre} ({p.duracionDefaultMin} min)</span>
                      {!p.activa && <span className="panel-prestaciones__inactiva"> inactiva</span>}
                    </label>
                  ))}
                </fieldset>
                <div className="panel-prestaciones__acciones">
                  <Boton type="submit" disabled={guardando}>Guardar prestaciones</Boton>
                  <Boton
                    variante="secundaria"
                    disabled={guardando}
                    onClick={() => {
                      actualizarSeleccion(seleccionInicial(asociadas));
                      actualizarAviso(undefined);
                      actualizarError(undefined);
                    }}
                  >
                    Cancelar
                  </Boton>
                </div>
              </form>
            )}
          </>
        );
      }
    }
  }

  return (
    <section className="panel panel-prestaciones" aria-labelledby={idTitulo}>
      <h3 className="panel__titulo" id={idTitulo}>
        Prestaciones de {profesional.apellido}, {profesional.nombre}
      </h3>
      {aviso && <Toast mensaje={aviso} />}
      {error && <Alerta tipo="peligro" titulo={error} />}
      {contenido()}
    </section>
  );
}

function ListaAsociadas({ asociadas }: Readonly<{ asociadas: readonly PrestacionAsociable[] }>) {
  if (asociadas.length === 0) return <p>El profesional no tiene prestaciones asociadas.</p>;
  return (
    <ul className="panel-prestaciones__lista" aria-label="Prestaciones asociadas">
      {asociadas.map((p) => (
        <li key={p.id}>
          {p.nombre} — {p.duracionDefaultMin} min
          {!p.activa && <span className="panel-prestaciones__inactiva"> (inactiva)</span>}
        </li>
      ))}
    </ul>
  );
}
