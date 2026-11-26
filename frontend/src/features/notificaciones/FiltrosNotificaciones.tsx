import { useId, useState, type FormEvent } from "react";
import { Boton, Campo, Icono } from "../../shared/ui";
import { FILTROS_VACIOS, turnoValido } from "./consulta";
import { ETIQUETAS_ESTADO } from "./presentacion";
import { MENSAJE_TURNO_INVALIDO } from "./textos";
import type { EstadoNotificacion, FiltrosNotificaciones as Filtros } from "./tipos";

const ESTADOS = Object.keys(ETIQUETAS_ESTADO) as EstadoNotificacion[];

export type FiltrosNotificacionesProps = Readonly<{
  onBuscar: (filtros: Filtros) => void;
}>;

/** Filtros por estado y número de turno (US-020). Un turno inválido no consulta. */
export function FiltrosNotificaciones({ onBuscar }: FiltrosNotificacionesProps) {
  const [filtros, actualizarFiltros] = useState<Filtros>(FILTROS_VACIOS);
  const [errorTurno, actualizarErrorTurno] = useState<string>();
  const idEstado = useId();

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!turnoValido(filtros.turno)) {
      actualizarErrorTurno(MENSAJE_TURNO_INVALIDO);
      return;
    }
    actualizarErrorTurno(undefined);
    onBuscar(filtros);
  }

  return (
    <form role="search" aria-label="Filtrar notificaciones" className="notificaciones__filtros" onSubmit={enviar} noValidate>
      <div className="field">
        <label htmlFor={idEstado}>Estado</label>
        <select
          id={idEstado}
          className="input"
          value={filtros.estado}
          onChange={(evento) => actualizarFiltros({ ...filtros, estado: evento.target.value as Filtros["estado"] })}
        >
          <option value="">Todos</option>
          {ESTADOS.map((estado) => (
            <option key={estado} value={estado}>
              {ETIQUETAS_ESTADO[estado]}
            </option>
          ))}
        </select>
      </div>
      <Campo
        etiqueta="Turno"
        name="turno"
        inputMode="numeric"
        error={errorTurno}
        value={filtros.turno}
        onChange={(evento) => actualizarFiltros({ ...filtros, turno: evento.target.value })}
      />
      <Boton type="submit" variante="secundaria"><Icono nombre="buscar" />Buscar</Boton>
    </form>
  );
}
