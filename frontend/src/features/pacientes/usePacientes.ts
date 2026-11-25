import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../shared/api/clienteApi";
import { consultaDePacientes, FILTROS_VACIOS } from "./consulta";
import type { FiltrosPacientes, PacienteRespuesta } from "./tipos";

/**
 * Carga las fichas activas que devuelve el backend, ya ordenadas por apellido y nombre, y recuerda
 * los filtros vigentes para repetir la consulta después de escribir. Un contador
 * descarta las respuestas viejas cuando se encadenan varias recargas después de escribir.
 */
export function usePacientes() {
  const [pacientes, actualizarPacientes] = useState<readonly PacienteRespuesta[]>();
  const [cargando, actualizarCargando] = useState(true);
  const [errorCarga, actualizarErrorCarga] = useState(false);
  const solicitud = useRef(0);
  const filtros = useRef<FiltrosPacientes>(FILTROS_VACIOS);

  const recargar = useCallback(() => {
    const actual = ++solicitud.current;
    actualizarCargando(true);
    return api
      .get<PacienteRespuesta[]>(consultaDePacientes(filtros.current))
      .then(
        (lista) => {
          if (actual !== solicitud.current) return;
          actualizarPacientes(lista);
          actualizarErrorCarga(false);
        },
        () => {
          if (actual !== solicitud.current) return;
          actualizarPacientes(undefined);
          actualizarErrorCarga(true);
        },
      )
      .finally(() => {
        if (actual === solicitud.current) actualizarCargando(false);
      });
  }, []);

  const buscar = useCallback(
    (nuevos: FiltrosPacientes) => {
      filtros.current = nuevos;
      return recargar();
    },
    [recargar],
  );

  useEffect(() => {
    void recargar();
  }, [recargar]);

  return { pacientes, cargando, errorCarga, buscar, recargar };
}
