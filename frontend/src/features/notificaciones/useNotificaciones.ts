import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../shared/api/clienteApi";
import { ErrorApi } from "../../shared/api/errorApi";
import { consultaDeNotificaciones, FILTROS_VACIOS } from "./consulta";
import type { FiltrosNotificaciones, PaginaNotificaciones } from "./tipos";

export type ErrorCarga = "permiso" | "carga";

/**
 * Carga una página del registro (C-25) y recuerda los filtros y la página vigentes, para paginar y
 * reintentar con la misma consulta. Un contador descarta respuestas viejas si se encadenan consultas.
 * Ante un error se descarta la página anterior: nunca se muestran datos viejos.
 */
export function useNotificaciones() {
  const [pagina, actualizarPagina] = useState<PaginaNotificaciones>();
  const [cargando, actualizarCargando] = useState(true);
  const [error, actualizarError] = useState<ErrorCarga>();
  const [filtrosAplicados, actualizarFiltrosAplicados] = useState<FiltrosNotificaciones>(FILTROS_VACIOS);
  const solicitud = useRef(0);
  const filtros = useRef<FiltrosNotificaciones>(FILTROS_VACIOS);
  const numeroPagina = useRef(0);

  const consultar = useCallback(() => {
    const actual = ++solicitud.current;
    actualizarCargando(true);
    return api
      .get<PaginaNotificaciones>(consultaDeNotificaciones(filtros.current, numeroPagina.current))
      .then(
        (respuesta) => {
          if (actual !== solicitud.current) return;
          actualizarPagina(respuesta);
          actualizarError(undefined);
        },
        (fallo: unknown) => {
          if (actual !== solicitud.current) return;
          actualizarPagina(undefined);
          actualizarError(fallo instanceof ErrorApi && fallo.estado === 403 ? "permiso" : "carga");
        },
      )
      .finally(() => {
        if (actual === solicitud.current) actualizarCargando(false);
      });
  }, []);

  const buscar = useCallback(
    (nuevos: FiltrosNotificaciones) => {
      filtros.current = nuevos;
      numeroPagina.current = 0;
      actualizarFiltrosAplicados(nuevos);
      return consultar();
    },
    [consultar],
  );

  const irAPagina = useCallback(
    (numero: number) => {
      numeroPagina.current = numero;
      return consultar();
    },
    [consultar],
  );

  useEffect(() => {
    void consultar();
  }, [consultar]);

  return { pagina, cargando, error, filtrosAplicados, buscar, irAPagina, reintentar: consultar };
}
