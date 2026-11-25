import { useRef, useState, type FormEvent } from "react";
import { api } from "../../../shared/api/clienteApi";
import { ErrorApi } from "../../../shared/api/errorApi";
import { Alerta, Boton, Campo } from "../../../shared/ui";
import { CampoSeleccion } from "../../../shared/ui/CampoSeleccion";
import type { PrestacionRespuesta } from "../../prestaciones/tipos";
import { rangoLegible } from "../presentacion";
import type { CatalogosAgenda } from "../useCatalogosAgenda";
import { duracionValida, rutaDeDisponibilidad, type HorarioSugerido } from "./consulta";
import { mensajeDelTurno } from "./errores";
import { SeleccionPaciente } from "./SeleccionPaciente";
import "./alta-turno.css";

type EstadoConsulta<T> = { tipo: "cargando" } | { tipo: "error"; mensaje: string } | { tipo: "lista"; datos: readonly T[] };
type TurnoCreado = Readonly<{ inicio: string }>;

export function FormularioCrearTurno({ tituloId, fechaInicial, catalogos, onCerrar, onCreado }: Readonly<{
  tituloId: string;
  fechaInicial: string;
  catalogos: CatalogosAgenda;
  onCerrar: () => void;
  onCreado: (inicio: string, profesionalId: number, sillonId: number) => void;
}>) {
  const [pacienteId, elegirPaciente] = useState("");
  const [profesionalId, elegirProfesional] = useState("");
  const [prestacionId, elegirPrestacion] = useState("");
  const [duracion, cambiarDuracion] = useState("");
  const [fecha, cambiarFecha] = useState(fechaInicial);
  const [sillonId, elegirSillon] = useState("");
  const [sugerencia, elegirSugerencia] = useState<HorarioSugerido>();
  const [prestaciones, actualizarPrestaciones] = useState<EstadoConsulta<PrestacionRespuesta>>();
  const [disponibilidad, actualizarDisponibilidad] = useState<EstadoConsulta<HorarioSugerido>>();
  const [errorAlta, actualizarErrorAlta] = useState("");
  const [guardando, actualizarGuardando] = useState(false);
  const pedidoPrestaciones = useRef(0);
  const pedidoDisponibilidad = useRef(0);
  const envioEnCurso = useRef(false);

  const minutos = Number(duracion);
  const puedeBuscar = Boolean(profesionalId && prestacionId && fecha && duracionValida(minutos));
  const puedeGuardar = puedeBuscar && Boolean(pacienteId && sugerencia && sillonId);
  const sillones = catalogos.sillones.filter((sillon) => sillon.activo);

  async function cargarPrestaciones(id: string) {
    const pedido = ++pedidoPrestaciones.current;
    actualizarPrestaciones(id ? { tipo: "cargando" } : undefined);
    if (!id) return;
    try {
      const lista = await api.get<PrestacionRespuesta[]>(`/profesionales/${id}/prestaciones`);
      if (pedido === pedidoPrestaciones.current) {
        actualizarPrestaciones({ tipo: "lista", datos: lista.filter((p) => p.activa) });
      }
    } catch {
      if (pedido === pedidoPrestaciones.current) {
        actualizarPrestaciones({ tipo: "error", mensaje: "No pudimos cargar las prestaciones del profesional." });
      }
    }
  }

  function limpiarHorario() {
    elegirSugerencia(undefined);
    elegirSillon("");
  }

  function invalidarDisponibilidad() {
    pedidoDisponibilidad.current++;
    actualizarDisponibilidad(undefined);
    actualizarErrorAlta("");
    limpiarHorario();
  }

  async function buscarHorarios() {
    if (!puedeBuscar) return;
    const pedido = ++pedidoDisponibilidad.current;
    limpiarHorario();
    actualizarDisponibilidad({ tipo: "cargando" });
    try {
      const ruta = rutaDeDisponibilidad(profesionalId, prestacionId, fecha, minutos);
      const lista = await api.get<HorarioSugerido[]>(ruta);
      if (pedido === pedidoDisponibilidad.current) actualizarDisponibilidad({ tipo: "lista", datos: lista });
    } catch (error) {
      if (pedido === pedidoDisponibilidad.current) {
        actualizarDisponibilidad({
          tipo: "error",
          mensaje: error instanceof RangeError ? "Elegí una fecha válida para buscar horarios." : mensajeDelTurno(error),
        });
      }
    }
  }

  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!puedeGuardar || !sugerencia || envioEnCurso.current) return;
    envioEnCurso.current = true;
    actualizarGuardando(true);
    actualizarErrorAlta("");
    try {
      const turno = await api.post<TurnoCreado>("/turnos", {
        pacienteId: Number(pacienteId),
        profesionalId: Number(profesionalId),
        prestacionId: Number(prestacionId),
        sillonId: Number(sillonId),
        inicio: sugerencia.inicio,
        duracionMin: minutos,
      });
      onCreado(turno.inicio, Number(profesionalId), Number(sillonId));
    } catch (error) {
      actualizarErrorAlta(mensajeDelTurno(error));
      if (error instanceof ErrorApi && error.tipo === "conflicto") void buscarHorarios();
    } finally {
      envioEnCurso.current = false;
      actualizarGuardando(false);
    }
  }

  return (
    <section id={`${tituloId}-panel`} className="panel alta-turno" aria-labelledby={tituloId}>
      <h3 id={tituloId} tabIndex={-1}>Nuevo turno</h3>

      <SeleccionPaciente pacienteId={pacienteId} onElegir={elegirPaciente} bloqueada={guardando} />

      <form className="alta-turno__grupo" aria-label="Datos del turno" onSubmit={(evento) => void guardar(evento)}>
        <div className="alta-turno__campos">
          <CampoSeleccion etiqueta="Profesional" vacia="Elegí un profesional" value={profesionalId} disabled={guardando}
            opciones={catalogos.profesionales.filter((p) => p.activo).map((p) => ({
              valor: String(p.id), etiqueta: `${p.nombre} ${p.apellido}`,
            }))}
            onChange={(evento) => {
              const id = evento.target.value;
              elegirProfesional(id);
              elegirPrestacion("");
              cambiarDuracion("");
              void cargarPrestaciones(id);
              invalidarDisponibilidad();
            }} />
          <CampoSeleccion etiqueta="Prestación" vacia="Elegí una prestación" value={prestacionId}
            disabled={guardando || prestaciones?.tipo !== "lista" || !prestaciones.datos.length}
            opciones={prestaciones?.tipo === "lista" ? prestaciones.datos.map((p) => ({
              valor: String(p.id), etiqueta: p.nombre,
            })) : []}
            onChange={(evento) => {
              const id = evento.target.value;
              const elegida = prestaciones?.tipo === "lista" ? prestaciones.datos.find((p) => String(p.id) === id) : undefined;
              elegirPrestacion(id);
              cambiarDuracion(elegida ? String(elegida.duracionDefaultMin) : "");
              invalidarDisponibilidad();
            }} />
          <Campo etiqueta="Duración (minutos)" type="number" min={5} max={480} step={5} value={duracion}
            disabled={guardando || !prestacionId}
            error={duracion && !duracionValida(minutos) ? "Ingresá de 5 a 480 minutos, en múltiplos de 5." : undefined}
            onChange={(evento) => { cambiarDuracion(evento.target.value); invalidarDisponibilidad(); }} />
          <Campo etiqueta="Fecha del turno" type="date" value={fecha} disabled={guardando}
            onChange={(evento) => { cambiarFecha(evento.target.value); invalidarDisponibilidad(); }} />
        </div>
        {prestaciones?.tipo === "cargando" && <p role="status">Cargando prestaciones…</p>}
        {prestaciones?.tipo === "error" && (
          <Alerta tipo="peligro" titulo={prestaciones.mensaje}>
            <Boton variante="secundaria" onClick={() => void cargarPrestaciones(profesionalId)}>Reintentar</Boton>
          </Alerta>
        )}
        {prestaciones?.tipo === "lista" && !prestaciones.datos.length && (
          <p role="status">El profesional no tiene prestaciones activas asignadas.</p>
        )}
        <div className="alta-turno__acciones">
          <Boton variante="secundaria" disabled={!puedeBuscar || guardando || disponibilidad?.tipo === "cargando"}
            onClick={() => { actualizarErrorAlta(""); void buscarHorarios(); }}>Buscar horarios</Boton>
        </div>
        {errorAlta && <Alerta tipo="peligro" titulo={errorAlta} />}
        {disponibilidad?.tipo === "cargando" && <p role="status">Buscando horarios…</p>}
        {disponibilidad?.tipo === "error" && <Alerta tipo="peligro" titulo={disponibilidad.mensaje} />}
        {disponibilidad?.tipo === "lista" && (disponibilidad.datos.length ? (
          <>
            <p role="status">Horarios en hora de Buenos Aires.</p>
            <div className="alta-turno__campos">
              <CampoSeleccion etiqueta="Horario sugerido" vacia="Elegí un horario" value={sugerencia?.inicio ?? ""}
                disabled={guardando}
                opciones={disponibilidad.datos.map((hueco) => ({
                  valor: hueco.inicio,
                  etiqueta: `${rangoLegible(hueco.inicio, hueco.fin)} · ${sillones.find((s) => s.id === hueco.sillonId)?.nombre ?? `Sillón ${hueco.sillonId}`}`,
                }))}
                onChange={(evento) => {
                  const horario = disponibilidad.datos.find((hueco) => hueco.inicio === evento.target.value);
                  elegirSugerencia(horario);
                  elegirSillon(horario && sillones.some((s) => s.id === horario.sillonId) ? String(horario.sillonId) : "");
                  actualizarErrorAlta("");
                }} />
              <CampoSeleccion etiqueta="Sillón" vacia="Elegí un sillón" value={sillonId}
                disabled={guardando || !sugerencia}
                opciones={sillones.map((sillon) => ({ valor: String(sillon.id), etiqueta: sillon.nombre }))}
                onChange={(evento) => { elegirSillon(evento.target.value); actualizarErrorAlta(""); }} />
            </div>
          </>
        ) : <p role="status">No hay horarios disponibles. Probá otra fecha o duración.</p>)}
        <div className="alta-turno__acciones">
          <Boton type="submit" disabled={!puedeGuardar || guardando}>{guardando ? "Guardando…" : "Guardar turno"}</Boton>
          <Boton variante="secundaria" onClick={onCerrar} disabled={guardando}>Cerrar formulario</Boton>
        </div>
      </form>
    </section>
  );
}
