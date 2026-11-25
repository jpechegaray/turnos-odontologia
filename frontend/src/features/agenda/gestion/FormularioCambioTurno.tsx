import { useState, type FormEvent } from "react";
import { aInstanteUtc, fechaLocal, formatearHora } from "../../../shared/fecha/zonaHoraria";
import { Alerta, Boton, Campo } from "../../../shared/ui";
import { CampoSeleccion } from "../../../shared/ui/CampoSeleccion";
import { useConfiguracion } from "../../configuracion/useConfiguracion";
import { duracionValida } from "../crear/consulta";
import type { TurnoAgenda } from "../tipos";
import type { CatalogosAgenda } from "../useCatalogosAgenda";
import type { Gestion } from "./acciones";
import { useAhoraEnLimites } from "./useAhoraEnLimites";

type Props = Readonly<{
  accion: "reprogramar" | "cancelar";
  turno: TurnoAgenda;
  catalogos: CatalogosAgenda;
  bloqueado: boolean;
  onConfirmar: (gestion: Gestion) => void;
}>;
type Opcion = Readonly<{ valor: string; etiqueta: string }>;

const HORA_EN_GRILLA = /^([01]\d|2[0-3]):[0-5][05]$/;

function agregarActualSiFalta(opciones: readonly Opcion[], actual: number, etiqueta: string): readonly Opcion[] {
  if (opciones.some((opcion) => opcion.valor === String(actual))) return opciones;
  return [{ valor: String(actual), etiqueta: `${etiqueta} actual (${actual})` }, ...opciones];
}

function instanteValido(fecha: string, hora: string): string | undefined {
  if (!HORA_EN_GRILLA.test(hora)) return undefined;
  try {
    return aInstanteUtc(fecha, hora).toISOString();
  } catch {
    return undefined;
  }
}

export function FormularioCambioTurno({ accion, turno, catalogos, bloqueado, onConfirmar }: Props) {
  const reprograma = accion === "reprogramar";
  const { configuracion, cargando, recargar } = useConfiguracion();
  const [fecha, cambiarFecha] = useState(() => fechaLocal(turno.inicio));
  const [hora, cambiarHora] = useState(() => formatearHora(turno.inicio));
  const [duracion, cambiarDuracion] = useState(String(turno.duracionMin));
  const [profesionalId, elegirProfesional] = useState(String(turno.profesionalId));
  const [sillonId, elegirSillon] = useState(String(turno.sillonId));
  const [motivo, cambiarMotivo] = useState("");
  const inicioOriginal = Date.parse(turno.inicio);
  const horasPlazo = configuracion?.["cancelacion.plazo_horas"];
  const plazo = horasPlazo !== undefined && Number.isInteger(horasPlazo) && horasPlazo >= 0 && horasPlazo <= 168
    ? horasPlazo * 3_600_000 : undefined;
  const ahora = useAhoraEnLimites(plazo === undefined ? [] : [inicioOriginal - plazo + 1]);
  const tardia = plazo !== undefined && inicioOriginal - ahora < plazo;
  const nuevoInicio = reprograma ? instanteValido(fecha, hora) : undefined;
  const fechaDisponible = nuevoInicio !== undefined && Date.parse(nuevoInicio) > Date.now();
  const cambioHorario = nuevoInicio !== undefined && Date.parse(nuevoInicio) !== inicioOriginal;
  const hayCambios = cambioHorario || Number(profesionalId) !== turno.profesionalId
    || Number(sillonId) !== turno.sillonId || Number(duracion) !== turno.duracionMin;
  const cambioValido = Boolean(fechaDisponible && duracionValida(Number(duracion)) && profesionalId && sillonId && hayCambios);
  const puedeEnviar = !bloqueado && !cargando && (reprograma ? cambioValido : motivo.length <= 255);

  const profesionales = agregarActualSiFalta(catalogos.profesionales.filter((p) => p.activo).map((p) => ({
    valor: String(p.id), etiqueta: `${p.nombre} ${p.apellido}`,
  })), turno.profesionalId, "Profesional");
  const sillones = agregarActualSiFalta(catalogos.sillones.filter((s) => s.activo).map((s) => ({
    valor: String(s.id), etiqueta: s.nombre,
  })), turno.sillonId, "Sillón");

  function confirmar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!puedeEnviar) return;
    if (reprograma) {
      if (!nuevoInicio || Date.parse(nuevoInicio) <= Date.now()) return;
      onConfirmar({ accion, cuerpo: { inicio: nuevoInicio, profesionalId: Number(profesionalId),
        sillonId: Number(sillonId), duracionMin: Number(duracion) } });
    } else {
      const texto = motivo.trim();
      onConfirmar({ accion, cuerpo: texto ? { motivo: texto } : {} });
    }
  }

  return (
    <form className="gestion-turnos__formulario" aria-label={reprograma ? "Reprogramación" : "Cancelación"} onSubmit={confirmar}>
      {cargando ? <p role="status">Consultando el plazo vigente…</p> : plazo === undefined ? (
        <Alerta tipo="info" titulo="No pudimos consultar el plazo. El servidor determinará si la operación es tardía.">
          <Boton variante="secundaria" disabled={bloqueado} onClick={() => void recargar()}>Reintentar consulta del plazo</Boton>
        </Alerta>
      ) : tardia && (
        <div role="status">
          <Alerta tipo="info" titulo={reprograma ? "Se registrará como reprogramación tardía." : "Se registrará como cancelación tardía."} />
        </div>
      )}
      {reprograma ? (
        <>
          <div className="alta-turno__campos">
            <Campo etiqueta="Nueva fecha" type="date" value={fecha} disabled={bloqueado}
              onChange={(evento) => cambiarFecha(evento.target.value)}
              error={nuevoInicio && !fechaDisponible ? "Elegí una fecha y hora futuras." : undefined} />
            <Campo etiqueta="Nueva hora" type="time" step={300} value={hora} disabled={bloqueado}
              ayuda="Hora de Buenos Aires." onChange={(evento) => cambiarHora(evento.target.value)}
              error={hora && !HORA_EN_GRILLA.test(hora) ? "Elegí una hora en múltiplos de 5 minutos." : undefined} />
            <Campo etiqueta="Duración nueva (minutos)" type="number" min={5} max={480} step={5} value={duracion}
              disabled={bloqueado} onChange={(evento) => cambiarDuracion(evento.target.value)}
              error={duracion && !duracionValida(Number(duracion)) ? "Ingresá de 5 a 480 minutos, en múltiplos de 5." : undefined} />
          </div>
          <div className="alta-turno__campos">
            <CampoSeleccion etiqueta="Profesional nuevo" opciones={profesionales} value={profesionalId}
              disabled={bloqueado} onChange={(evento) => elegirProfesional(evento.target.value)} />
            <CampoSeleccion etiqueta="Sillón nuevo" opciones={sillones} value={sillonId}
              disabled={bloqueado} onChange={(evento) => elegirSillon(evento.target.value)} />
          </div>
        </>
      ) : (
        <Campo etiqueta="Motivo (opcional)" maxLength={255} value={motivo}
          disabled={bloqueado} onChange={(evento) => cambiarMotivo(evento.target.value)} />
      )}
      <div className="gestion-turnos__acciones">
        <Boton type="submit" variante={reprograma ? "primaria" : "peligro"} disabled={!puedeEnviar}>
          {reprograma ? "Confirmar reprogramación" : "Confirmar cancelación"}
        </Boton>
      </div>
    </form>
  );
}
