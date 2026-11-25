import { useState } from "react";
import { api } from "../../../shared/api/clienteApi";
import { Alerta, Boton, Tabla, Toast, type ColumnaTabla } from "../../../shared/ui";
import { AccionesFila } from "../AccionesFila";
import { useEsAdministrador } from "../permisos";
import { mensajeDeErrorSillon } from "./errores";
import { FormularioSillon } from "./FormularioSillon";
import type { SillonModificacion, SillonRespuesta, SillonSolicitud } from "./tipos";
import { useSillones } from "./useSillones";

const MARCA_VACIO = "—";

const COLUMNAS: readonly ColumnaTabla<SillonRespuesta>[] = [
  { clave: "nombre", titulo: "Nombre", celda: (s) => s.nombre },
  { clave: "descripcion", titulo: "Descripción", celda: (s) => s.descripcion ?? MARCA_VACIO },
  { clave: "estado", titulo: "Estado", celda: (s) => (s.activo ? "Activo" : "Inactivo") },
];

/** Cuerpo del `PUT` a partir de la representación actual, con el `activo` pedido. */
function conEstado(sillon: SillonRespuesta, activo: boolean): SillonModificacion {
  return { nombre: sillon.nombre, descripcion: sillon.descripcion, activo };
}

export function PaginaSillones() {
  const esAdmin = useEsAdministrador();
  const { sillones, cargando, errorCarga, recargar } = useSillones();
  const [aviso, actualizarAviso] = useState<string>();
  const [editando, actualizarEditando] = useState<SillonRespuesta>();
  const [confirmandoBaja, actualizarConfirmandoBaja] = useState<number>();
  const [errorAccion, actualizarErrorAccion] = useState<string>();

  async function crear(solicitud: SillonSolicitud) {
    await api.post<SillonRespuesta>("/sillones", solicitud);
    actualizarAviso("Sillón agregado.");
    await recargar();
  }

  async function modificar(sillon: SillonRespuesta, solicitud: SillonSolicitud) {
    try {
      await api.put<SillonRespuesta>(`/sillones/${sillon.id}`, { ...solicitud, activo: sillon.activo });
      actualizarEditando(undefined);
      actualizarAviso("Sillón actualizado.");
      await recargar();
    } catch (error) {
      await recargar();
      throw error;
    }
  }

  async function cambiarEstado(sillon: SillonRespuesta, activo: boolean) {
    actualizarErrorAccion(undefined);
    try {
      await api.put<SillonRespuesta>(`/sillones/${sillon.id}`, conEstado(sillon, activo));
      actualizarAviso(activo ? "Sillón reactivado." : "Sillón dado de baja.");
    } catch (error) {
      actualizarErrorAccion(mensajeDeErrorSillon(error));
    } finally {
      actualizarConfirmandoBaja(undefined);
      await recargar();
    }
  }

  const columnas: readonly ColumnaTabla<SillonRespuesta>[] = esAdmin
    ? [
        ...COLUMNAS,
        {
          clave: "acciones",
          titulo: "Acciones",
          celda: (s) => (
            <AccionesFila
              elemento={s}
              confirmandoBaja={confirmandoBaja === s.id}
              onEditar={actualizarEditando}
              onReactivar={(sillon) => void cambiarEstado(sillon, true)}
              onSolicitarBaja={(sillon) => actualizarConfirmandoBaja(sillon.id)}
              onConfirmarBaja={(sillon) => void cambiarEstado(sillon, false)}
              onCancelarBaja={() => actualizarConfirmandoBaja(undefined)}
            />
          ),
        },
      ]
    : COLUMNAS;

  return (
    <section>
      <h2>Sillones</h2>
      {esAdmin && (
        <FormularioSillon
          key={editando?.id ?? "nuevo"}
          valorInicial={editando}
          etiquetaEnvio={editando ? "Guardar cambios" : "Agregar sillón"}
          onEnviar={(solicitud) => (editando ? modificar(editando, solicitud) : crear(solicitud))}
          onCancelar={editando ? () => actualizarEditando(undefined) : undefined}
        />
      )}
      {aviso && <Toast mensaje={aviso} />}
      {errorAccion && <Alerta tipo="peligro" titulo={errorAccion} />}
      {cargando ? (
        <p role="status">Cargando…</p>
      ) : errorCarga ? (
        <Alerta tipo="peligro" titulo="No pudimos cargar los sillones.">
          <Boton variante="secundaria" onClick={() => void recargar()}>Reintentar</Boton>
        </Alerta>
      ) : (
        <Tabla
          titulo="Sillones"
          columnas={columnas}
          filas={sillones ?? []}
          claveFila={(s) => s.id}
          mensajeVacio="No hay sillones cargados."
        />
      )}
    </section>
  );
}
