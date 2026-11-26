import { useId } from "react";
import { Boton, Icono } from "../../shared/ui";
import { useVistaMovil } from "../../shared/ui/useVistaMovil";
import type { CatalogosAgenda } from "./useCatalogosAgenda";

function Filtro(props: Readonly<{
  etiqueta: string;
  todos: string;
  valor?: number;
  opciones: readonly Readonly<{ id: number; nombre: string }>[];
  onCambiar: (id?: number) => void;
}>) {
  const id = useId();
  return (
    <div className="agenda-barra__filtro">
      <label htmlFor={id} className="sr-only">{props.etiqueta}</label>
      <select
        id={id}
        className="input"
        value={props.valor ?? ""}
        onChange={(evento) => props.onCambiar(evento.target.value === "" ? undefined : Number(evento.target.value))}
      >
        <option value="">{props.todos}</option>
        {props.opciones.map((opcion) => (
          <option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>
        ))}
      </select>
    </div>
  );
}

export type BarraAgendaProps = Readonly<{
  /** "Lunes 07/12/2026" o "Semana del …"; vacío mientras carga la primera vez. */
  titulo?: string;
  actualizando: boolean;
  fecha?: string;
  profesionalId?: number;
  sillonId?: number;
  catalogos: CatalogosAgenda;
  navegable: boolean;
  onDesplazar: (sentido: -1 | 1) => void;
  onHoy: () => void;
  onFecha: (fecha: string) => void;
  onProfesional: (id?: number) => void;
  onSillon: (id?: number) => void;
}>;

/** Controles de consulta: no modifican datos, solo cambian lo que se pide a `GET /api/agenda`. */
export function BarraAgenda(props: BarraAgendaProps) {
  const idFecha = useId();
  const profesionales = props.catalogos.profesionales.map((p) => ({ id: p.id, nombre: `${p.nombre} ${p.apellido}` }));
  const movil = useVistaMovil();
  const filtros = (
    <div className="agenda-barra__filtros">
      <div className="agenda-barra__fecha">
        <label htmlFor={idFecha} className="sr-only">Fecha</label>
        <input
          id={idFecha}
          className="input"
          type="date"
          value={props.fecha ?? ""}
          onChange={(evento) => {
            if (evento.target.value) props.onFecha(evento.target.value);
          }}
        />
      </div>
      <Filtro etiqueta="Profesional" todos="Todos los profesionales" valor={props.profesionalId} opciones={profesionales} onCambiar={props.onProfesional} />
      <Filtro etiqueta="Sillón" todos="Todos los sillones" valor={props.sillonId} opciones={props.catalogos.sillones} onCambiar={props.onSillon} />
    </div>
  );
  return (
    <section className="panel panel--plano agenda-barra" aria-label="Navegación de la agenda">
      <div className="agenda-barra__navegar" role="group" aria-label="Período">
        <button type="button" className="boton-icono" aria-label="Anterior" disabled={!props.navegable} onClick={() => props.onDesplazar(-1)}>
          <Icono nombre="izq" />
        </button>
        <h3 className="agenda-periodo" aria-busy={props.actualizando}>{props.titulo ?? "Cargando…"}</h3>
        <button type="button" className="boton-icono" aria-label="Siguiente" disabled={!props.navegable} onClick={() => props.onDesplazar(1)}>
          <Icono nombre="der" />
        </button>
        <Boton variante="secundaria" tamano="chico" onClick={props.onHoy}>Hoy</Boton>
      </div>
      {movil ? (
        <details className="agenda-barra__opciones">
          <summary>Fecha y filtros</summary>
          {filtros}
        </details>
      ) : filtros}
    </section>
  );
}
