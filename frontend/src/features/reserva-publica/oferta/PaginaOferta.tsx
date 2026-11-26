import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { Alerta, Boton } from "../../../shared/ui";
import { EnlaceNoDisponible } from "../EnlaceNoDisponible";
import { etiquetaHorario } from "../reservaApi";
import { MENSAJE_REINTENTO, esEnlaceNoDisponible } from "../validacionPublica";
import {
  MENSAJE_ESTADO, consultarOferta, estadoTrasRechazo, tomarOferta, type OfertaPublica, type TurnoTomado,
} from "./ofertaApi";
import "../reserva.css";
import { CabeceraPublica } from "../CabeceraPublica";

type Estado =
  | { tipo: "cargando" }
  | { tipo: "error-carga" }
  | { tipo: "no-disponible" }
  | { tipo: "listo"; oferta: OfertaPublica }
  | { tipo: "tomado"; turno: TurnoTomado };

function Resumen({ inicio, profesional, prestacion }: Readonly<{ inicio: string; profesional: string; prestacion: string }>) {
  return (
    <dl className="reserva-resumen turno-token__datos">
      <dt>Fecha y hora</dt>
      <dd>{etiquetaHorario(inicio)}</dd>
      <dt>Profesional</dt>
      <dd>{profesional}</dd>
      {prestacion && (<><dt>Prestación</dt><dd>{prestacion}</dd></>)}
    </dl>
  );
}

/** `/oferta/:token`: ver y tomar el hueco ofrecido por la lista de espera (US-022, Flujo 7). */
export default function PaginaOferta() {
  const { token = "" } = useParams();
  const [intento, cambiarIntento] = useState(0);
  const [estado, cambiarEstado] = useState<Estado>({ tipo: "cargando" });
  const [aviso, cambiarAviso] = useState("");
  const [enviando, cambiarEnviando] = useState(false);
  const enCurso = useRef(false);
  const titulo = useRef<HTMLHeadingElement>(null);
  const mensaje = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let vigente = true;
    cambiarEstado({ tipo: "cargando" });
    consultarOferta(token).then(
      (oferta) => { if (vigente) cambiarEstado({ tipo: "listo", oferta }); },
      (error: unknown) => {
        if (vigente) cambiarEstado(esEnlaceNoDisponible(error) ? { tipo: "no-disponible" } : { tipo: "error-carga" });
      },
    );
    return () => { vigente = false; };
  }, [token, intento]);

  useEffect(() => { if (estado.tipo === "tomado") titulo.current?.focus(); }, [estado.tipo]);

  async function tomar(oferta: OfertaPublica) {
    // Un solo pedido en curso, sin reintentos automáticos: el 409 del servidor resuelve la concurrencia.
    if (enCurso.current) return;
    enCurso.current = true;
    cambiarEnviando(true);
    cambiarAviso("");
    try {
      cambiarEstado({ tipo: "tomado", turno: await tomarOferta(token) });
    } catch (error) {
      const rechazo = estadoTrasRechazo(error);
      if (esEnlaceNoDisponible(error)) cambiarEstado({ tipo: "no-disponible" });
      else if (rechazo) {
        cambiarEstado({ tipo: "listo", oferta: { ...oferta, estado: rechazo } });
        mensaje.current?.focus();
      } else cambiarAviso(MENSAJE_REINTENTO);
    } finally {
      enCurso.current = false;
      cambiarEnviando(false);
    }
  }

  return (
    <main className="reserva-publica">
      <CabeceraPublica />
      <div className="reserva-panel">
        {estado.tipo === "cargando" && <p role="status">Cargando la oferta…</p>}
        {estado.tipo === "error-carga" && (
          <div className="reserva-bloque">
            <h1>Se liberó un turno</h1>
            <Alerta tipo="peligro" titulo="No pudimos cargar la oferta." />
            <Boton variante="secundaria" onClick={() => cambiarIntento((anterior) => anterior + 1)}>
              Volver a intentar
            </Boton>
          </div>
        )}
        {estado.tipo === "no-disponible" && <EnlaceNoDisponible />}
        {estado.tipo === "listo" && (
          <div className="reserva-bloque">
            <h1>Se liberó un turno</h1>
            <Resumen {...estado.oferta} />
            <div ref={mensaje} tabIndex={-1} role="status">
              {estado.oferta.estado !== "DISPONIBLE" && (
                <Alerta tipo="info" titulo={MENSAJE_ESTADO[estado.oferta.estado]}>
                  <p>Seguís en la lista de espera si tu anotación sigue activa.</p>
                </Alerta>
              )}
            </div>
            {aviso && <Alerta tipo="peligro" titulo={aviso} />}
            {estado.oferta.estado === "DISPONIBLE" && (
              <>
                <p>Lo toma quien confirme primero. No tenés que cargar tus datos.</p>
                <Boton anchoCompleto disabled={enviando} onClick={() => void tomar(estado.oferta)}>
                  Tomar este turno
                </Boton>
              </>
            )}
          </div>
        )}
        {estado.tipo === "tomado" && (
          <div className="reserva-bloque">
            <h1 ref={titulo} tabIndex={-1}>¡El turno es tuyo!</h1>
            <Resumen {...estado.turno} />
            <p>Te enviamos un email con este enlace para ver, confirmar o cancelar el turno.</p>
            <a className="btn btn--primary btn--block" href={`/turno/${encodeURIComponent(estado.turno.token)}`}>
              Gestionar mi turno
            </a>
          </div>
        )}
      </div>
    </main>
  );
}
