import { useEffect, useRef, useState } from "react";
import { ErrorApi } from "../../shared/api/errorApi";
import { fechaLocal, formatearHora } from "../../shared/fecha/zonaHoraria";
import { Alerta, Avatar, Boton } from "../../shared/ui";
import { FormularioReserva, VERSION_AVISO, type ErroresPaciente } from "./FormularioReserva";
import {
  DATOS_INICIALES, etiquetaHorario, fechaTrasDias, leerAlternativas, pedirReserva, rutaDisponibilidad,
  type CampoPaciente, type Consultorio, type Horario, type PrestacionPublica,
  type ProfesionalPublico, type ReservaConfirmada,
} from "./reservaApi";
import { useConsultaReserva } from "./useConsultaReserva";
import { CabeceraPublica, PasosReserva } from "./CabeceraPublica";
import "./reserva.css";

const NOMBRES_PASOS = ["Prestación", "Profesional", "Horario", "Tus datos"] as const;

const PASOS = [
  "Elegí una prestación", "Elegí un profesional", "Elegí día y horario", "Tus datos", "Turno reservado",
] as const;
const DIA_CORTO = new Intl.DateTimeFormat("es-AR", {
  timeZone: "America/Argentina/Buenos_Aires", weekday: "short", day: "2-digit", month: "short",
});

function nombreProfesional(profesional: ProfesionalPublico) {
  return `${profesional.nombre} ${profesional.apellido}`;
}

function EstadoConsulta({ estado, recargar }: { estado: "listo" | "cargando" | "error"; recargar: () => void }) {
  if (estado === "cargando") return <p role="status">Cargando…</p>;
  if (estado === "error") {
    return <div className="reserva-bloque">
      <Alerta tipo="peligro" titulo="No pudimos cargar los datos." />
      <Boton variante="secundaria" onClick={recargar}>Volver a intentar</Boton>
    </div>;
  }
  return null;
}

export default function PaginaReserva() {
  const [paso, irAPaso] = useState(0);
  const [prestacion, elegirPrestacion] = useState<PrestacionPublica>();
  const [profesional, elegirProfesional] = useState<ProfesionalPublico>();
  const [horario, elegirHorario] = useState<Horario>();
  const [hoy] = useState(() => fechaLocal(new Date()));
  const [desde, cambiarDesde] = useState(hoy);
  const [dia, cambiarDia] = useState<string>();
  const [alternativas, cambiarAlternativas] = useState<Horario[] | null>(null);
  const [datos, cambiarDatos] = useState(DATOS_INICIALES);
  const [aceptado, aceptar] = useState(false);
  const [errores, cambiarErrores] = useState<ErroresPaciente>({});
  const [aviso, cambiarAviso] = useState("");
  const [enviando, cambiarEnviando] = useState(false);
  const [confirmada, confirmar] = useState<ReservaConfirmada>();
  const enCurso = useRef(false);
  const titulo = useRef<HTMLHeadingElement>(null);
  const avisoError = useRef<HTMLDivElement>(null);

  const prestaciones = useConsultaReserva<PrestacionPublica[]>(paso === 0 ? "/public/prestaciones" : null);
  const profesionales = useConsultaReserva<ProfesionalPublico[]>(
    paso === 1 && prestacion ? `/public/profesionales?prestacionId=${prestacion.id}` : null,
  );
  const horarios = useConsultaReserva<Horario[]>(
    paso === 2 && alternativas === null && prestacion && profesional
      ? rutaDisponibilidad(prestacion.id, profesional.id, desde)
      : null,
  );
  const consulta = useConsultaReserva<Consultorio>(paso === 3 ? "/public/consultorio" : null);
  const consultorio = consulta.estado.tipo === "listo" ? consulta.estado.datos : undefined;

  useEffect(() => { titulo.current?.focus(); }, [paso]);
  useEffect(() => { if (aviso) avisoError.current?.focus(); }, [aviso]);

  function seleccionarPrestacion(nueva: PrestacionPublica) {
    if (prestacion?.id !== nueva.id) {
      elegirProfesional(undefined);
      elegirHorario(undefined);
      cambiarAlternativas(null);
      cambiarDesde(hoy);
      cambiarDia(undefined);
    }
    elegirPrestacion(nueva);
    irAPaso(1);
  }

  function seleccionarProfesional(nuevo: ProfesionalPublico) {
    if (profesional?.id !== nuevo.id) {
      elegirHorario(undefined);
      cambiarAlternativas(null);
      cambiarDesde(hoy);
      cambiarDia(undefined);
    }
    elegirProfesional(nuevo);
    irAPaso(2);
  }

  function actualizar(campo: CampoPaciente, valor: string) {
    cambiarDatos((anteriores) => ({ ...anteriores, [campo]: valor }));
    cambiarErrores((anteriores) => ({ ...anteriores, [campo]: undefined }));
  }

  function informarError(error: unknown) {
    if (error instanceof ErrorApi) {
      if (error.tipo === "conflicto" && error.codigo === "RN-01") {
        elegirHorario(undefined);
        cambiarDia(undefined);
        cambiarAlternativas(leerAlternativas(error.propiedades.alternativas));
        irAPaso(2);
        return;
      }
      if (error.codigo === "CONSENTIMIENTO_VERSION_NO_VIGENTE") {
        aceptar(false);
        consulta.recargar();
        cambiarAviso("El consentimiento cambió. Leé el aviso actualizado y volvé a aceptar.");
        return;
      }
      if (error.tipo === "validacion" && error.campos.length > 0) {
        const incidencias: ErroresPaciente = {};
        for (const { campo, mensaje } of error.campos) {
          if (Object.prototype.hasOwnProperty.call(DATOS_INICIALES, campo)) {
            incidencias[campo as CampoPaciente] = mensaje;
          }
        }
        cambiarErrores(incidencias);
        cambiarAviso("Revisá los datos indicados.");
        return;
      }
      cambiarAviso(error.estado === 429 ? "Demasiadas solicitudes. Esperá un momento y volvé a intentar." : error.detalle);
      return;
    }
    cambiarAviso("No pudimos completar la reserva. Intentá de nuevo.");
  }

  async function reservar() {
    if (enCurso.current || !prestacion || !profesional || !horario || !aceptado
      || consultorio?.versionConsentimiento !== VERSION_AVISO) return;
    enCurso.current = true;
    cambiarEnviando(true);
    cambiarAviso("");
    cambiarErrores({});
    try {
      const turno = await pedirReserva({
        ...datos, prestacionId: prestacion.id, profesionalId: profesional.id, inicio: horario.inicio,
        versionConsentimiento: consultorio.versionConsentimiento,
      });
      confirmar(turno);
      cambiarDatos(DATOS_INICIALES);
      aceptar(false);
      irAPaso(4);
    } catch (error) {
      informarError(error);
    } finally {
      enCurso.current = false;
      cambiarEnviando(false);
    }
  }

  const opcionesHorario = alternativas ?? (horarios.estado.tipo === "listo" ? horarios.estado.datos : []);
  const fechas = [...new Set(opcionesHorario.map(({ inicio }) => fechaLocal(inicio)))];
  const fechaSeleccionada = dia && fechas.includes(dia) ? dia : fechas[0];
  const disponiblesDelDia = opcionesHorario.filter(({ inicio }) => fechaLocal(inicio) === fechaSeleccionada);
  const horarioDisponible = disponiblesDelDia.some(({ inicio }) => inicio === horario?.inicio);

  function moverSemana(dias: number) {
    cambiarDesde(fechaTrasDias(desde, dias));
    cambiarDia(undefined);
    elegirHorario(undefined);
  }

  return (
    <main className="reserva-publica">
      <CabeceraPublica />
      <div className={paso === 2 ? "reserva-panel reserva-panel--agenda" : "reserva-panel"}>
        {paso < 4 && <PasosReserva pasos={NOMBRES_PASOS} actual={paso} />}
        <div className="reserva-titulo">
          <h1 ref={titulo} tabIndex={-1}>{PASOS[paso]}</h1>
          {paso < 4 && <p className="muted">Sin crear cuenta. Te mandamos la confirmación por email.</p>}
        </div>
        {paso > 0 && paso < 4 && (
          <Boton variante="secundaria" disabled={enviando} onClick={() => {
            cambiarAviso("");
            irAPaso(paso - 1);
          }}>Atrás</Boton>
        )}
        {aviso && <div ref={avisoError} tabIndex={-1}><Alerta tipo="peligro" titulo={aviso} /></div>}

        {paso === 0 && (
          <div className="reserva-bloque">
            <EstadoConsulta estado={prestaciones.estado.tipo} recargar={prestaciones.recargar} />
            {prestaciones.estado.tipo === "listo" && (
              prestaciones.estado.datos.length === 0
                ? <p role="status">No hay prestaciones disponibles para reservar.</p>
                : prestaciones.estado.datos.map((opcion) => (
                  <Boton key={opcion.id} variante="secundaria" className="opcion" onClick={() => seleccionarPrestacion(opcion)}>
                    {opcion.nombre}
                  </Boton>
                ))
            )}
          </div>
        )}
        {paso === 1 && (
          <div className="reserva-bloque">
            <EstadoConsulta estado={profesionales.estado.tipo} recargar={profesionales.recargar} />
            {profesionales.estado.tipo === "listo" && (
              profesionales.estado.datos.length === 0
                ? <p role="status">No hay profesionales disponibles para esta prestación.</p>
                : profesionales.estado.datos.map((opcion) => (
                  <Boton key={opcion.id} variante="secundaria" className="opcion" onClick={() => seleccionarProfesional(opcion)}>
                    {nombreProfesional(opcion)}
                  </Boton>
                ))
            )}
          </div>
        )}
        {paso === 2 && prestacion && profesional && (
          <div className="reserva-distribucion">
          <section className="reserva-bloque" aria-label="Horarios disponibles">
            <div className="reserva-elegido">
              <Avatar nombre={nombreProfesional(profesional)} colorId={profesional.id} />
              <div><strong>{prestacion.nombre}</strong><small>{nombreProfesional(profesional)}</small></div>
              <Boton variante="secundaria" onClick={() => irAPaso(0)}>Cambiar</Boton>
            </div>
            {alternativas ? (
              <>
                <Alerta tipo="peligro" titulo="Ese horario ya se ocupó.">
                  <p>{alternativas.length ? "Elegí una alternativa para continuar." : "No hay alternativas disponibles."}</p>
                </Alerta>
                <Boton variante="secundaria" onClick={() => {
                  horarios.recargar();
                  cambiarAlternativas(null);
                }}>Buscar otros horarios</Boton>
              </>
            ) : (
              <>
                <p>Semana desde <time dateTime={desde}>{desde.split("-").reverse().join("/")}</time></p>
                <div className="reserva-semanas">
                  <Boton variante="secundaria" disabled={desde === hoy}
                    onClick={() => moverSemana(-7)}>Semana anterior</Boton>
                  <Boton variante="secundaria" onClick={() => moverSemana(7)}>
                    Semana siguiente
                  </Boton>
                </div>
                <EstadoConsulta estado={horarios.estado.tipo} recargar={horarios.recargar} />
                {horarios.estado.tipo === "listo" && opcionesHorario.length === 0 && (
                  <p role="status">No hay horarios disponibles en esta semana.</p>
                )}
              </>
            )}
            {fechas.length > 0 && <div className="reserva-dias" role="group" aria-label="Día">
              {fechas.map((fecha) => {
                const ejemplo = opcionesHorario.find(({ inicio }) => fechaLocal(inicio) === fecha);
                return ejemplo && <Boton key={fecha} variante="secundaria" aria-pressed={fecha === fechaSeleccionada}
                  onClick={() => { cambiarDia(fecha); elegirHorario(undefined); }}>
                  {DIA_CORTO.format(Date.parse(ejemplo.inicio))}
                </Boton>;
              })}
            </div>}
            {["Mañana", "Tarde"].map((franja, i) => {
              const opciones = disponiblesDelDia.filter(({ inicio }) => (formatearHora(inicio) < "13:00" ? 0 : 1) === i);
              if (opciones.length === 0) return null;
              return <section key={franja} className="reserva-franja" aria-label={`Horarios de la ${franja.toLowerCase()}`}>
                <h2>{franja}</h2>
                <div className="reserva-horarios">
                  {opciones.map((opcion) => (
                    <Boton key={opcion.inicio} variante="secundaria" className="hora"
                      aria-pressed={horario?.inicio === opcion.inicio} onClick={() => elegirHorario(opcion)}>
                      {formatearHora(opcion.inicio)}
                    </Boton>
                  ))}
                </div>
              </section>;
            })}
          </section>
          <aside className="panel reserva-eleccion" aria-label="Resumen del turno">
            <h2>Tu turno</h2>
            <dl>
              <div><dt>Prestación</dt><dd>{prestacion.nombre}</dd></div>
              <div><dt>Profesional</dt><dd>{nombreProfesional(profesional)}</dd></div>
              <div><dt>Día y hora</dt><dd>{horario && horarioDisponible ? etiquetaHorario(horario.inicio) : "Elegí un horario"}</dd></div>
            </dl>
            <Boton anchoCompleto disabled={!horarioDisponible} onClick={() => {
              cambiarAviso("");
              irAPaso(3);
            }}>Continuar</Boton>
          </aside>
          </div>
        )}
        {paso === 3 && prestacion && profesional && horario && (
          <>
            <p className="reserva-resumen">{prestacion.nombre} · {nombreProfesional(profesional)} · {etiquetaHorario(horario.inicio)}</p>
            <EstadoConsulta estado={consulta.estado.tipo} recargar={consulta.recargar} />
            <FormularioReserva datos={datos} actualizar={actualizar} errores={errores} consultorio={consultorio}
              aceptado={aceptado} aceptar={aceptar} enviando={enviando} confirmar={() => void reservar()} />
          </>
        )}
        {paso === 4 && confirmada && (
          <div className="reserva-bloque">
            <p className="reserva-resumen">
              {confirmada.prestacion} · {confirmada.profesional} · {etiquetaHorario(confirmada.inicio)}
            </p>
            <a className="btn btn--primary btn--block" href={`/turno/${encodeURIComponent(confirmada.token)}`}>
              Gestionar mi turno
            </a>
          </div>
        )}
      </div>
    </main>
  );
}
