import { Fragment } from "react";
import { formatearHora } from "../../shared/fecha/zonaHoraria";
import { BadgeEstadoTurno, claseDeEstado } from "../../shared/ui";
import { ContenidoTurno, type FichaElegida } from "./ContenidoTurno";
import { diasDeLaSemana, nombreDelDia, tramoEnDia } from "./grilla";
import { fechaCorta, rangoLegible, type Nombres } from "./presentacion";
import type { AgendaRespuesta } from "./tipos";

type Props = Readonly<{
  agenda: AgendaRespuesta;
  nombres: Nombres;
  onVerFicha: (ficha: FichaElegida) => void;
}>;

/**
 * Semana de lunes a domingo con una columna por día. Cada día lista sus horarios de atención y, en orden
 * de hora, los bloqueos y los turnos que lo tocan (un bloqueo de varios días aparece en cada uno).
 */
export function VistaSemana({ agenda, nombres, onVerFicha }: Props) {
  return (
    <div className="semana" role="region" aria-label="Agenda de la semana, desplazable horizontalmente" tabIndex={0}>
      {diasDeLaSemana(agenda.desde).map((fecha) => {
        const titulo = `${nombreDelDia(fecha)} ${fechaCorta(fecha)}`;
        const horarios = agenda.horarios.filter((h) => h.fecha === fecha);
        const items = [
          ...agenda.bloqueos.flatMap((bloqueo) => {
            const tramo = tramoEnDia(bloqueo.inicio, bloqueo.fin, fecha);
            if (!tramo) return [];
            return [{
              clave: `bloqueo-${bloqueo.id}`,
              desde: tramo.desdeMin,
              nodo: (
                <li className="semana__item st-bloqueo">
                  <BadgeEstadoTurno estado="BLOQUEO" />
                  <span className="slot__meta">{rangoLegible(bloqueo.inicio, bloqueo.fin)}</span>
                  <span className="slot__detalle">
                    {bloqueo.motivo ? `${nombres.profesional(bloqueo.profesionalId)} · ${bloqueo.motivo}`
                      : nombres.profesional(bloqueo.profesionalId)}
                  </span>
                </li>
              ),
            }];
          }),
          ...agenda.turnos.flatMap((turno) => {
            const tramo = tramoEnDia(turno.inicio, turno.fin, fecha);
            if (!tramo) return [];
            const cancelado = turno.estado === "CANCELADO" ? " semana__item--cancelado" : "";
            return [{
              clave: `turno-${turno.id}`,
              desde: tramo.desdeMin,
              nodo: (
                <li className={`semana__item ${claseDeEstado(turno.estado)}${cancelado}`}>
                  <ContenidoTurno
                    turno={turno}
                    prestacion={nombres.prestacion(turno.prestacionId)}
                    detalle={`${nombres.profesional(turno.profesionalId)} · ${nombres.sillon(turno.sillonId)}`}
                    onVerFicha={onVerFicha}
                  />
                </li>
              ),
            }];
          }),
        ].sort((a, b) => a.desde - b.desde);

        return (
          <section key={fecha} className="semana__dia" aria-label={titulo}>
            <h4 className="semana__titulo">{titulo}</h4>
            {horarios.length > 0 && (
              <ul className="semana__atencion" aria-label={`Horarios de atención del ${titulo}`}>
                {horarios.map((h) => (
                  <li key={`${h.profesionalId}-${h.inicio}`}>
                    {`${nombres.profesional(h.profesionalId)}: ${formatearHora(h.inicio)} a ${formatearHora(h.fin)}`}
                  </li>
                ))}
              </ul>
            )}
            {items.length > 0 ? (
              <ul className="semana__lista">
                {items.map((item) => <Fragment key={item.clave}>{item.nodo}</Fragment>)}
              </ul>
            ) : (
              <p className="semana__vacio">Sin turnos</p>
            )}
          </section>
        );
      })}
    </div>
  );
}

