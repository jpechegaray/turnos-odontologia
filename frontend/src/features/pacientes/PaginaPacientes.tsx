import { useEffect, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import { api } from "../../shared/api/clienteApi";
import { Alerta, Avatar, Boton, Campo, Icono, Tabla, Toast, type ColumnaTabla } from "../../shared/ui";
import { fechaLocal, formatearFecha, formatearHora } from "../../shared/fecha/zonaHoraria";
import { useAgenda } from "../agenda/useAgenda";
import { sumarDias } from "../agenda/grilla";
import type { TurnoAgenda } from "../agenda/tipos";
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
    titulo: "Paciente",
    celda: (p) => (
      <span className="celda-persona">
        <Avatar nombre={`${p.nombre} ${p.apellido}`} neutro />
        <strong>{p.apellido}, {p.nombre}</strong>
      </span>
    ),
  },
  { clave: "dni", titulo: "DNI", celda: (p) => <span className="mono">{p.dni ?? MARCA_VACIO}</span> },
  { clave: "contacto", titulo: "Contacto", celda: (p) => (
    <span className="pacientes__contacto">
      <span className="mono">{p.telefono ?? MARCA_VACIO}</span><small>{p.email ?? MARCA_VACIO}</small>
    </span>
  ) },
];

function bajada(pacientes: readonly PacienteRespuesta[] | undefined): string {
  if (!pacientes) return "Fichas de pacientes del consultorio";
  return pacientes.length === 1 ? "1 paciente en la lista" : `${pacientes.length} pacientes en la lista`;
}

export function PaginaPacientes() {
  const { state, key } = useLocation();
  const [parametros, cambiarParametros] = useSearchParams();
  const [alta, actualizarAlta] = useState(false);
  const [texto, actualizarTexto] = useState("");
  const [soloConTurno, actualizarSoloConTurno] = useState(false);
  const { pacientes, cargando, errorCarga, buscar, recargar } = usePacientes();
  const ahora = Date.now();
  const hoy = fechaLocal(ahora);
  const { agenda } = useAgenda({ vista: "semana", fecha: hoy });
  const { agenda: agendaSiguiente } = useAgenda({ vista: "semana", fecha: sumarDias(hoy, 7) });
  const [aviso, actualizarAviso] = useState<string>();
  const [editando, actualizarEditando] = useState<PacienteRespuesta>();
  const [confirmandoBaja, actualizarConfirmandoBaja] = useState<number>();
  const [errorAccion, actualizarErrorAccion] = useState<string>();
  const pideAlta = parametros.get("alta") === "1";
  useEffect(() => {
    const origen: unknown = state;
    if (origen && typeof origen === "object" && "busqueda" in origen && typeof origen.busqueda === "string") {
      actualizarTexto(origen.busqueda);
    }
  }, [state, key]);
  useEffect(() => {
    if (!pideAlta) return;
    actualizarAlta(true);
    cambiarParametros((actuales) => {
      const siguientes = new URLSearchParams(actuales);
      siguientes.delete("alta");
      return siguientes;
    }, { replace: true });
  }, [pideAlta, cambiarParametros]);

  const proximos = new Map<number, TurnoAgenda>();
  const turnos = [agenda, agendaSiguiente].flatMap((datos) => Array.isArray(datos?.turnos) ? datos.turnos : []);
  for (const turno of turnos) {
    const anterior = proximos.get(turno.pacienteId);
    if ((turno.estado === "CONFIRMADO" || turno.estado === "PROGRAMADO") && Date.parse(turno.inicio) >= ahora
      && (!anterior || Date.parse(anterior.inicio) > Date.parse(turno.inicio))) {
      proximos.set(turno.pacienteId, turno);
    }
  }
  const terminos = texto.trim().toLocaleLowerCase("es").split(/\s+/u).filter(Boolean);
  const visibles = (pacientes ?? []).filter((paciente) => {
    const campos = [paciente.apellido, paciente.nombre, paciente.dni, paciente.email].filter(Boolean).join(" ").toLocaleLowerCase("es");
    return (!soloConTurno || proximos.has(paciente.id)) && terminos.every((termino) => campos.includes(termino));
  });

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
        actualizarAlta(false);
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
      clave: "proximo", titulo: "Próximo turno", celda: (p) => {
        const turno = proximos.get(p.id);
        if (!turno) return MARCA_VACIO;
        const dia = fechaLocal(turno.inicio) === hoy ? "Hoy" : formatearFecha(turno.inicio);
        return <span className="mono">{dia} · {formatearHora(turno.inicio)}</span>;
      },
    },
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
        <Boton variante="secundaria" aria-expanded={alta} onClick={() => {
          actualizarEditando(undefined);
          actualizarAlta(true);
        }}><Icono nombre="mas" />Registrar paciente</Boton>
      </div>
      {aviso ? <Toast mensaje={aviso} /> : null}
      {errorAccion ? <Alerta tipo="peligro" titulo={errorAccion} /> : null}
      <div className="pacientes">
      <section className="panel panel--plano pacientes__listado" aria-label="Listado de pacientes">
      <div className="pacientes__buscar">
        <Campo etiqueta="Buscar pacientes" etiquetaOculta type="search" placeholder="Nombre, apellido, DNI o email"
          value={texto} onChange={(evento) => actualizarTexto(evento.target.value)} />
        <div className="segmentado" role="group" aria-label="Buscar por">
          <button type="button" aria-pressed={!soloConTurno} onClick={() => actualizarSoloConTurno(false)}>Todos</button>
          <button type="button" aria-pressed={soloConTurno} onClick={() => actualizarSoloConTurno(true)}>Con turno próximo</button>
        </div>
      </div>
      <details className="pacientes__avanzada">
        <summary>Búsqueda avanzada</summary>
        <BusquedaPacientes onBuscar={(filtros) => void buscar(filtros)} />
      </details>
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
          filas={visibles}
          claveFila={(p) => p.id}
          mensajeVacio="No se encontraron pacientes."
        />
      )}
      </section>
      {(alta || editando) && <FormularioPaciente
        key={editando?.id ?? "nuevo"}
        valorInicial={editando}
        etiquetaEnvio={editando ? "Guardar cambios" : "Agregar paciente"}
        onEnviar={guardar}
        onCancelar={() => { actualizarEditando(undefined); actualizarAlta(false); }}
      />}
      </div>
    </section>
  );
}
