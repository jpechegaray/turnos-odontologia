import { useEffect, useRef, useState } from "react";
import { Alerta, Boton, Icono, Tabla, type ColumnaTabla } from "../../shared/ui";
import { api } from "../../shared/api/clienteApi";
import { useSesion } from "../auth/sesion";
import { hayFiltros } from "./consulta";
import { FiltrosNotificaciones } from "./FiltrosNotificaciones";
import { ETIQUETAS_TIPO, fechaHora, textoPaginacion, turnoDe } from "./presentacion";
import { MENSAJE_ERROR_CARGA, MENSAJE_PERMISO, MENSAJE_SIN_COINCIDENCIAS, MENSAJE_SIN_ENVIOS } from "./textos";
import type { NotificacionRespuesta } from "./tipos";
import { useNotificaciones } from "./useNotificaciones";
import "./notificaciones.css";

function FechaEnvio({ instante }: { instante: string | null }) {
  return instante ? <time dateTime={instante}>{fechaHora(instante)}</time>
    : <span aria-label="Sin fecha de envío">{fechaHora(null)}</span>;
}

type Correo = Readonly<{ asunto: string; cuerpo: string }>;

function VistaPreviaCorreo({ avisoId }: { avisoId: number }) {
  const [correo, mostrarCorreo] = useState<Correo>();
  const [fallo, mostrarFallo] = useState(false);

  useEffect(() => {
    let vigente = true;
    void api.get<Correo>(`/notificaciones/${avisoId}/vista-previa`).then((respuesta) => {
      if (vigente) mostrarCorreo(respuesta);
    }, () => {
      if (vigente) mostrarFallo(true);
    });
    return () => { vigente = false; };
  }, [avisoId]);

  return (
    <section className="notificaciones__correo" aria-label="Vista previa del correo">
      <h4>Vista previa del correo</h4>
      {fallo ? <Alerta tipo="peligro" titulo="No pudimos cargar la vista previa del correo." /> : null}
      {correo ? <><strong>{correo.asunto}</strong><p className="notificaciones__cuerpo">{correo.cuerpo}</p></> : null}
      {!correo && !fallo ? <p role="status">Cargando vista previa…</p> : null}
    </section>
  );
}

function cargarLecturas(clave: string): ReadonlySet<number> {
  try {
    const ids: unknown = JSON.parse(localStorage.getItem(clave) ?? "[]");
    if (Array.isArray(ids)) {
      return new Set(ids.filter((id): id is number => typeof id === "number" && Number.isSafeInteger(id) && id > 0));
    }
  } catch {
    return new Set();
  }
  return new Set();
}

/** Registro de notificaciones para administración y recepción (US-020, RN-30). */
export function PaginaNotificaciones() {
  const { estado } = useSesion();
  return estado.tipo === "activa" ? <Bandeja key={estado.identidad.id} usuarioId={estado.identidad.id} /> : null;
}

function Bandeja({ usuarioId }: { usuarioId: number }) {
  const { pagina, cargando, error, filtrosAplicados, buscar, irAPagina, reintentar } = useNotificaciones();
  const claveLecturas = `turnos.notificaciones.leidas.${usuarioId}`;
  const [leidas, actualizarLeidas] = useState(() => cargarLecturas(claveLecturas));
  const [seleccionadas, actualizarSeleccion] = useState<ReadonlySet<number>>(() => new Set());
  const [detalle, actualizarDetalle] = useState<NotificacionRespuesta>();
  const tituloDetalle = useRef<HTMLHeadingElement>(null);
  const botonApertura = useRef<HTMLButtonElement>();
  const avisos = pagina?.contenido ?? [];
  const seleccion = avisos.filter((aviso) => seleccionadas.has(aviso.id));
  const todasSeleccionadas = avisos.length > 0 && seleccion.length === avisos.length;

  useEffect(() => {
    if (detalle) tituloDetalle.current?.focus();
    else botonApertura.current?.focus();
  }, [detalle]);

  function seleccionar(id: number, elegida: boolean) {
    actualizarSeleccion((actuales) => {
      const siguientes = new Set(actuales);
      if (elegida) siguientes.add(id);
      else siguientes.delete(id);
      return siguientes;
    });
  }

  function guardarLecturas(avisos: readonly NotificacionRespuesta[], leida: boolean) {
    const siguientes = new Set(leidas);
    for (const { id } of avisos) {
      if (leida) siguientes.add(id);
      else siguientes.delete(id);
    }
    actualizarLeidas(siguientes);
    try {
      localStorage.setItem(claveLecturas, JSON.stringify([...siguientes]));
    } catch {
      // El estado en memoria sigue disponible si el navegador bloquea el almacenamiento.
    }
  }

  function marcarLectura(leida: boolean) {
    guardarLecturas(seleccion, leida);
    actualizarSeleccion(new Set());
  }

  function abrirDetalle(aviso: NotificacionRespuesta, boton: HTMLButtonElement) {
    botonApertura.current = boton;
    if (!leidas.has(aviso.id)) guardarLecturas([aviso], true);
    actualizarDetalle(aviso);
  }

  function cambiarPagina(numero: number) {
    actualizarSeleccion(new Set());
    void irAPagina(numero);
  }

  const columnas: readonly ColumnaTabla<NotificacionRespuesta>[] = [
    {
      clave: "seleccion", titulo: "Selección", celda: (n) => (
        <input type="checkbox" disabled={cargando} checked={seleccionadas.has(n.id)}
          aria-label={`Seleccionar ${ETIQUETAS_TIPO[n.tipo]} para ${n.destinatario}`}
          onChange={(evento) => seleccionar(n.id, evento.currentTarget.checked)} />
      ),
    },
    {
      clave: "tipo", titulo: "Tipo", celda: (n) => (
        <button type="button" className="celda-persona notificaciones__abrir" disabled={cargando}
          onClick={(evento) => abrirDetalle(n, evento.currentTarget)}>
          <span className="icono-caja"><Icono nombre="mail" /></span>
          <strong>{ETIQUETAS_TIPO[n.tipo]}</strong>
        </button>
      ),
    },
    { clave: "destinatario", titulo: "Destinatario", celda: (n) => n.destinatario },
    { clave: "turno", titulo: "Turno", celda: turnoDe },
    {
      clave: "lectura", titulo: "Lectura", celda: (n) => leidas.has(n.id)
        ? <span className="notificaciones__lectura"><Icono nombre="check" />Leída</span>
        : <strong className="notificaciones__lectura notificaciones__lectura--nueva">No leída</strong>,
    },
    {
      clave: "enviada", titulo: "Fecha de envío", numerica: true, celda: (n) => (
        <>
          <span className="notificaciones__etiqueta-movil" aria-hidden="true">Envío </span>
          <FechaEnvio instante={n.enviadaEn} />
        </>
      ),
    },
  ];

  return (
    <section className="pagina-notificaciones">
      <div className="encabezado">
        <div>
          <h2>Notificaciones</h2>
          <p className="encabezado__bajada">Avisos a pacientes. La lectura se marca para tu cuenta en este navegador.</p>
        </div>
      </div>
      {detalle && (
        <section className="panel notificaciones__detalle" aria-label="Detalle de notificación">
          <Boton variante="secundaria" tamano="chico" onClick={() => actualizarDetalle(undefined)}>
            <Icono nombre="izq" />Volver a la bandeja
          </Boton>
          <div className="celda-persona">
            <span className="icono-caja"><Icono nombre="mail" /></span>
            <h3 ref={tituloDetalle} tabIndex={-1}>{ETIQUETAS_TIPO[detalle.tipo]}</h3>
          </div>
          <dl className="notificaciones__datos">
            <div><dt>Destinatario</dt><dd>{detalle.destinatario}</dd></div>
            <div><dt>Turno</dt><dd>{turnoDe(detalle)}</dd></div>
            <div><dt>Fecha de envío</dt><dd><FechaEnvio instante={detalle.enviadaEn} /></dd></div>
          </dl>
          <VistaPreviaCorreo key={detalle.id} avisoId={detalle.id} />
        </section>
      )}
      <section className="panel panel--plano notificaciones" aria-label="Registro" hidden={detalle !== undefined}>
      <FiltrosNotificaciones onBuscar={(filtros) => {
        actualizarSeleccion(new Set());
        void buscar(filtros);
      }} />
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
          {avisos.length > 0 && (
            <div className="notificaciones__acciones" role="group" aria-label="Acciones de notificaciones">
              <label className="notificaciones__seleccionar">
                <input type="checkbox" disabled={cargando} checked={todasSeleccionadas}
                  ref={(casilla) => { if (casilla) casilla.indeterminate = seleccion.length > 0 && !todasSeleccionadas; }}
                  aria-label="Seleccionar todos los avisos de esta página"
                  onChange={(evento) => actualizarSeleccion(new Set(evento.currentTarget.checked ? avisos.map(({ id }) => id) : []))} />
                Seleccionar todos
              </label>
              <Boton variante="secundaria" tamano="chico" disabled={cargando || seleccion.length !== 1}
                onClick={(evento) => abrirDetalle(seleccion[0], evento.currentTarget)}>
                Abrir notificación
              </Boton>
              <Boton variante="secundaria" tamano="chico" disabled={cargando || seleccion.length === 0} onClick={() => marcarLectura(true)}>
                Marcar como leído
              </Boton>
              <Boton variante="secundaria" tamano="chico" disabled={cargando || seleccion.length === 0} onClick={() => marcarLectura(false)}>
                Marcar como no leído
              </Boton>
            </div>
          )}
          <Tabla
            titulo="Registro de notificaciones"
            tituloOculto
            columnas={columnas}
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
                onClick={() => cambiarPagina(pagina.pagina - 1)}
              >
                Anterior
              </Boton>
              <span aria-live="polite">{textoPaginacion(pagina)}</span>
              <Boton
                variante="secundaria"
                tamano="chico"
                disabled={cargando || pagina.pagina >= pagina.totalPaginas - 1}
                onClick={() => cambiarPagina(pagina.pagina + 1)}
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
