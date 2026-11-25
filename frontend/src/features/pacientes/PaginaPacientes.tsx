import { useState } from "react";
import { api } from "../../shared/api/clienteApi";
import { Alerta, Boton, Tabla, Toast, type ColumnaTabla } from "../../shared/ui";
import { AccionesFilaPaciente } from "./AccionesFilaPaciente";
import { BusquedaPacientes } from "./BusquedaPacientes";
import { erroresDeApiPaciente } from "./errores";
import { FormularioPaciente } from "./FormularioPaciente";
import type { PacienteRespuesta, PacienteSolicitud } from "./tipos";
import { usePacientes } from "./usePacientes";

const MARCA_VACIO = "—";

const COLUMNAS_DE_DATOS: readonly ColumnaTabla<PacienteRespuesta>[] = [
  { clave: "apellido", titulo: "Apellido", celda: (p) => p.apellido },
  { clave: "nombre", titulo: "Nombre", celda: (p) => p.nombre },
  { clave: "dni", titulo: "DNI", celda: (p) => p.dni ?? MARCA_VACIO },
  { clave: "email", titulo: "Email", celda: (p) => p.email ?? MARCA_VACIO },
  { clave: "telefono", titulo: "Teléfono", celda: (p) => p.telefono ?? MARCA_VACIO },
  { clave: "obraSocial", titulo: "Obra social", celda: (p) => p.obraSocial ?? MARCA_VACIO },
];

export function PaginaPacientes() {
  const { pacientes, cargando, errorCarga, buscar, recargar } = usePacientes();
  const [aviso, actualizarAviso] = useState<string>();
  const [editando, actualizarEditando] = useState<PacienteRespuesta>();
  const [confirmandoBaja, actualizarConfirmandoBaja] = useState<number>();
  const [errorAccion, actualizarErrorAccion] = useState<string>();

  /** Alta o edición según haya una ficha en edición; en ambos casos la lista se vuelve a consultar. */
  async function guardar(solicitud: PacienteSolicitud) {
    try {
      if (editando) {
        await api.put<PacienteRespuesta>(`/pacientes/${editando.id}`, solicitud);
        actualizarEditando(undefined);
        actualizarAviso("Paciente actualizado.");
      } else {
        await api.post<PacienteRespuesta>("/pacientes", solicitud);
        actualizarAviso("Paciente agregado.");
      }
      await recargar();
    } catch (error) {
      // Una ficha editada pudo cambiar o desaparecer: se refresca para no mostrar datos viejos.
      if (editando) await recargar();
      throw error;
    }
  }

  async function darDeBaja(paciente: PacienteRespuesta) {
    actualizarErrorAccion(undefined);
    try {
      await api.delete(`/pacientes/${paciente.id}`);
      actualizarAviso("Paciente dado de baja.");
    } catch (error) {
      actualizarErrorAccion(erroresDeApiPaciente(error).alerta);
    } finally {
      actualizarConfirmandoBaja(undefined);
      await recargar();
    }
  }

  const columnas: readonly ColumnaTabla<PacienteRespuesta>[] = [
    ...COLUMNAS_DE_DATOS,
    {
      clave: "acciones",
      titulo: "Acciones",
      celda: (p) => (
        <AccionesFilaPaciente
          paciente={p}
          confirmandoBaja={confirmandoBaja === p.id}
          onEditar={actualizarEditando}
          onSolicitarBaja={(paciente) => actualizarConfirmandoBaja(paciente.id)}
          onConfirmarBaja={(paciente) => void darDeBaja(paciente)}
          onCancelarBaja={() => actualizarConfirmandoBaja(undefined)}
        />
      ),
    },
  ];

  return (
    <section>
      <h2>Pacientes</h2>
      <BusquedaPacientes onBuscar={(filtros) => void buscar(filtros)} />
      <FormularioPaciente
        key={editando?.id ?? "nuevo"}
        valorInicial={editando}
        etiquetaEnvio={editando ? "Guardar cambios" : "Agregar paciente"}
        onEnviar={guardar}
        onCancelar={editando ? () => actualizarEditando(undefined) : undefined}
      />
      {aviso ? <Toast mensaje={aviso} /> : null}
      {errorAccion ? <Alerta tipo="peligro" titulo={errorAccion} /> : null}
      {cargando ? (
        <p role="status">Cargando…</p>
      ) : errorCarga ? (
        <Alerta tipo="peligro" titulo="No pudimos cargar los pacientes.">
          <Boton variante="secundaria" onClick={() => void recargar()}>Reintentar</Boton>
        </Alerta>
      ) : (
        <Tabla
          titulo="Pacientes"
          columnas={columnas}
          filas={pacientes ?? []}
          claveFila={(p) => p.id}
          mensajeVacio="No se encontraron pacientes."
        />
      )}
    </section>
  );
}
