import { useState, type ComponentType } from "react";
import { useSearchParams } from "react-router-dom";
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
  const [parametros] = useSearchParams();
  const esAdmin = useEsAdministrador();
  const { estado, recargar } = useProfesionales();
  const explorador = paneles.length > 0;
  const [alta, actualizarAlta] = useState(false);
  const [seleccion, actualizarSeleccion] = useState<Seleccion>();
  const [aviso, actualizarAviso] = useState<string>();
  const [editando, actualizarEditando] = useState<ProfesionalRespuesta>();
  const [confirmandoBaja, actualizarConfirmandoBaja] = useState<number>();
  const [errorAccion, actualizarErrorAccion] = useState<string>();

  async function crear(solicitud: ProfesionalSolicitud) {
    await api.post<ProfesionalRespuesta>("/profesionales", solicitud);
    actualizarAviso("Profesional agregado.");
    actualizarAlta(false);
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

  const columnas: readonly ColumnaTabla<ProfesionalRespuesta>[] = !esAdmin ? COLUMNAS : [
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
        />
      ),
    },
  ];

  const predeterminada = parametros.get("seccion") === "bloqueos" ? "Bloqueos" : "Horarios";
  const primeraPestana = paneles.find(({ etiqueta }) => etiqueta === predeterminada)?.etiqueta ?? paneles[0]?.etiqueta ?? "Datos";
  const seleccionadoVigente = estado.tipo === "listo"
    ? estado.profesionales.find((p) => p.id === seleccion?.profesional.id)
      ?? estado.profesionales.find((p) => p.activo) ?? estado.profesionales[0]
    : seleccion?.profesional;
  const pestanaActiva = seleccion?.etiqueta ?? primeraPestana;
  const panelAbierto = paneles.find((p) => p.etiqueta === pestanaActiva);

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
        if (explorador) {
          return (
            <section className="panel panel--plano explorador-prof__lista" aria-label="Listado de profesionales">
              {actual.profesionales.length ? <ul>{actual.profesionales.map((profesional) => (
                <li key={profesional.id}>
                  <button type="button" className="explorador-prof__enlace"
                    aria-current={profesional.id === seleccionadoVigente?.id ? "true" : undefined}
                    onClick={() => {
                      actualizarConfirmandoBaja(undefined);
                      actualizarErrorAccion(undefined);
                      actualizarSeleccion({ etiqueta: pestanaActiva, profesional });
                    }}>
                    <Avatar nombre={`${profesional.nombre} ${profesional.apellido}`} colorId={profesional.id} neutro={!profesional.activo} />
                    <span><strong>{profesional.nombre} {profesional.apellido}</strong>
                      <small>{profesional.activo
                        ? [profesional.especialidad, profesional.matricula].filter(Boolean).join(" · ")
                        : "Inactivo"}</small>
                    </span>
                  </button>
                </li>
              ))}</ul> : <p>No hay profesionales cargados.</p>}
            </section>
          );
        }
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
        {explorador && esAdmin && <Boton variante="secundaria" onClick={() => {
          actualizarEditando(undefined);
          actualizarAlta(true);
        }}><Icono nombre="mas" />Agregar profesional</Boton>}
      </div>
      {aviso && <Toast mensaje={aviso} />}
      {errorAccion && <Alerta tipo="peligro" titulo={errorAccion} />}
      <div className={explorador ? "explorador-prof" : undefined}>
      {contenido(estado)}
      {explorador && seleccionadoVigente && (
        <section className="panel panel--plano detalle-prof" aria-label={`Detalle de ${seleccionadoVigente.apellido}, ${seleccionadoVigente.nombre}`}>
          <div className="detalle-prof__cabeza">
            <Avatar nombre={`${seleccionadoVigente.nombre} ${seleccionadoVigente.apellido}`} colorId={seleccionadoVigente.id} tamano="lg" />
            <div className="detalle-prof__titulo">
              <h3>{seleccionadoVigente.nombre} {seleccionadoVigente.apellido}</h3>
              <span className="muted">
                {[seleccionadoVigente.especialidad?.trim(), seleccionadoVigente.matricula].filter(Boolean).join(" · ")}
              </span>
            </div>
            {esAdmin && <Boton variante="secundaria" tamano="chico" onClick={() => actualizarEditando(seleccionadoVigente)}>
              <Icono nombre="editar" />Editar datos
            </Boton>}
          </div>
          <div className="pestanas" role="tablist" aria-label="Secciones del profesional">
            {["Datos", ...["Horarios", "Bloqueos", "Prestaciones"].filter((etiqueta) => paneles.some((p) => p.etiqueta === etiqueta)),
              ...paneles.filter(({ etiqueta }) => !["Horarios", "Bloqueos", "Prestaciones"].includes(etiqueta)).map(({ etiqueta }) => etiqueta)]
              .map((etiqueta) => (
              <button
                key={etiqueta}
                type="button"
                role="tab"
                aria-selected={etiqueta === pestanaActiva}
                onClick={() => actualizarSeleccion({ etiqueta, profesional: seleccionadoVigente })}
              >
                {etiqueta}
              </button>
            ))}
          </div>
          <div role="tabpanel" aria-label={pestanaActiva}>
            {panelAbierto ? <panelAbierto.Componente
              key={`${panelAbierto.etiqueta}-${seleccionadoVigente.id}`}
              profesional={seleccionadoVigente}
              refrescarLista={recargar}
              informarError={actualizarErrorAccion}
            /> : (
              <div className="explorador-prof__datos">
                <p>Matrícula: <strong>{seleccionadoVigente.matricula}</strong></p>
                <p>Especialidad: <strong>{seleccionadoVigente.especialidad || MARCA_VACIO}</strong></p>
                <p>Estado: <strong>{seleccionadoVigente.activo ? "Activo" : "Inactivo"}</strong></p>
                {esAdmin && <AccionesFila elemento={seleccionadoVigente} confirmandoBaja={confirmandoBaja === seleccionadoVigente.id}
                  onEditar={actualizarEditando} onReactivar={(p) => void cambiarEstado(p, true)}
                  onSolicitarBaja={(p) => actualizarConfirmandoBaja(p.id)} onConfirmarBaja={(p) => void cambiarEstado(p, false)}
                  onCancelarBaja={() => actualizarConfirmandoBaja(undefined)} />}
              </div>
            )}
          </div>
        </section>
      )}
      </div>
      {esAdmin && (!explorador || alta || editando) && (
        <FormularioProfesional
          key={editando?.id ?? "nuevo"}
          valorInicial={editando}
          onEnviar={(solicitud) => (editando ? modificar(editando, solicitud) : crear(solicitud))}
          onCancelar={editando || explorador ? () => { actualizarEditando(undefined); actualizarAlta(false); } : undefined}
        />
      )}
    </section>
  );
}
