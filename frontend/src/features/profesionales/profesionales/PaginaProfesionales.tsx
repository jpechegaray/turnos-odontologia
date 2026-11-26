import { useState, type ComponentType } from "react";
import { api } from "../../../shared/api/clienteApi";
import { Alerta, Avatar, Boton, Icono, Tabla, Toast, type ColumnaTabla } from "../../../shared/ui";
import { AccionesFila } from "../AccionesFila";
import { useEsAdministrador } from "../permisos";
import "../panel-agenda.css";
import { mensajeDeError } from "./errores";
import { FormularioProfesional } from "./FormularioProfesional";
import { cuerpoModificacion, type ProfesionalRespuesta, type ProfesionalSolicitud } from "./tipos";
import { useProfesionales, type EstadoProfesionales } from "./useProfesionales";

const MARCA_VACIO = "—";

const COLUMNAS: readonly ColumnaTabla<ProfesionalRespuesta>[] = [
  {
    clave: "nombre",
    titulo: "Apellido y nombre",
    celda: (p) => (
      <span className="celda-persona">
        <Avatar nombre={`${p.nombre} ${p.apellido}`} colorId={p.id} neutro={!p.activo} />
        <strong>{`${p.apellido}, ${p.nombre}`}</strong>
      </span>
    ),
  },
  { clave: "matricula", titulo: "Matrícula", celda: (p) => <span className="mono">{p.matricula}</span> },
  { clave: "especialidad", titulo: "Especialidad", celda: (p) => p.especialidad?.trim() || MARCA_VACIO },
  {
    clave: "estado",
    titulo: "Estado",
    celda: (p) => <span className={p.activo ? "estado" : "estado estado--inactivo"}>{p.activo ? "Activo" : "Inactivo"}</span>,
  },
];

/** Lo que recibe una pieza montada en esta página para el profesional elegido. */
export type PanelProfesionalProps = Readonly<{
  profesional: ProfesionalRespuesta;
  /** Vuelve a consultar `GET /api/profesionales` (p. ej. ante un `404` del profesional). */
  refrescarLista: () => Promise<void>;
  /** Muestra un error en la página (sobrevive a que la recarga cierre el panel). */
  informarError: (mensaje: string) => void;
}>;

/** Pieza que se abre desde una acción de fila con la etiqueta indicada. */
export type PanelDefinicion = Readonly<{ etiqueta: string; Componente: ComponentType<PanelProfesionalProps> }>;

export type PaginaProfesionalesProps = Readonly<{
  /**
   * Punto de extensión: cada panel suma una acción de fila con su `etiqueta`, en este orden. Elegirla
   * monta su `Componente` debajo de la tabla, de a uno por vez, con `key` por etiqueta e id. Sin
   * `paneles`, la página no ofrece esas acciones.
   */
  paneles?: readonly PanelDefinicion[];
}>;

/** Panel abierto: qué pieza y para qué profesional. */
type Seleccion = Readonly<{ etiqueta: string; profesional: ProfesionalRespuesta }>;

function resumen(profesionales: readonly ProfesionalRespuesta[]): string {
  const activos = profesionales.filter((p) => p.activo).length;
  const inactivos = profesionales.length - activos;
  const texto = `${activos} ${activos === 1 ? "activo" : "activos"}`;
  return inactivos > 0 ? `${texto} · ${inactivos} ${inactivos === 1 ? "inactivo" : "inactivos"}` : texto;
}

export function PaginaProfesionales({ paneles = [] }: PaginaProfesionalesProps = {}) {
  const esAdmin = useEsAdministrador();
  const { estado, recargar } = useProfesionales();
  const [seleccion, actualizarSeleccion] = useState<Seleccion>();
  const [aviso, actualizarAviso] = useState<string>();
  const [editando, actualizarEditando] = useState<ProfesionalRespuesta>();
  const [confirmandoBaja, actualizarConfirmandoBaja] = useState<number>();
  const [errorAccion, actualizarErrorAccion] = useState<string>();

  async function crear(solicitud: ProfesionalSolicitud) {
    await api.post<ProfesionalRespuesta>("/profesionales", solicitud);
    actualizarAviso("Profesional agregado.");
    await recargar();
  }

  async function modificar(profesional: ProfesionalRespuesta, solicitud: ProfesionalSolicitud) {
    try {
      await api.put<ProfesionalRespuesta>(`/profesionales/${profesional.id}`, cuerpoModificacion(profesional, solicitud));
    } catch (error) {
      // El formulario muestra la alerta y conserva lo ingresado; la lista se refresca igual.
      await recargar();
      throw error;
    }
    actualizarEditando(undefined);
    actualizarAviso("Profesional actualizado.");
    await recargar();
  }

  /** Baja lógica o reactivación: `PUT` con los datos actuales y `activo` explícito. */
  async function cambiarEstado(profesional: ProfesionalRespuesta, activo: boolean) {
    actualizarErrorAccion(undefined);
    try {
      await api.put<ProfesionalRespuesta>(`/profesionales/${profesional.id}`, cuerpoModificacion(profesional, { activo }));
      actualizarAviso(activo ? "Profesional reactivado." : "Profesional dado de baja.");
    } catch (error) {
      actualizarErrorAccion(mensajeDeError(error));
    } finally {
      actualizarConfirmandoBaja(undefined);
      await recargar();
    }
  }

  const accionesPanel = (p: ProfesionalRespuesta) =>
    paneles.length > 0 && (
      <>
        {paneles.map(({ etiqueta }) => (
          <Boton
            key={etiqueta}
            variante="secundaria"
            tamano="chico"
            aria-pressed={seleccion?.etiqueta === etiqueta && seleccion.profesional.id === p.id}
            onClick={() => {
              actualizarErrorAccion(undefined);
              actualizarSeleccion({ etiqueta, profesional: p });
            }}
          >
            {etiqueta}
          </Boton>
        ))}
      </>
    );

  // Roles de consulta: sin acciones de escritura; si hay `paneles`, solo las acciones que los abren
  // (visibles para los tres roles) en una columna que no se llama «Acciones».
  const columnasConsulta: readonly ColumnaTabla<ProfesionalRespuesta>[] = paneles.length === 0 ? COLUMNAS : [
    ...COLUMNAS,
    { clave: "detalle", titulo: "Detalle", acciones: true, celda: (p) => <div>{accionesPanel(p)}</div> },
  ];

  const columnas: readonly ColumnaTabla<ProfesionalRespuesta>[] = !esAdmin ? columnasConsulta : [
    ...COLUMNAS,
    {
      clave: "acciones",
      titulo: "Acciones",
      acciones: true,
      celda: (p) => (
        <AccionesFila
          elemento={p}
          confirmandoBaja={confirmandoBaja === p.id}
          onEditar={actualizarEditando}
          onReactivar={(profesional) => void cambiarEstado(profesional, true)}
          onSolicitarBaja={(profesional) => actualizarConfirmandoBaja(profesional.id)}
          onConfirmarBaja={(profesional) => void cambiarEstado(profesional, false)}
          onCancelarBaja={() => actualizarConfirmandoBaja(undefined)}
          extra={accionesPanel(p)}
        />
      ),
    },
  ];

  // Tras una recarga que ya no trae al elegido, el panel se cierra; mientras carga, sigue montado.
  const seleccionadoVigente = estado.tipo === "listo"
    ? estado.profesionales.find((p) => p.id === seleccion?.profesional.id)
    : seleccion?.profesional;
  const panelAbierto = paneles.find((p) => p.etiqueta === seleccion?.etiqueta);

  function contenido(actual: EstadoProfesionales) {
    switch (actual.tipo) {
      case "cargando":
        return <p role="status">Cargando…</p>;
      case "error":
        return (
          <Alerta tipo="peligro" titulo="No pudimos cargar los profesionales.">
            <Boton variante="secundaria" onClick={() => void recargar()}>Reintentar</Boton>
          </Alerta>
        );
      case "sinVinculo":
        return (
          <Alerta
            tipo="info"
            titulo="Tu usuario no tiene un profesional vinculado. Pedile al administrador que lo vincule."
          />
        );
      case "listo":
        return (
          <Tabla
            titulo="Profesionales"
            columnas={columnas}
            filas={actual.profesionales}
            claveFila={(p) => p.id}
            mensajeVacio="No hay profesionales cargados."
          />
        );
    }
  }

  return (
    <section>
      <div className="encabezado">
        <div>
          <h2>Profesionales</h2>
          {estado.tipo === "listo" && <p className="encabezado__bajada">{resumen(estado.profesionales)}</p>}
        </div>
      </div>
      {aviso && <Toast mensaje={aviso} />}
      {errorAccion && <Alerta tipo="peligro" titulo={errorAccion} />}
      {contenido(estado)}
      {panelAbierto && seleccionadoVigente && (
        <section className="panel panel--plano detalle-prof" aria-label={`Detalle de ${seleccionadoVigente.apellido}, ${seleccionadoVigente.nombre}`}>
          <div className="detalle-prof__cabeza">
            <Avatar nombre={`${seleccionadoVigente.nombre} ${seleccionadoVigente.apellido}`} colorId={seleccionadoVigente.id} tamano="lg" />
            <div className="detalle-prof__titulo">
              <strong>{`${seleccionadoVigente.apellido}, ${seleccionadoVigente.nombre}`}</strong>
              <span className="muted">
                {[seleccionadoVigente.especialidad?.trim(), seleccionadoVigente.matricula].filter(Boolean).join(" · ")}
              </span>
            </div>
            <button type="button" className="boton-icono" aria-label="Cerrar detalle" onClick={() => actualizarSeleccion(undefined)}>
              <Icono nombre="x" />
            </button>
          </div>
          <div className="pestanas" role="tablist" aria-label="Secciones del profesional">
            {paneles.map(({ etiqueta }) => (
              <button
                key={etiqueta}
                type="button"
                role="tab"
                aria-selected={etiqueta === panelAbierto.etiqueta}
                onClick={() => actualizarSeleccion({ etiqueta, profesional: seleccionadoVigente })}
              >
                {etiqueta}
              </button>
            ))}
          </div>
          <div role="tabpanel" aria-label={panelAbierto.etiqueta}>
            <panelAbierto.Componente
              key={`${panelAbierto.etiqueta}-${seleccionadoVigente.id}`}
              profesional={seleccionadoVigente}
              refrescarLista={recargar}
              informarError={actualizarErrorAccion}
            />
          </div>
        </section>
      )}
      {esAdmin && (
        <FormularioProfesional
          key={editando?.id ?? "nuevo"}
          valorInicial={editando}
          onEnviar={(solicitud) => (editando ? modificar(editando, solicitud) : crear(solicitud))}
          onCancelar={editando ? () => actualizarEditando(undefined) : undefined}
        />
      )}
    </section>
  );
}
