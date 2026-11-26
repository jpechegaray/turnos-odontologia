import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { Alerta, BadgeEstadoTurno, Boton } from "../../../shared/ui";
import { EnlaceNoDisponible } from "../EnlaceNoDisponible";
import { etiquetaHorario } from "../reservaApi";
import { MENSAJE_REINTENTO, esEnlaceNoDisponible } from "../validacionPublica";
import {
  admiteCancelar, admiteConfirmar, cancelarTurno, confirmarTurno, consultarTurno, type TurnoPorToken,
} from "./turnoTokenApi";
import "../reserva.css";

type Estado =
  | { tipo: "cargando" }
  | { tipo: "error-carga" }
  | { tipo: "no-disponible" }
  | { tipo: "listo"; turno: TurnoPorToken; cancelado: boolean };

/** `/turno/:token`: ver, confirmar y cancelar el turno del enlace (US-016, Flujo 4 paso 9). */
export default function PaginaTurnoToken() {
  const { token = "" } = useParams();
  const [intento, cambiarIntento] = useState(0);
  const [estado, cambiarEstado] = useState<Estado>({ tipo: "cargando" });
  const [preguntando, cambiarPreguntando] = useState(false);
  const [aviso, cambiarAviso] = useState("");
  const [enviando, cambiarEnviando] = useState(false);
  const enCurso = useRef(false);
  const pregunta = useRef<HTMLDivElement>(null);
  const resultado = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    let vigente = true;
    cambiarEstado({ tipo: "cargando" });
    consultarTurno(token).then(
      (turno) => { if (vigente) cambiarEstado({ tipo: "listo", turno, cancelado: false }); },
      (error: unknown) => {
        if (vigente) cambiarEstado(esEnlaceNoDisponible(error) ? { tipo: "no-disponible" } : { tipo: "error-carga" });
      },
    );
    return () => { vigente = false; };
  }, [token, intento]);

  useEffect(() => { if (preguntando) pregunta.current?.focus(); }, [preguntando]);

  async function ejecutar(accion: () => Promise<Estado>) {
    // Un solo pedido en curso por acción, sin reintentos automáticos.
    if (enCurso.current) return;
    enCurso.current = true;
    cambiarEnviando(true);
    cambiarAviso("");
    try {
      cambiarEstado(await accion());
      cambiarPreguntando(false);
    } catch (error) {
      if (esEnlaceNoDisponible(error)) cambiarEstado({ tipo: "no-disponible" });
      else cambiarAviso(MENSAJE_REINTENTO);
    } finally {
      enCurso.current = false;
      cambiarEnviando(false);
    }
  }

  function confirmar() {
    void ejecutar(async () => ({ tipo: "listo", turno: await confirmarTurno(token), cancelado: false }));
  }

  function cancelar(turno: TurnoPorToken) {
    // El 204 revoca el token: otra consulta daría 404, así que el estado se actualiza en la pantalla.
    void ejecutar(async () => {
      await cancelarTurno(token);
      return { tipo: "listo", turno: { ...turno, estado: "CANCELADO" }, cancelado: true };
    }).then(() => resultado.current?.focus());
  }

  return (
    <main className="reserva-publica">
      <div className="reserva-panel">
        {estado.tipo === "cargando" && <p role="status">Cargando tu turno…</p>}
        {estado.tipo === "error-carga" && (
          <div className="reserva-bloque">
            <h1>Tu turno</h1>
            <Alerta tipo="peligro" titulo="No pudimos cargar tu turno." />
            <Boton variante="secundaria" onClick={() => cambiarIntento((anterior) => anterior + 1)}>
              Volver a intentar
            </Boton>
          </div>
        )}
        {estado.tipo === "no-disponible" && <EnlaceNoDisponible />}
        {estado.tipo === "listo" && (
          <div className="reserva-bloque">
            <h1>Tu turno</h1>
            <dl className="reserva-resumen turno-token__datos">
              <dt>Fecha y hora</dt>
              <dd>{etiquetaHorario(estado.turno.inicio)}</dd>
              <dt>Profesional</dt>
              <dd>{estado.turno.profesional}</dd>
              <dt>Prestación</dt>
              <dd>{estado.turno.prestacion}</dd>
              <dt>Estado</dt>
              <dd><BadgeEstadoTurno estado={estado.turno.estado} /></dd>
            </dl>
            {estado.cancelado && (
              <p ref={resultado} tabIndex={-1} role="status">Tu turno quedó cancelado.</p>
            )}
            {aviso && <Alerta tipo="peligro" titulo={aviso} />}
            {preguntando ? (
              <div ref={pregunta} tabIndex={-1} className="reserva-aviso turno-token__pregunta">
                <p><strong>¿Querés cancelar este turno?</strong></p>
                {estado.turno.cancelacionSeriaTardia && (
                  <Alerta tipo="peligro" titulo="Esta cancelación es tardía.">
                    <p>Falta poco para el turno y la cancelación queda registrada como tardía.</p>
                  </Alerta>
                )}
                <Boton variante="peligro" anchoCompleto disabled={enviando} onClick={() => cancelar(estado.turno)}>
                  Sí, cancelar
                </Boton>
                <Boton variante="secundaria" anchoCompleto disabled={enviando} onClick={() => cambiarPreguntando(false)}>
                  Volver
                </Boton>
              </div>
            ) : (
              <>
                {admiteConfirmar(estado.turno.estado) && (
                  <Boton anchoCompleto disabled={enviando} onClick={confirmar}>Confirmar asistencia</Boton>
                )}
                {admiteCancelar(estado.turno.estado) && (
                  <Boton variante="peligro-atenuado" anchoCompleto disabled={enviando}
                    onClick={() => { cambiarAviso(""); cambiarPreguntando(true); }}>
                    Cancelar turno
                  </Boton>
                )}
              </>
            )}
            {!estado.cancelado && admiteCancelar(estado.turno.estado) && (
              <p className="turno-token__nota">Si necesitás otro horario, cancelá y reservá de nuevo o llamá al consultorio.</p>
            )}
          </div>
        )}
      </div>
    </main>
  );
}
