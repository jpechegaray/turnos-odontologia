import { useState, type FormEvent } from "react";
import { Boton, Campo, Icono } from "../../shared/ui";
import { FILTROS_VACIOS, turnoValido } from "./consulta";
import { MENSAJE_TURNO_INVALIDO } from "./textos";
import type { FiltrosNotificaciones as Filtros } from "./tipos";

export type FiltrosNotificacionesProps = Readonly<{
  onBuscar: (filtros: Filtros) => void;
}>;

/** Un turno inválido no consulta el registro. */
export function FiltrosNotificaciones({ onBuscar }: FiltrosNotificacionesProps) {
  const [filtros, actualizarFiltros] = useState<Filtros>(FILTROS_VACIOS);
  const [errorTurno, actualizarErrorTurno] = useState<string>();

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
      <Campo
        etiqueta="Turno"
        name="turno"
        inputMode="numeric"
        error={errorTurno}
        value={filtros.turno}
        onChange={(evento) => actualizarFiltros({ turno: evento.target.value })}
      />
      <Boton type="submit" variante="secundaria"><Icono nombre="buscar" />Buscar</Boton>
    </form>
  );
}
