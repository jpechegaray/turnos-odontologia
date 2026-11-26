import { Link } from "react-router-dom";
import { agruparMenu, useMenuDelLayout } from "../../app/contextoLayout";
import { aMilisegundos, formatearHora } from "../../shared/fecha/zonaHoraria";
import { Alerta } from "../../shared/ui/Alerta";
import { BadgeEstadoTurno } from "../../shared/ui/BadgeEstadoTurno";
import { Boton } from "../../shared/ui/Boton";
import { Icono, type NombreIcono } from "../../shared/ui/Icono";
import { fechaLarga, nombreDelPaciente, nombresDe } from "../agenda/presentacion";
import type { EstadoTurno, TurnoAgenda } from "../agenda/tipos";
import { useAgenda } from "../agenda/useAgenda";
import { useCatalogosAgenda } from "../agenda/useCatalogosAgenda";
import "./inicio.css";

const CONSULTA_HOY = { vista: "dia" } as const;
const MAXIMO_PROXIMOS = 5;

function contar(turnos: readonly TurnoAgenda[], estado: EstadoTurno): number {
  return turnos.filter((t) => t.estado === estado).length;
}

function Cifra({ titulo, valor, detalle, icono }: { titulo: string; valor: number; detalle?: string; icono: NombreIcono }) {
  return (
    <div className="panel cifra">
      <p className="cifra__titulo"><span className="cifra__icono"><Icono nombre={icono} /></span>{titulo}</p>
      <p className="cifra__valor">{valor}</p>
      {detalle ? <p className="cifra__detalle">{detalle}</p> : null}
    </div>
  );
}

function Secciones() {
  const grupos = agruparMenu(useMenuDelLayout().filter((entrada) => entrada.ruta !== "/"));
  if (grupos.length === 0) return null;
  return (
    <section className="secciones" aria-labelledby="titulo-secciones">
      <h3 id="titulo-secciones" className="sr-only">Secciones</h3>
      {grupos.map((grupo) => (
        <div key={grupo.id} className="secciones__grupo">
          <p className="secciones__rotulo">{grupo.titulo}</p>
          <ul className="secciones__lista">
            {grupo.entradas.map((entrada) => (
              <li key={entrada.ruta}>
                <Link to={entrada.ruta} className="seccion">
                  <span className="seccion__icono"><Icono nombre={entrada.icono ?? "punto"} /></span>
                  <span className="seccion__nombre">{entrada.etiqueta}</span>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </section>
  );
}

export function PaginaInicio() {
  const { agenda, cargando, errorCarga, recargar } = useAgenda(CONSULTA_HOY);
  const nombres = nombresDe(useCatalogosAgenda());

  const delDia = Array.isArray(agenda?.turnos) ? agenda.turnos : [];
  const turnos = delDia.filter((t) => t.estado !== "CANCELADO");
  const ahora = Date.now();
  const proximos = [...turnos]
    .filter((t) => aMilisegundos(t.inicio) >= ahora)
    .sort((a, b) => aMilisegundos(a.inicio) - aMilisegundos(b.inicio))
    .slice(0, MAXIMO_PROXIMOS);
  const pendientes = turnos.filter((t) => aMilisegundos(t.inicio) >= ahora).length;
  const ausentes = contar(delDia, "AUSENTE");

  return (
    <section className="inicio" aria-labelledby="titulo-inicio">
      <header className="inicio__encabezado">
        <h2 id="titulo-inicio">Inicio</h2>
        {agenda?.desde ? <p className="muted">{fechaLarga(agenda.desde)}</p> : null}
      </header>

      <Secciones />

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
            <Cifra titulo="Turnos de hoy" valor={turnos.length} icono="agenda" detalle={`${pendientes} por empezar`} />
            <Cifra titulo="Confirmados" valor={contar(turnos, "CONFIRMADO")} icono="check" />
            <Cifra titulo="Reservados" valor={contar(turnos, "PROGRAMADO")} icono="reloj" detalle="Sin confirmar" />
            <Cifra titulo="Atendidos" valor={contar(turnos, "ATENDIDO")} icono="hecho" detalle={`${ausentes} ${ausentes === 1 ? "ausente" : "ausentes"}`} />
          </section>

          <section className="panel proximos" aria-labelledby="titulo-proximos">
            <div className="proximos__cabecera">
              <h3 id="titulo-proximos">Próximos turnos</h3>
              <Link to="/agenda">Ver agenda</Link>
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
                      <p className="proximo__detalle">{nombres.prestacion(t.prestacionId)} · {nombres.profesional(t.profesionalId)}</p>
                    </div>
                    <BadgeEstadoTurno estado={t.estado} />
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </section>
  );
}
