import { useState, type CSSProperties } from "react";
import { fechaLocal, formatearHora } from "../../shared/fecha/zonaHoraria";
import { Avatar, BadgeEstadoTurno, Boton, claseDeEstado } from "../../shared/ui";
import { useVistaMovil } from "../../shared/ui/useVistaMovil";
import { ContenidoTurno, type FichaElegida } from "./ContenidoTurno";
import { cantidadDeFilas, minutosDelDia, filasDe, MINUTOS_FILA, rangoDeGrilla, tramoEnDia, type Tramo } from "./grilla";
import { horaDeMinutos, rangoLegible, type Nombres } from "./presentacion";
import type { AgendaRespuesta, Agrupacion, BloqueoAgenda, TurnoAgenda } from "./tipos";
import type { CatalogosAgenda } from "./useCatalogosAgenda";

type Ubicado<T> = Readonly<{ dato: T; tramo: Tramo }>;

type Columna = Readonly<{
  id: number;
  titulo: string;
  detalle?: string;
  atencion: readonly Tramo[];
  bloqueos: readonly Ubicado<BloqueoAgenda>[];
  turnos: readonly Ubicado<TurnoAgenda>[];
}>;

type Props = Readonly<{
  agenda: AgendaRespuesta;
  agrupacion: Agrupacion;
  /** Filtro vigente: con él la grilla muestra solo esa columna. */
  filtro?: number;
  catalogos: CatalogosAgenda;
  nombres: Nombres;
  onVerFicha: (ficha: FichaElegida) => void;
}>;

function ubicar<T extends Readonly<{ inicio: string; fin: string }>>(datos: readonly T[], fecha: string): Ubicado<T>[] {
  return datos.flatMap((dato) => {
    const tramo = tramoEnDia(dato.inicio, dato.fin, fecha);
    return tramo ? [{ dato, tramo }] : [];
  });
}

function filas(tramo: Tramo, rango: Tramo): CSSProperties {
  const { desde, hasta } = filasDe(tramo, rango);
  return { gridRow: `${desde} / ${hasta}` };
}

/** Columnas activas del catálogo más las que aparecen en los datos (por ejemplo, un recurso ya inactivo). */
function idsDeColumnas(agrupacion: Agrupacion, agenda: AgendaRespuesta, catalogos: CatalogosAgenda, filtro?: number) {
  if (filtro !== undefined) return [filtro];
  const delCatalogo = agrupacion === "profesional"
    ? catalogos.profesionales.filter((p) => p.activo).map((p) => p.id)
    : catalogos.sillones.filter((s) => s.activo).map((s) => s.id);
  const deLosDatos = agrupacion === "profesional"
    ? [...agenda.turnos.map((t) => t.profesionalId), ...agenda.horarios.map((h) => h.profesionalId),
       ...agenda.bloqueos.map((b) => b.profesionalId)]
    : agenda.turnos.map((t) => t.sillonId);
  return [...new Set([...delCatalogo, ...deLosDatos])];
}

function textoAtencion(horarios: AgendaRespuesta["horarios"]): string {
  if (horarios.length === 0) return "No atiende";
  return `Atiende ${horarios.map((h) => `de ${formatearHora(h.inicio)} a ${formatearHora(h.fin)}`).join(" y ")}`;
}

function AgendaMovil({ columnas, agrupacion, nombres, onVerFicha }: Pick<Props, "agrupacion" | "nombres" | "onVerFicha"> & {
  columnas: readonly Columna[];
}) {
  const [preferida, elegir] = useState<number>();
  const columna = columnas.find(({ id }) => id === preferida) ?? columnas[0];
  const eventos = [
    ...columna.turnos.map(({ dato }) => ({ tipo: "turno", dato } as const)),
    ...columna.bloqueos.map(({ dato }) => ({ tipo: "bloqueo", dato } as const)),
  ].sort((a, b) => Date.parse(a.dato.inicio) - Date.parse(b.dato.inicio));
  return (
    <div className="agenda-movil">
      <div className="agenda-movil__recursos" role="group" aria-label={agrupacion === "profesional" ? "Profesional" : "Sillón"}>
        {columnas.map((opcion) => (
          <Boton key={opcion.id} variante={opcion.id === columna.id ? "primaria" : "secundaria"}
            aria-pressed={opcion.id === columna.id} onClick={() => elegir(opcion.id)}>
            {agrupacion === "profesional" && <Avatar nombre={opcion.titulo} colorId={opcion.id} tamano="sm" />}
            {opcion.titulo}
          </Boton>
        ))}
      </div>
      {eventos.length === 0 ? <p className="muted">No hay turnos ni bloqueos.</p> : (
        <ul className="agenda-movil__lista">
          {eventos.map(({ tipo, dato }) => (
            <li key={`${tipo}-${dato.id}`}>
              <div className="agenda-movil__hora mono">
                <strong>{formatearHora(dato.inicio)}</strong>
                <small>{Math.round((Date.parse(dato.fin) - Date.parse(dato.inicio)) / 60_000)} min</small>
              </div>
              <div className="agenda-movil__detalle">
                {tipo === "turno" ? (
                  <ContenidoTurno turno={dato} prestacion={nombres.prestacion(dato.prestacionId)}
                    detalle={agrupacion === "profesional" ? nombres.sillon(dato.sillonId) : nombres.profesional(dato.profesionalId)}
                    onVerFicha={onVerFicha} />
                ) : <><strong>{dato.motivo || "Bloqueo"}</strong><BadgeEstadoTurno estado="BLOQUEO" /></>}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/**
 * Vista diaria en filas de 15 minutos, con una columna por profesional o por sillón. Los horarios y los
 * bloqueos son del profesional (SU-08), así que solo se dibujan en la agrupación por profesional.
 */
export function GrillaDia({ agenda, agrupacion, filtro, catalogos, nombres, onVerFicha }: Props) {
  const movil = useVistaMovil();
  const fecha = agenda.desde;
  const porProfesional = agrupacion === "profesional";
  const columnas: Columna[] = idsDeColumnas(agrupacion, agenda, catalogos, filtro)
    .map((id) => {
      const turnos = agenda.turnos.filter((t) => (porProfesional ? t.profesionalId : t.sillonId) === id);
      if (!porProfesional) return { id, titulo: nombres.sillon(id), atencion: [], bloqueos: [], turnos: ubicar(turnos, fecha) };
      const horarios = agenda.horarios.filter((h) => h.profesionalId === id && h.fecha === fecha);
      return {
        id,
        titulo: nombres.profesional(id),
        detalle: textoAtencion(horarios),
        atencion: ubicar(horarios, fecha).map((h) => h.tramo),
        bloqueos: ubicar(agenda.bloqueos.filter((b) => b.profesionalId === id), fecha),
        turnos: ubicar(turnos, fecha),
      };
    })
    .sort((a, b) => a.titulo.localeCompare(b.titulo, "es"));

  if (columnas.length === 0) {
    return <p>{porProfesional ? "No hay profesionales para mostrar." : "No hay sillones para mostrar."}</p>;
  }
  if (movil) return <AgendaMovil columnas={columnas} agrupacion={agrupacion} nombres={nombres} onVerFicha={onVerFicha} />;

  const rango = rangoDeGrilla(
    columnas.flatMap((c) => [...c.atencion, ...c.bloqueos.map((b) => b.tramo), ...c.turnos.map((t) => t.tramo)]),
  );
  const cantidad = cantidadDeFilas(rango);
  const horas = Array.from({ length: cantidad }, (_, i) => rango.desdeMin + i * MINUTOS_FILA);
  const dimensiones = { "--agenda-columnas": columnas.length, "--agenda-filas": cantidad } as CSSProperties;
  // Línea de "ahora": solo si el día mostrado es hoy y la hora cae dentro de la grilla.
  const ahora = Date.now();
  const minutoActual = fechaLocal(ahora) === fecha ? minutosDelDia(ahora) : undefined;
  const lineaAhora = minutoActual !== undefined && minutoActual >= rango.desdeMin && minutoActual < rango.hastaMin
    ? ({ "--agenda-ahora-filas": (minutoActual - rango.desdeMin) / MINUTOS_FILA } as CSSProperties)
    : undefined;

  return (
    <div className="agenda-wrap" role="region" aria-label="Agenda del día, desplazable horizontalmente" tabIndex={0}>
      <div className="agenda" style={dimensiones}>
        <div className="agenda__columna agenda__columna--horas" aria-hidden="true">
          <div className="agenda__corner" />
          {horas.map((minutos, i) => (
            <div key={minutos} className="agenda__time mono" style={{ gridRow: i + 2 }}>
              {minutos % 60 === 0 ? horaDeMinutos(minutos) : ""}
            </div>
          ))}
          {lineaAhora && minutoActual !== undefined && (
            <b className="agenda__ahora-rotulo" style={lineaAhora}>{horaDeMinutos(minutoActual)}</b>
          )}
        </div>
        {columnas.map((columna) => (
          <section key={columna.id} className="agenda__columna" aria-label={columna.titulo}>
            <div className="agenda__col-head">
              {porProfesional ? <Avatar nombre={columna.titulo} colorId={columna.id} /> : null}
              <div className="agenda__col-texto">
                <strong>{columna.titulo}</strong>
                {columna.detalle && <small>{columna.detalle}</small>}
              </div>
            </div>
            {lineaAhora && <div className="agenda__ahora" style={lineaAhora} aria-hidden="true" />}
            {columna.atencion.map((tramo) => (
              <div key={`atencion-${tramo.desdeMin}`} className="agenda__atencion" aria-hidden="true" style={filas(tramo, rango)} />
            ))}
            {columna.bloqueos.map(({ dato, tramo }) => (
              <div key={`bloqueo-${dato.id}`} className="slot slot--bloqueo" style={filas(tramo, rango)}>
                {dato.motivo && <strong>{dato.motivo}</strong>}
                <span className="slot__meta">{rangoLegible(dato.inicio, dato.fin)}</span>
                <span className="slot__pie"><BadgeEstadoTurno estado="BLOQUEO" /></span>
              </div>
            ))}
            {columna.turnos.map(({ dato, tramo }) => (
              <div
                key={`turno-${dato.id}`}
                className={`slot ${claseDeEstado(dato.estado)}${dato.estado === "CANCELADO" ? " slot--cancelado" : ""}`}
                style={filas(tramo, rango)}
              >
                <ContenidoTurno
                  turno={dato}
                  prestacion={nombres.prestacion(dato.prestacionId)}
                  detalle={porProfesional ? nombres.sillon(dato.sillonId) : nombres.profesional(dato.profesionalId)}
                  onVerFicha={onVerFicha}
                />
              </div>
            ))}
          </section>
        ))}
      </div>
    </div>
  );
}
