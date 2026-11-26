import { useRef, useState } from "react";
import type { Rol } from "../../../app/rutas/tipos";
import { api } from "../../../shared/api/clienteApi";
import { formatearFecha } from "../../../shared/fecha/zonaHoraria";
import { Alerta, BadgeEstadoTurno, Boton } from "../../../shared/ui";
import { CampoSeleccion } from "../../../shared/ui/CampoSeleccion";
import { nombreDelPaciente, rangoLegible, type Nombres } from "../presentacion";
import type { TurnoAgenda } from "../tipos";
import type { CatalogosAgenda } from "../useCatalogosAgenda";
import {
  accionesDeTurno, ETIQUETAS_GESTION, mensajeErrorGestion, requiereRecarga, rutaDeGestion,
  type AccionTurno, type Gestion, type TurnoGestionado,
} from "./acciones";
import { FormularioAsistencia } from "./FormularioAsistencia";
import { FormularioCambioTurno } from "./FormularioCambioTurno";
import { useAhoraEnLimites } from "./useAhoraEnLimites";
import "./gestion-turnos.css";

type ErrorGestion = Readonly<{ mensaje: string; requiereRecarga: boolean }>;

type Props = Readonly<{
  tituloId: string;
  turnos: readonly TurnoAgenda[];
  rol: Rol;
  catalogos: CatalogosAgenda;
  nombres: Nombres;
  cargandoAgenda: boolean;
  onCerrar: () => void;
  onActualizarAgenda: () => void;
  onCompletado: (gestion: Gestion, turno: TurnoGestionado) => void;
}>;

export function GestionTurnos({ tituloId, turnos, rol, catalogos, nombres, cargandoAgenda, onCerrar, onActualizarAgenda, onCompletado }: Props) {
  const [turnoId, elegirTurno] = useState("");
  const [accion, elegirAccion] = useState<AccionTurno>();
  const [error, cambiarError] = useState<ErrorGestion>();
  const [guardando, cambiarGuardando] = useState(false);
  const enviando = useRef(false);
  const ahora = useAhoraEnLimites(turnos.map((turno) => Date.parse(turno.inicio)));
  const gestionables = turnos.filter((turno) => accionesDeTurno(turno, rol, ahora).length > 0)
    .sort((a, b) => Date.parse(a.inicio) - Date.parse(b.inicio) || a.id - b.id);
  const elegido = gestionables.find((turno) => String(turno.id) === turnoId);
  const acciones = elegido ? accionesDeTurno(elegido, rol, ahora) : [];
  const accionVigente = accion && acciones.includes(accion) ? accion : undefined;
  const bloqueado = guardando || cargandoAgenda || Boolean(error?.requiereRecarga);

  async function confirmar(gestion: Gestion) {
    if (!elegido || bloqueado || enviando.current || accionVigente !== gestion.accion) return;
    if (!accionesDeTurno(elegido, rol, Date.now()).includes(gestion.accion)) {
      cambiarError({ mensaje: "El turno cambió. Actualizá la agenda antes de continuar.", requiereRecarga: true });
      return;
    }
    enviando.current = true;
    cambiarGuardando(true);
    cambiarError(undefined);
    try {
      const resultado = await api.post<TurnoGestionado>(rutaDeGestion(elegido.id, gestion.accion), gestion.cuerpo);
      onCompletado(gestion, resultado);
    } catch (causa) {
      cambiarError({ mensaje: mensajeErrorGestion(causa), requiereRecarga: requiereRecarga(causa) });
    } finally {
      enviando.current = false;
      cambiarGuardando(false);
    }
  }

  return (
    <section id={`${tituloId}-panel`} className="panel gestion-turnos" aria-labelledby={tituloId} aria-busy={guardando}>
      <div className="gestion-turnos__encabezado">
        <h3 id={tituloId} tabIndex={-1}>Gestionar turnos</h3>
        <Boton variante="secundaria" onClick={onCerrar} disabled={guardando}>Cerrar gestión</Boton>
      </div>
      {gestionables.length > 0 ? (
        <CampoSeleccion etiqueta="Turno" opciones={gestionables.map((turno) => ({
          valor: String(turno.id),
          etiqueta: `${formatearFecha(turno.inicio)} · ${rangoLegible(turno.inicio, turno.fin)} · ${nombreDelPaciente(turno) ?? `Turno ${turno.id}`} · ${nombres.profesional(turno.profesionalId)}`,
        }))} vacia="Elegí un turno"
          value={elegido ? turnoId : ""} disabled={bloqueado}
          onChange={(evento) => {
            elegirTurno(evento.target.value);
            elegirAccion(undefined);
            cambiarError(undefined);
          }} />
      ) : <p role="status">No hay turnos para gestionar en la vista actual.</p>}
      {elegido && (
        <>
          <div className="gestion-turnos__acciones">
            <BadgeEstadoTurno estado={elegido.estado} />
            <p>{nombres.prestacion(elegido.prestacionId)} · {nombres.sillon(elegido.sillonId)}</p>
          </div>
          <div className="gestion-turnos__acciones" role="group" aria-label="Acciones del turno">
            {acciones.map((opcion) => (
              <Boton key={opcion} variante={opcion === accionVigente ? "primaria" : "secundaria"}
                aria-pressed={opcion === accionVigente} disabled={bloqueado}
                onClick={() => { elegirAccion(opcion); cambiarError(undefined); }}>
                {ETIQUETAS_GESTION[opcion]}
              </Boton>
            ))}
          </div>
        </>
      )}
      {error && (
        <Alerta tipo="peligro" titulo={error.mensaje}>
          {error.requiereRecarga && (
            <Boton variante="secundaria" onClick={onActualizarAgenda}>Actualizar agenda</Boton>
          )}
        </Alerta>
      )}
      {elegido && (accionVigente === "reprogramar" || accionVigente === "cancelar") && (
        <FormularioCambioTurno key={`${elegido.id}-${accionVigente}`} accion={accionVigente}
          turno={elegido} catalogos={catalogos} bloqueado={bloqueado} onConfirmar={(gestion) => void confirmar(gestion)} />
      )}
      {elegido && (accionVigente === "asistencia" || accionVigente === "corregir") && (
        <FormularioAsistencia key={`${elegido.id}-${accionVigente}`} turno={elegido}
          corregir={accionVigente === "corregir"} bloqueado={bloqueado} onConfirmar={(gestion) => void confirmar(gestion)} />
      )}
      {guardando && <p role="status">Guardando cambio…</p>}
    </section>
  );
}
