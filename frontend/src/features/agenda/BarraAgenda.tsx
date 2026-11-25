import { useId } from "react";
import { Boton, Campo } from "../../shared/ui";
import type { Agrupacion, Vista } from "./tipos";
import type { CatalogosAgenda } from "./useCatalogosAgenda";

type Opcion<T extends string> = Readonly<{ valor: T; etiqueta: string }>;

const VISTAS: readonly Opcion<Vista>[] = [
  { valor: "dia", etiqueta: "Día" },
  { valor: "semana", etiqueta: "Semana" },
];
const AGRUPACIONES: readonly Opcion<Agrupacion>[] = [
  { valor: "profesional", etiqueta: "Por profesional" },
  { valor: "sillon", etiqueta: "Por sillón" },
];

function Alternativas<T extends string>(props: Readonly<{
  titulo: string;
  opciones: readonly Opcion<T>[];
  valor: T;
  onElegir: (valor: T) => void;
}>) {
  return (
    <div className="agenda-barra__grupo" role="group" aria-label={props.titulo}>
      {props.opciones.map((opcion) => (
        <Boton
          key={opcion.valor}
          tamano="chico"
          variante={opcion.valor === props.valor ? "primaria" : "secundaria"}
          aria-pressed={opcion.valor === props.valor}
          onClick={() => props.onElegir(opcion.valor)}
        >
          {opcion.etiqueta}
        </Boton>
      ))}
    </div>
  );
}

function Filtro(props: Readonly<{
  etiqueta: string;
  valor?: number;
  opciones: readonly Readonly<{ id: number; nombre: string }>[];
  onCambiar: (id?: number) => void;
}>) {
  const id = useId();
  return (
    <div className="field">
      <label htmlFor={id}>{props.etiqueta}</label>
      <select
        id={id}
        className="input"
        value={props.valor ?? ""}
        onChange={(evento) => props.onCambiar(evento.target.value === "" ? undefined : Number(evento.target.value))}
      >
        <option value="">Todos</option>
        {props.opciones.map((opcion) => (
          <option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>
        ))}
      </select>
    </div>
  );
}

export type BarraAgendaProps = Readonly<{
  vista: Vista;
  agrupacion: Agrupacion;
  fecha?: string;
  profesionalId?: number;
  sillonId?: number;
  catalogos: CatalogosAgenda;
  navegable: boolean;
  onVista: (vista: Vista) => void;
  onAgrupacion: (agrupacion: Agrupacion) => void;
  onDesplazar: (sentido: -1 | 1) => void;
  onHoy: () => void;
  onFecha: (fecha: string) => void;
  onProfesional: (id?: number) => void;
  onSillon: (id?: number) => void;
}>;

/** Controles de consulta: no modifican datos, solo cambian lo que se pide a `GET /api/agenda`. */
export function BarraAgenda(props: BarraAgendaProps) {
  const profesionales = props.catalogos.profesionales.map((p) => ({ id: p.id, nombre: `${p.nombre} ${p.apellido}` }));
  return (
    <div className="agenda-barra">
      <Alternativas titulo="Vista" opciones={VISTAS} valor={props.vista} onElegir={props.onVista} />
      {props.vista === "dia" && (
        <Alternativas titulo="Columnas" opciones={AGRUPACIONES} valor={props.agrupacion} onElegir={props.onAgrupacion} />
      )}
      <div className="agenda-barra__grupo" role="group" aria-label="Período">
        <Boton variante="secundaria" tamano="chico" disabled={!props.navegable} onClick={() => props.onDesplazar(-1)}>
          Anterior
        </Boton>
        <Boton variante="secundaria" tamano="chico" onClick={props.onHoy}>Hoy</Boton>
        <Boton variante="secundaria" tamano="chico" disabled={!props.navegable} onClick={() => props.onDesplazar(1)}>
          Siguiente
        </Boton>
      </div>
      <Campo
        etiqueta="Fecha"
        type="date"
        value={props.fecha ?? ""}
        onChange={(evento) => {
          if (evento.target.value) props.onFecha(evento.target.value);
        }}
      />
      <Filtro etiqueta="Profesional" valor={props.profesionalId} opciones={profesionales} onCambiar={props.onProfesional} />
      <Filtro etiqueta="Sillón" valor={props.sillonId} opciones={props.catalogos.sillones} onCambiar={props.onSillon} />
    </div>
  );
}
