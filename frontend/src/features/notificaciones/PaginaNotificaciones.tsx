import { Alerta, Boton, Icono, Tabla, type ColumnaTabla, type NombreIcono } from "../../shared/ui";
import { hayFiltros } from "./consulta";
import { FiltrosNotificaciones } from "./FiltrosNotificaciones";
import { errorDe, ETIQUETAS_ESTADO, ETIQUETAS_TIPO, fechaHora, textoPaginacion, turnoDe } from "./presentacion";
import { MENSAJE_ERROR_CARGA, MENSAJE_PERMISO, MENSAJE_SIN_COINCIDENCIAS, MENSAJE_SIN_ENVIOS } from "./textos";
import type { NotificacionRespuesta } from "./tipos";
import { useNotificaciones } from "./useNotificaciones";
import "./notificaciones.css";

const ICONO_ESTADO: Readonly<Record<NotificacionRespuesta["estado"], NombreIcono>> = {
  PENDIENTE: "reloj",
  ENVIADA: "checkCirculo",
  FALLIDA: "alerta",
};

const COLUMNAS: readonly ColumnaTabla<NotificacionRespuesta>[] = [
  {
    clave: "tipo",
    titulo: "Tipo",
    celda: (n) => (
      <span className="celda-persona">
        <span className="icono-caja"><Icono nombre="mail" /></span>
        <strong>{ETIQUETAS_TIPO[n.tipo]}</strong>
      </span>
    ),
  },
  {
    clave: "estado",
    titulo: "Estado",
    celda: (n) => (
      <span className={`badge notif-${n.estado.toLowerCase()}`}>
        <Icono nombre={ICONO_ESTADO[n.estado]} />
        {ETIQUETAS_ESTADO[n.estado]}
      </span>
    ),
  },
  { clave: "destinatario", titulo: "Destinatario", celda: (n) => n.destinatario },
  { clave: "turno", titulo: "Turno", celda: turnoDe },
  { clave: "referencia", titulo: "Fecha de referencia", numerica: true, celda: (n) => fechaHora(n.inicioReferencia) },
  { clave: "intentos", titulo: "Intentos", numerica: true, celda: (n) => n.intentos },
  { clave: "enviada", titulo: "Fecha de envío", numerica: true, celda: (n) => fechaHora(n.enviadaEn) },
  { clave: "error", titulo: "Último error", celda: errorDe },
];

/** Registro de notificaciones para administración y recepción (US-020, RN-30). */
export function PaginaNotificaciones() {
  const { pagina, cargando, error, filtrosAplicados, buscar, irAPagina, reintentar } = useNotificaciones();

  return (
    <section className="pagina-notificaciones">
      <div className="encabezado">
        <div>
          <h2>Notificaciones</h2>
          <p className="encabezado__bajada">Registro de los emails que el sistema envía a los pacientes: confirmaciones, recordatorios y avisos.</p>
        </div>
      </div>
      <section className="panel panel--plano notificaciones" aria-label="Registro">
      <FiltrosNotificaciones onBuscar={(filtros) => void buscar(filtros)} />
      {error === "permiso" ? <div className="notificaciones__aviso"><Alerta tipo="peligro" titulo={MENSAJE_PERMISO} /></div> : null}
      {error === "carga" ? (
        <div className="notificaciones__aviso">
        <Alerta tipo="peligro" titulo={MENSAJE_ERROR_CARGA}>
          <div>
            <Boton variante="secundaria" tamano="chico" onClick={() => void reintentar()}>
              Reintentar
            </Boton>
          </div>
        </Alerta>
        </div>
      ) : null}
      {cargando && !pagina ? <p aria-live="polite" className="notificaciones__aviso">Cargando el registro…</p> : null}
      {pagina ? (
        <>
          <Tabla
            titulo="Registro de notificaciones"
            tituloOculto
            columnas={COLUMNAS}
            filas={pagina.contenido}
            claveFila={(n) => n.id}
            mensajeVacio={hayFiltros(filtrosAplicados) ? MENSAJE_SIN_COINCIDENCIAS : MENSAJE_SIN_ENVIOS}
          />
          {pagina.totalElementos > 0 ? (
            <nav className="notificaciones__paginacion" aria-label="Paginación del registro">
              <Boton
                variante="secundaria"
                tamano="chico"
                disabled={cargando || pagina.pagina <= 0}
                onClick={() => void irAPagina(pagina.pagina - 1)}
              >
                Anterior
              </Boton>
              <span aria-live="polite">{textoPaginacion(pagina)}</span>
              <Boton
                variante="secundaria"
                tamano="chico"
                disabled={cargando || pagina.pagina >= pagina.totalPaginas - 1}
                onClick={() => void irAPagina(pagina.pagina + 1)}
              >
                Siguiente
              </Boton>
            </nav>
          ) : null}
        </>
      ) : null}
      </section>
    </section>
  );
}
