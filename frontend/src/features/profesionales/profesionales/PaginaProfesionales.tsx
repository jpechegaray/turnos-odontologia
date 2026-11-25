import { useState, type ComponentType } from "react";
import { api } from "../../../shared/api/clienteApi";
import { Alerta, Boton, Tabla, Toast, type ColumnaTabla } from "../../../shared/ui";
import { AccionesFila } from "../AccionesFila";
import { useEsAdministrador } from "../permisos";
import { mensajeDeError } from "./errores";
import { FormularioProfesional } from "./FormularioProfesional";
import { cuerpoModificacion, type ProfesionalRespuesta, type ProfesionalSolicitud } from "./tipos";
import { useProfesionales, type EstadoProfesionales } from "./useProfesionales";

const MARCA_VACIO = "—";

const COLUMNAS: readonly ColumnaTabla<ProfesionalRespuesta>[] = [
  { clave: "nombre", titulo: "Apellido y nombre", celda: (p) => `${p.apellido}, ${p.nombre}` },
  { clave: "matricula", titulo: "Matrícula", celda: (p) => p.matricula },
  { clave: "especialidad", titulo: "Especialidad", celda: (p) => p.especialidad?.trim() || MARCA_VACIO },
  { clave: "estado", titulo: "Estado", celda: (p) => (p.activo ? "Activo" : "Inactivo") },
];

/** Lo que recibe una pieza montada en esta página para el profesional elegido. */
export type PanelProfesionalProps = Readonly<{
  profesional: ProfesionalRespuesta;
  /** Vuelve a consultar `GET /api/profesionales` (p. ej. ante un `404` del profesional). */
  refrescarLista: () => Promise<void>;
  /** Muestra un error en la página (sobrevive a que la recarga cierre el panel). */
  informarError: (mensaje: string) => void;
}>;

export type PaginaProfesionalesProps = Readonly<{
  /**
   * Punto de extensión: la acción de fila `etiqueta` elige un profesional y monta `Componente`
   * debajo de la tabla con `key` por id. Sin `panel`, la página no ofrece esa acción.
   */
  panel?: Readonly<{ etiqueta: string; Componente: ComponentType<PanelProfesionalProps> }>;
}>;

export function PaginaProfesionales({ panel }: PaginaProfesionalesProps = {}) {
  const esAdmin = useEsAdministrador();
  const { estado, recargar } = useProfesionales();
  const [seleccionado, actualizarSeleccionado] = useState<ProfesionalRespuesta>();
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

  const accionPanel = (p: ProfesionalRespuesta) =>
    panel && (
      <Boton variante="secundaria" tamano="chico" onClick={() => {
        actualizarErrorAccion(undefined);
        actualizarSeleccionado(p);
      }}>
        {panel.etiqueta}
      </Boton>
    );

  // Roles de consulta: sin acciones de escritura; si hay `panel`, solo la acción que lo abre
  // (visible para los tres roles) en una columna que no se llama «Acciones».
  const columnasConsulta: readonly ColumnaTabla<ProfesionalRespuesta>[] = !panel ? COLUMNAS : [
    ...COLUMNAS,
    { clave: "detalle", titulo: "Detalle", celda: (p) => accionPanel(p) },
  ];

  const columnas: readonly ColumnaTabla<ProfesionalRespuesta>[] = !esAdmin ? columnasConsulta : [
    ...COLUMNAS,
    {
      clave: "acciones",
      titulo: "Acciones",
      celda: (p) => (
        <AccionesFila
          elemento={p}
          confirmandoBaja={confirmandoBaja === p.id}
          onEditar={actualizarEditando}
          onReactivar={(profesional) => void cambiarEstado(profesional, true)}
          onSolicitarBaja={(profesional) => actualizarConfirmandoBaja(profesional.id)}
          onConfirmarBaja={(profesional) => void cambiarEstado(profesional, false)}
          onCancelarBaja={() => actualizarConfirmandoBaja(undefined)}
          extra={accionPanel(p)}
        />
      ),
    },
  ];

  // Tras una recarga que ya no trae al elegido, el panel se cierra; mientras carga, sigue montado.
  const seleccionadoVigente =
    estado.tipo === "listo" ? estado.profesionales.find((p) => p.id === seleccionado?.id) : seleccionado;

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
      <h2>Profesionales</h2>
      {esAdmin && (
        <FormularioProfesional
          key={editando?.id ?? "nuevo"}
          valorInicial={editando}
          onEnviar={(solicitud) => (editando ? modificar(editando, solicitud) : crear(solicitud))}
          onCancelar={editando ? () => actualizarEditando(undefined) : undefined}
        />
      )}
      {aviso && <Toast mensaje={aviso} />}
      {errorAccion && <Alerta tipo="peligro" titulo={errorAccion} />}
      {contenido(estado)}
      {panel && seleccionadoVigente && (
        <panel.Componente
          key={seleccionadoVigente.id}
          profesional={seleccionadoVigente}
          refrescarLista={recargar}
          informarError={actualizarErrorAccion}
        />
      )}
    </section>
  );
}
