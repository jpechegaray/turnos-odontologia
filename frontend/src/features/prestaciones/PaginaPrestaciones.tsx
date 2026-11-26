import { useState } from "react";
import { api } from "../../shared/api/clienteApi";
import { Alerta, Boton, Icono, Tabla, Toast, type ColumnaTabla } from "../../shared/ui";
import { AccionesFila } from "./AccionesFila";
import { mensajeDeError } from "./errores";
import { FormularioPrestacion } from "./FormularioPrestacion";
import { useEsAdministrador } from "./permisos";
import type { PrestacionRespuesta, PrestacionSolicitud } from "./tipos";
import { useCatalogoPrestaciones } from "./useCatalogoPrestaciones";

const COLUMNAS: readonly ColumnaTabla<PrestacionRespuesta>[] = [
  {
    clave: "nombre",
    titulo: "Nombre",
    celda: (p) => (
      <span className="celda-persona">
        <span className={p.activa ? "icono-caja" : "icono-caja icono-caja--inactivo"}><Icono nombre="prestaciones" /></span>
        <strong>{p.nombre}</strong>
      </span>
    ),
  },
  { clave: "duracion", titulo: "Duración (min)", numerica: true, celda: (p) => p.duracionDefaultMin },
  {
    clave: "estado",
    titulo: "Estado",
    celda: (p) => <span className={p.activa ? "estado" : "estado estado--inactivo"}>{p.activa ? "Activa" : "Inactiva"}</span>,
  },
];

export function PaginaPrestaciones() {
  const esAdmin = useEsAdministrador();
  const { prestaciones, cargando, errorCarga, recargar } = useCatalogoPrestaciones();
  const [aviso, actualizarAviso] = useState<string>();
  const [errorAccion, actualizarErrorAccion] = useState<string>();
  const [editando, actualizarEditando] = useState<PrestacionRespuesta>();
  const [confirmandoBaja, actualizarConfirmandoBaja] = useState<number>();

  async function crear(solicitud: PrestacionSolicitud) {
    await api.post<PrestacionRespuesta>("/prestaciones", solicitud);
    actualizarAviso("Prestación agregada.");
    await recargar();
  }

  async function modificar(prestacion: PrestacionRespuesta, solicitud: PrestacionSolicitud) {
    try {
      await api.put<PrestacionRespuesta>(`/prestaciones/${prestacion.id}`, {
        ...solicitud,
        activa: prestacion.activa,
      });
      actualizarEditando(undefined);
      actualizarAviso("Prestación actualizada.");
      await recargar();
    } catch (error) {
      await recargar();
      throw error;
    }
  }

  async function reactivar(prestacion: PrestacionRespuesta) {
    actualizarErrorAccion(undefined);
    try {
      await api.put<PrestacionRespuesta>(`/prestaciones/${prestacion.id}`, {
        nombre: prestacion.nombre,
        duracionDefaultMin: prestacion.duracionDefaultMin,
        activa: true,
      });
      actualizarAviso("Prestación reactivada.");
    } catch (error) {
      actualizarErrorAccion(mensajeDeError(error));
    } finally {
      await recargar();
    }
  }

  async function darDeBaja(prestacion: PrestacionRespuesta) {
    actualizarErrorAccion(undefined);
    try {
      await api.delete(`/prestaciones/${prestacion.id}`);
      actualizarAviso("Prestación dada de baja.");
    } catch (error) {
      actualizarErrorAccion(mensajeDeError(error));
    } finally {
      actualizarConfirmandoBaja(undefined);
      await recargar();
    }
  }

  const columnas: readonly ColumnaTabla<PrestacionRespuesta>[] = esAdmin
    ? [
        ...COLUMNAS,
        {
          clave: "acciones",
          titulo: "Acciones",
          acciones: true,
          celda: (p) => (
            <AccionesFila
              prestacion={p}
              confirmandoBaja={confirmandoBaja === p.id}
              onEditar={actualizarEditando}
              onReactivar={(prestacion) => void reactivar(prestacion)}
              onSolicitarBaja={(prestacion) => actualizarConfirmandoBaja(prestacion.id)}
              onConfirmarBaja={(prestacion) => void darDeBaja(prestacion)}
              onCancelarBaja={() => actualizarConfirmandoBaja(undefined)}
            />
          ),
        },
      ]
    : COLUMNAS;

  return (
    <section>
      <div className="encabezado">
        <div>
          <h2>Prestaciones</h2>
          <p className="encabezado__bajada">Tratamientos que ofrece el consultorio, con su duración por defecto.</p>
        </div>
      </div>
      {aviso && <Toast mensaje={aviso} />}
      {errorAccion && <Alerta tipo="peligro" titulo={errorAccion} />}
      <div className={esAdmin ? "con-formulario" : undefined}>
      <div className="con-formulario__lista">
      {cargando ? (
        <p role="status">Cargando…</p>
      ) : errorCarga ? (
        <Alerta tipo="peligro" titulo="No pudimos cargar las prestaciones.">
          <Boton variante="secundaria" onClick={() => void recargar()}>Reintentar</Boton>
        </Alerta>
      ) : (
        <Tabla
          titulo="Prestaciones"
          columnas={columnas}
          filas={prestaciones ?? []}
          claveFila={(p) => p.id}
          mensajeVacio="No hay prestaciones cargadas."
        />
      )}
      </div>
      {esAdmin && (
        <FormularioPrestacion
          key={editando?.id ?? "nueva"}
          valorInicial={editando}
          etiquetaEnvio={editando ? "Guardar cambios" : "Agregar prestación"}
          onEnviar={(solicitud) => (editando ? modificar(editando, solicitud) : crear(solicitud))}
          onCancelar={editando ? () => actualizarEditando(undefined) : undefined}
        />
      )}
      </div>
    </section>
  );
}
