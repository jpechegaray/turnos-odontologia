import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../shared/api/clienteApi";
import { rutaDeAgenda } from "./consulta";
import type { AgendaRespuesta, ConsultaAgenda } from "./tipos";

/**
 * Consulta la agenda cada vez que cambia la consulta. Un contador descarta las respuestas viejas cuando
 * el usuario navega más rápido de lo que responde la API.
 */
export function useAgenda(consulta: ConsultaAgenda) {
  const [agenda, actualizarAgenda] = useState<AgendaRespuesta>();
  const [cargando, actualizarCargando] = useState(true);
  const [errorCarga, actualizarErrorCarga] = useState(false);
  const solicitud = useRef(0);
  const ruta = rutaDeAgenda(consulta);

  const recargar = useCallback(() => {
    const actual = ++solicitud.current;
    actualizarCargando(true);
    return api
      .get<AgendaRespuesta>(ruta)
      .then(
        (respuesta) => {
          if (actual !== solicitud.current) return;
          actualizarAgenda(respuesta);
          actualizarErrorCarga(false);
        },
        () => {
          if (actual !== solicitud.current) return;
          actualizarErrorCarga(true);
        },
      )
      .finally(() => {
        if (actual === solicitud.current) actualizarCargando(false);
      });
  }, [ruta]);

  useEffect(() => {
    void recargar();
  }, [recargar]);

  return { agenda, cargando, errorCarga, recargar };
}
