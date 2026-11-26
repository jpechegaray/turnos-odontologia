import { useRef, useState, type FormEvent } from "react";
import { api } from "../../../shared/api/clienteApi";
import { Alerta, Boton, Campo } from "../../../shared/ui";
import { CampoSeleccion } from "../../../shared/ui/CampoSeleccion";
import { consultaDePacientes, FILTROS_VACIOS } from "../../pacientes/consulta";
import type { FiltrosPacientes, PacienteRespuesta } from "../../pacientes/tipos";
// El componente usa las clases `alta-turno__*`: se importan acá para que funcione fuera del alta de turnos (C-47).
import "./alta-turno.css";

const CRITERIOS = [
  { valor: "apellido", etiqueta: "Apellido" },
  { valor: "nombre", etiqueta: "Nombre" },
  { valor: "dni", etiqueta: "DNI" },
  { valor: "email", etiqueta: "Email" },
] as const;

type PacienteParaTurno = Pick<PacienteRespuesta, "id" | "nombre" | "apellido" | "dni">;

type EstadoBusqueda =
  | { tipo: "cargando" }
  | { tipo: "error" }
  | { tipo: "lista"; pacientes: readonly PacienteParaTurno[] };

export function SeleccionPaciente({ pacienteId, onElegir, bloqueada }: Readonly<{
  pacienteId: string;
  onElegir: (id: string) => void;
  bloqueada: boolean;
}>) {
  const [criterio, cambiarCriterio] = useState<keyof FiltrosPacientes>("apellido");
  const [texto, cambiarTexto] = useState("");
  const [busqueda, actualizarBusqueda] = useState<EstadoBusqueda>();
  const pedido = useRef(0);

  async function buscar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!texto.trim() || bloqueada) return;
    const solicitud = ++pedido.current;
    onElegir("");
    actualizarBusqueda({ tipo: "cargando" });
    try {
      const pacientes = await api.get<PacienteRespuesta[]>(
        consultaDePacientes({ ...FILTROS_VACIOS, [criterio]: texto }),
      );
      if (solicitud !== pedido.current) return;
      actualizarBusqueda({
        tipo: "lista",
        pacientes: pacientes.filter((p) => p.activo).map(({ id, nombre, apellido, dni }) => ({ id, nombre, apellido, dni })),
      });
    } catch {
      if (solicitud === pedido.current) actualizarBusqueda({ tipo: "error" });
    }
  }

  function cambiarDato() {
    pedido.current++;
    actualizarBusqueda(undefined);
    onElegir("");
  }

  return (
    <form className="alta-turno__grupo" role="search" aria-label="Buscar paciente" onSubmit={(evento) => void buscar(evento)}>
      <div className="alta-turno__campos">
        <CampoSeleccion
          etiqueta="Buscar por"
          value={criterio}
          opciones={CRITERIOS}
          disabled={bloqueada}
          onChange={(evento) => {
            const opcion = CRITERIOS.find((item) => item.valor === evento.target.value);
            if (!opcion) return;
            cambiarCriterio(opcion.valor);
            cambiarDato();
          }}
        />
        <Campo etiqueta="Dato del paciente" value={texto} disabled={bloqueada} autoComplete="off"
          onChange={(evento) => { cambiarTexto(evento.target.value); cambiarDato(); }} />
        <div className="alta-turno__acciones">
          <Boton type="submit" variante="secundaria" disabled={bloqueada || !texto.trim() || busqueda?.tipo === "cargando"}>
            Buscar paciente
          </Boton>
        </div>
      </div>
      {busqueda?.tipo === "cargando" && <p role="status">Buscando pacientes…</p>}
      {busqueda?.tipo === "error" && <Alerta tipo="peligro" titulo="No pudimos buscar pacientes. Volvé a intentar." />}
      {busqueda?.tipo === "lista" && (busqueda.pacientes.length > 0 ? (
        <CampoSeleccion
          etiqueta="Paciente"
          vacia="Elegí un paciente"
          value={pacienteId}
          disabled={bloqueada}
          onChange={(evento) => onElegir(evento.target.value)}
          opciones={busqueda.pacientes.map((p) => ({
            valor: String(p.id), etiqueta: `${p.nombre} ${p.apellido}${p.dni ? ` · DNI ${p.dni}` : ""}`,
          }))}
        />
      ) : <p role="status">No encontramos pacientes activos. Probá con otro dato.</p>)}
    </form>
  );
}
