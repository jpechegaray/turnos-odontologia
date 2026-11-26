import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useMenuDelLayout } from "../../app/contextoLayout";
import { aMilisegundos, formatearHora } from "../../shared/fecha/zonaHoraria";
import { Alerta } from "../../shared/ui/Alerta";
import { BadgeEstadoTurno } from "../../shared/ui/BadgeEstadoTurno";
import { Avatar } from "../../shared/ui/Avatar";
import { Boton } from "../../shared/ui/Boton";
import { Icono, type NombreIcono } from "../../shared/ui/Icono";
import { fechaLarga, nombreDelPaciente, nombresDe } from "../agenda/presentacion";
import type { AgendaRespuesta, EstadoTurno, TurnoAgenda } from "../agenda/tipos";
import { useAgenda } from "../agenda/useAgenda";
import { useCatalogosAgenda } from "../agenda/useCatalogosAgenda";
import { listarEntradas } from "../lista-espera/api";
import "./inicio.css";

const CONSULTA_HOY = { vista: "dia" } as const;
const MAXIMO_PROXIMOS = 5;

function contar(turnos: readonly TurnoAgenda[], estado: EstadoTurno): number {
  return turnos.filter((t) => t.estado === estado).length;
}

function Cifra({ titulo, valor, detalle, icono }: { titulo: string; valor: number | string; detalle?: string; icono: NombreIcono }) {
  return (
    <div className="panel cifra">
      <p className="cifra__titulo"><span className="cifra__icono"><Icono nombre={icono} /></span>{titulo}</p>
      <p className="cifra__valor">{valor}</p>
      {detalle ? <p className="cifra__detalle">{detalle}</p> : null}
    </div>
  );
}

const ACCESOS = [
  { permiso: "/pacientes", ruta: "/pacientes?alta=1", texto: "Registrar paciente", icono: "pacientes" },
  { permiso: "/lista-espera", ruta: "/lista-espera", texto: "Revisar lista de espera", icono: "espera" },
  { permiso: "/profesionales", ruta: "/profesionales?seccion=bloqueos", texto: "Ver bloqueos", icono: "bloqueo" },
] as const;

function Accesos() {
  const menu = useMenuDelLayout();
  const disponibles = ACCESOS.filter(({ permiso }) => menu.some(({ ruta }) => ruta === permiso));
  if (disponibles.length === 0) return null;
  return (
    <section className="panel inicio__lateral-panel" aria-labelledby="titulo-accesos">
      <h3 id="titulo-accesos">Accesos rápidos</h3>
      {disponibles.map(({ ruta, texto, icono }) => (
        <Link key={ruta} to={ruta} className="inicio__atajo">
          <Icono nombre={icono} /><span>{texto}</span><Icono nombre="der" />
        </Link>
      ))}
    </section>
  );
}

function ventanasDelDia(horarios: AgendaRespuesta["horarios"]) {
  const ventanas: { inicio: number; fin: number }[] = [];
  for (const horario of [...horarios].sort((a, b) => aMilisegundos(a.inicio) - aMilisegundos(b.inicio))) {
    const inicio = aMilisegundos(horario.inicio);
    const fin = aMilisegundos(horario.fin);
    const ultima = ventanas[ventanas.length - 1];
    if (ultima && inicio <= ultima.fin) ultima.fin = Math.max(ultima.fin, fin);
    else ventanas.push({ inicio, fin });
  }
  return ventanas;
}

function Ocupacion({ agenda, sillones }: { agenda: AgendaRespuesta; sillones: ReturnType<typeof useCatalogosAgenda>["sillones"] }) {
  const ventanas = ventanasDelDia(Array.isArray(agenda.horarios) ? agenda.horarios : []);
  const total = ventanas.reduce((suma, { inicio, fin }) => suma + fin - inicio, 0);
  const activos = sillones.filter((sillon) => sillon.activo);
  return (
    <section className="panel inicio__lateral-panel" aria-labelledby="titulo-ocupacion">
      <h3 id="titulo-ocupacion">Ocupación de sillones</h3>
      {activos.length === 0 || total <= 0 ? <p className="muted">No hay horarios para calcular la ocupación.</p> :
        activos.map(({ id, nombre }) => {
          const turnos = agenda.turnos.filter((turno) => turno.sillonId === id && turno.estado !== "CANCELADO");
          const ocupado = turnos.reduce((suma, turno) => suma + ventanas.reduce((duracion, ventana) =>
            duracion + Math.max(0, Math.min(ventana.fin, aMilisegundos(turno.fin))
              - Math.max(ventana.inicio, aMilisegundos(turno.inicio))), 0), 0);
          const porcentaje = Math.min(100, Math.round(100 * ocupado / total));
          return <div key={id} className="inicio__ocupacion">
            <div><span>{nombre}</span><span className="mono">{porcentaje} %</span></div>
            <progress max={100} value={porcentaje} aria-label={`Ocupación de ${nombre}`} />
          </div>;
        })}
    </section>
  );
}

export function PaginaInicio() {
  const { agenda, cargando, errorCarga, recargar } = useAgenda(CONSULTA_HOY);
  const catalogos = useCatalogosAgenda();
  const nombres = nombresDe(catalogos);
  const puedeVerEspera = useMenuDelLayout().some(({ ruta }) => ruta === "/lista-espera");
  const [enEspera, actualizarEspera] = useState<number>();
  useEffect(() => {
    if (!puedeVerEspera) return;
    let vigente = true;
    void listarEntradas("ACTIVA").then((entradas) => {
      if (vigente && Array.isArray(entradas)) actualizarEspera(entradas.length);
    }, () => undefined);
    return () => { vigente = false; };
  }, [puedeVerEspera]);

  const delDia = Array.isArray(agenda?.turnos) ? agenda.turnos : [];
  const turnos = delDia.filter((t) => t.estado !== "CANCELADO");
  const ahora = Date.now();
  const proximos = [...turnos]
    .filter((t) => aMilisegundos(t.inicio) >= ahora)
    .sort((a, b) => aMilisegundos(a.inicio) - aMilisegundos(b.inicio))
    .slice(0, MAXIMO_PROXIMOS);
  const ausentes = contar(delDia, "AUSENTE");

  return (
    <section className="inicio" aria-labelledby="titulo-inicio">
      <header className="inicio__encabezado">
        <h2 id="titulo-inicio">Inicio</h2>
        {agenda?.desde ? <p className="muted">{fechaLarga(agenda.desde)}</p> : null}
      </header>

      {errorCarga && !cargando ? (
        <div className="inicio__error">
          <Alerta tipo="peligro" titulo="No pudimos cargar el resumen del día." />
          <Boton variante="secundaria" onClick={() => void recargar()}>Reintentar</Boton>
        </div>
      ) : !agenda ? (
        <p role="status" className="muted">Cargando el resumen del día…</p>
      ) : (
        <>
          <section className="cifras" aria-label="Cifras del día">
            <Cifra titulo="Turnos de hoy" valor={turnos.length} icono="agenda" detalle={`${contar(turnos, "ATENDIDO")} ya atendidos`} />
            <Cifra titulo="Confirmados" valor={contar(turnos, "CONFIRMADO")} icono="check" />
            <Cifra titulo="Sin confirmar" valor={contar(turnos, "PROGRAMADO")} icono="reloj" />
            {puedeVerEspera
              ? <Cifra titulo="Lista de espera" valor={enEspera ?? "—"} icono="espera" />
              : <Cifra titulo="Atendidos" valor={contar(turnos, "ATENDIDO")} icono="hecho" detalle={`${ausentes} ausentes`} />}
          </section>

          <div className="inicio__columnas">
          <section className="panel proximos" aria-labelledby="titulo-proximos">
            <div className="proximos__cabecera">
              <h3 id="titulo-proximos">Próximos turnos</h3>
              <Link to="/agenda" className="btn btn--sm">Ver agenda</Link>
            </div>
            {proximos.length === 0 ? (
              <p className="proximos__vacio muted">No quedan turnos para hoy</p>
            ) : (
              <ul className="proximos__lista">
                {proximos.map((t) => (
                  <li key={t.id} className="proximo">
                    <p className="proximo__hora mono">{formatearHora(t.inicio)}<small>{t.duracionMin} min</small></p>
                    <div className="proximo__datos">
                      <p className="proximo__paciente">{nombreDelPaciente(t) ?? "Paciente de otro profesional"}</p>
                      <p className="proximo__detalle">
                        <Avatar nombre={nombres.profesional(t.profesionalId)} colorId={t.profesionalId} tamano="sm" />
                        {nombres.profesional(t.profesionalId)} · {nombres.sillon(t.sillonId)} · {nombres.prestacion(t.prestacionId)}
                      </p>
                    </div>
                    <BadgeEstadoTurno estado={t.estado} />
                  </li>
                ))}
              </ul>
            )}
          </section>
          <aside className="inicio__lateral">
            <Ocupacion agenda={agenda} sillones={catalogos.sillones} />
            <Accesos />
          </aside>
          </div>
        </>
      )}
    </section>
  );
}
