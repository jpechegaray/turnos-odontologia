import { useState } from "react";
import { api } from "../../shared/api/clienteApi";
import { Alerta, Avatar, Boton, Tabla, Toast, type ColumnaTabla } from "../../shared/ui";
import { AccionesFilaPaciente } from "./AccionesFilaPaciente";
import { BusquedaPacientes } from "./BusquedaPacientes";
import { erroresDeApiPaciente } from "./errores";
import { FormularioPaciente } from "./FormularioPaciente";
import type { PacienteRespuesta, PacienteSolicitud } from "./tipos";
import { usePacientes } from "./usePacientes";
import "./pacientes.css";

const MARCA_VACIO = "—";

const COLUMNAS_DE_DATOS: readonly ColumnaTabla<PacienteRespuesta>[] = [
  {
    clave: "apellido",
    titulo: "Apellido",
    celda: (p) => (
      <span className="celda-persona">
        <Avatar nombre={`${p.nombre} ${p.apellido}`} neutro />
        <strong>{p.apellido}</strong>
      </span>
    ),
  },
  { clave: "nombre", titulo: "Nombre", celda: (p) => p.nombre },
  { clave: "dni", titulo: "DNI", celda: (p) => <span className="mono">{p.dni ?? MARCA_VACIO}</span> },
  { clave: "email", titulo: "Email", celda: (p) => p.email ?? MARCA_VACIO },
  { clave: "telefono", titulo: "Teléfono", celda: (p) => <span className="mono">{p.telefono ?? MARCA_VACIO}</span> },
  { clave: "obraSocial", titulo: "Obra social", celda: (p) => p.obraSocial ?? MARCA_VACIO },
];

function bajada(pacientes: readonly PacienteRespuesta[] | undefined): string {
  if (!pacientes) return "Fichas de pacientes del consultorio";
  return pacientes.length === 1 ? "1 paciente en la lista" : `${pacientes.length} pacientes en la lista`;
}

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
      acciones: true,
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
      <div className="encabezado">
        <div>
          <h2>Pacientes</h2>
          <p className="encabezado__bajada">{bajada(pacientes)}</p>
        </div>
      </div>
      {aviso ? <Toast mensaje={aviso} /> : null}
      {errorAccion ? <Alerta tipo="peligro" titulo={errorAccion} /> : null}
      <div className="pacientes">
      <section className="panel panel--plano pacientes__listado" aria-label="Listado de pacientes">
      <BusquedaPacientes onBuscar={(filtros) => void buscar(filtros)} />
      {cargando ? (
        <p role="status" className="pacientes__estado">Cargando…</p>
      ) : errorCarga ? (
        <div className="pacientes__estado">
          <Alerta tipo="peligro" titulo="No pudimos cargar los pacientes.">
            <Boton variante="secundaria" onClick={() => void recargar()}>Reintentar</Boton>
          </Alerta>
        </div>
      ) : (
        <Tabla
          titulo="Pacientes"
          tituloOculto
          columnas={columnas}
          filas={pacientes ?? []}
          claveFila={(p) => p.id}
          mensajeVacio="No se encontraron pacientes."
        />
      )}
      </section>
      <FormularioPaciente
        key={editando?.id ?? "nuevo"}
        valorInicial={editando}
        etiquetaEnvio={editando ? "Guardar cambios" : "Agregar paciente"}
        onEnviar={guardar}
        onCancelar={editando ? () => actualizarEditando(undefined) : undefined}
      />
      </div>
    </section>
  );
}
