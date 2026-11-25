import { useMemo, useState } from "react";
import { Alerta, BadgeEstadoTurno, Boton, type EstadoVisualTurno } from "../../shared/ui";
import { BarraAgenda } from "./BarraAgenda";
import type { FichaElegida } from "./ContenidoTurno";
import { FichaPaciente } from "./FichaPaciente";
import { GrillaDia } from "./GrillaDia";
import { nombreDelDia, sumarDias } from "./grilla";
import { fechaCorta, nombresDe } from "./presentacion";
import type { AgendaRespuesta, Agrupacion, ConsultaAgenda } from "./tipos";
import { useAgenda } from "./useAgenda";
import { useCatalogosAgenda } from "./useCatalogosAgenda";
import { VistaSemana } from "./VistaSemana";
import "./agenda.css";

const LEYENDA: readonly EstadoVisualTurno[] = ["PROGRAMADO", "CONFIRMADO", "ATENDIDO", "AUSENTE", "CANCELADO", "BLOQUEO"];

function tituloDelPeriodo(agenda: AgendaRespuesta): string {
  return agenda.vista === "dia"
    ? `${nombreDelDia(agenda.desde)} ${fechaCorta(agenda.desde, true)}`
    : `Semana del ${fechaCorta(agenda.desde, true)} al ${fechaCorta(agenda.hasta, true)}`;
}

/**
 * Agenda diaria y semanal del personal (US-013). Es de solo lectura para todos los roles: las acciones
 * sobre turnos llegan con C-45 y C-46. El primer pedido va sin fecha para que el servidor decida cuál es
 * el día de hoy en Buenos Aires (RN-14).
 */
export function PaginaAgenda() {
  const [consulta, actualizarConsulta] = useState<ConsultaAgenda>({ vista: "dia" });
  const [agrupacion, actualizarAgrupacion] = useState<Agrupacion>("profesional");
  const [ficha, actualizarFicha] = useState<FichaElegida>();
  const catalogos = useCatalogosAgenda();
  const nombres = useMemo(() => nombresDe(catalogos), [catalogos]);
  const { agenda, cargando, errorCarga, recargar } = useAgenda(consulta);
  const fechaVigente = consulta.fecha ?? agenda?.desde;

  function cambiar(cambios: Partial<ConsultaAgenda>) {
    actualizarConsulta((actual) => ({ ...actual, ...cambios }));
  }

  function desplazar(sentido: -1 | 1) {
    if (!agenda) return;
    cambiar({ fecha: sumarDias(agenda.desde, sentido * (agenda.vista === "dia" ? 1 : 7)) });
  }

  return (
    <section className="pagina-agenda">
      <h2>Agenda</h2>
      <BarraAgenda
        vista={consulta.vista}
        agrupacion={agrupacion}
        fecha={fechaVigente}
        profesionalId={consulta.profesionalId}
        sillonId={consulta.sillonId}
        catalogos={catalogos}
        navegable={agenda !== undefined}
        onVista={(vista) => cambiar({ vista, fecha: fechaVigente })}
        onAgrupacion={actualizarAgrupacion}
        onDesplazar={desplazar}
        onHoy={() => cambiar({ fecha: undefined })}
        onFecha={(fecha) => cambiar({ fecha })}
        onProfesional={(profesionalId) => cambiar({ profesionalId })}
        onSillon={(sillonId) => cambiar({ sillonId })}
      />
      {ficha && <FichaPaciente key={ficha.pacienteId} elegida={ficha} onCerrar={() => actualizarFicha(undefined)} />}
      {errorCarga ? (
        <Alerta tipo="peligro" titulo="No pudimos cargar la agenda.">
          <Boton variante="secundaria" onClick={() => void recargar()}>Reintentar</Boton>
        </Alerta>
      ) : !agenda ? (
        cargando && <p role="status">Cargando…</p>
      ) : (
        <>
          <h3 className="agenda-periodo" aria-busy={cargando}>{tituloDelPeriodo(agenda)}</h3>
          {agenda.vista === "dia" ? (
            <GrillaDia
              agenda={agenda}
              agrupacion={agrupacion}
              filtro={agrupacion === "profesional" ? consulta.profesionalId : consulta.sillonId}
              catalogos={catalogos}
              nombres={nombres}
              onVerFicha={actualizarFicha}
            />
          ) : (
            <VistaSemana agenda={agenda} nombres={nombres} onVerFicha={actualizarFicha} />
          )}
          <ul className="leyenda" aria-label="Estados de la agenda">
            {LEYENDA.map((estado) => (
              <li key={estado}><BadgeEstadoTurno estado={estado} /></li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}
