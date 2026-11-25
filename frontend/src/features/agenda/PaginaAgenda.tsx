import { useEffect, useId, useMemo, useState } from "react";
import { fechaLocal } from "../../shared/fecha/zonaHoraria";
import { useSesion } from "../auth/sesion";
import { Alerta, BadgeEstadoTurno, Boton, type EstadoVisualTurno } from "../../shared/ui";
import { BarraAgenda } from "./BarraAgenda";
import type { FichaElegida } from "./ContenidoTurno";
import { rutaDeAgenda } from "./consulta";
import { FormularioCrearTurno } from "./crear/FormularioCrearTurno";
import { FichaPaciente } from "./FichaPaciente";
import { accionesDeTurno, mensajeGestionada, type Gestion, type TurnoGestionado } from "./gestion/acciones";
import { GestionTurnos } from "./gestion/GestionTurnos";
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

/** Agenda diaria y semanal del personal (US-013), con alta para ADMIN y RECEPCION (US-010). */
export function PaginaAgenda() {
  const [consulta, actualizarConsulta] = useState<ConsultaAgenda>({ vista: "dia" });
  const [agrupacion, actualizarAgrupacion] = useState<Agrupacion>("profesional");
  const [ficha, actualizarFicha] = useState<FichaElegida>();
  const [formularioAbierto, actualizarFormularioAbierto] = useState(false);
  const [gestionAbierta, actualizarGestionAbierta] = useState(false);
  const [mensaje, actualizarMensaje] = useState("");
  const idGestion = useId();
  const idGestionBoton = `${idGestion}-boton`;
  const idAlta = useId();
  const idNuevo = `${idAlta}-boton`;
  const { estado } = useSesion();
  const rol = estado.tipo === "activa" ? estado.identidad.rol : undefined;
  const puedeCrear = rol === "ADMIN" || rol === "RECEPCION";
  const catalogos = useCatalogosAgenda();
  const nombres = useMemo(() => nombresDe(catalogos), [catalogos]);
  const { agenda, cargando, errorCarga, recargar } = useAgenda(consulta);
  const fechaVigente = consulta.fecha ?? agenda?.desde;
  const puedeGestionar = rol !== undefined && agenda?.turnos.some(
    (turno) => accionesDeTurno(turno, rol, Date.now()).length > 0,
  );

  useEffect(() => {
    if (gestionAbierta && puedeCrear) document.getElementById(idGestion)?.focus();
  }, [gestionAbierta, idGestion, puedeCrear]);

  function cerrarGestion() {
    actualizarGestionAbierta(false);
    document.getElementById(idGestionBoton)?.focus();
  }

  function actualizarTrasGestion(gestion: Gestion, turno: TurnoGestionado) {
    cerrarGestion();
    actualizarMensaje(mensajeGestionada(gestion, turno));
    if (gestion.accion === "reprogramar") {
      refrescarParaTurno(turno.inicio, turno.profesionalId, turno.sillonId);
    } else {
      void recargar();
    }
  }

  useEffect(() => {
    if (formularioAbierto && puedeCrear) document.getElementById(idAlta)?.focus();
  }, [formularioAbierto, idAlta, puedeCrear]);

  function cerrarFormulario() {
    actualizarFormularioAbierto(false);
    document.getElementById(idNuevo)?.focus();
  }

  function refrescarParaTurno(inicio: string, profesionalId: number, sillonId: number) {
    const siguiente: ConsultaAgenda = {
      ...consulta,
      fecha: fechaLocal(inicio),
      profesionalId: consulta.profesionalId === profesionalId ? profesionalId : undefined,
      sillonId: consulta.sillonId === sillonId ? sillonId : undefined,
    };
    if (rutaDeAgenda(siguiente) === rutaDeAgenda(consulta)) void recargar();
    else actualizarConsulta(siguiente);
  }

  function mostrarTurnoCreado(inicio: string, profesionalId: number, sillonId: number) {
    refrescarParaTurno(inicio, profesionalId, sillonId);
    cerrarFormulario();
    actualizarMensaje("Turno creado.");
  }

  function cambiar(cambios: Partial<ConsultaAgenda>) {
    actualizarConsulta((actual) => ({ ...actual, ...cambios }));
  }

  function desplazar(sentido: -1 | 1) {
    if (!agenda) return;
    cambiar({ fecha: sumarDias(agenda.desde, sentido * (agenda.vista === "dia" ? 1 : 7)) });
  }

  return (
    <section className="pagina-agenda">
      <div className="pagina-agenda__encabezado">
        <h2>Agenda</h2>
        <div className="pagina-agenda__acciones">
          {puedeCrear && (
            <Boton id={idNuevo} disabled={gestionAbierta} aria-expanded={formularioAbierto} aria-controls={formularioAbierto ? `${idAlta}-panel` : undefined}
              onClick={() => {
                if (formularioAbierto) {
                  document.getElementById(idAlta)?.focus();
                  return;
                }
                actualizarMensaje("");
                actualizarFormularioAbierto(true);
              }}>Nuevo turno</Boton>
          )}
          {(puedeGestionar || gestionAbierta) && puedeCrear && (
            <Boton id={idGestionBoton} variante="secundaria" disabled={formularioAbierto || cargando || errorCarga}
              aria-expanded={gestionAbierta} aria-controls={gestionAbierta ? `${idGestion}-panel` : undefined}
              onClick={() => {
                if (gestionAbierta) {
                  document.getElementById(idGestion)?.focus();
                  return;
                }
                actualizarMensaje("");
                actualizarGestionAbierta(true);
              }}>Gestionar turnos</Boton>
          )}
        </div>
      </div>
      {mensaje && <p role="status">{mensaje}</p>}
      {gestionAbierta && puedeCrear && rol && agenda && (
        <GestionTurnos tituloId={idGestion} turnos={agenda.turnos} rol={rol} catalogos={catalogos}
          nombres={nombres} cargandoAgenda={cargando || errorCarga} onCerrar={cerrarGestion}
          onActualizarAgenda={() => { cerrarGestion(); void recargar(); }} onCompletado={actualizarTrasGestion} />
      )}
      {puedeCrear && formularioAbierto && (
        <FormularioCrearTurno
          tituloId={idAlta}
          fechaInicial={fechaVigente ?? fechaLocal(Date.now())}
          catalogos={catalogos}
          onCerrar={cerrarFormulario}
          onCreado={mostrarTurnoCreado}
        />
      )}
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
