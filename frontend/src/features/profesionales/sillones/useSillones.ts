import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../../shared/api/clienteApi";
import type { SillonRespuesta } from "./tipos";

/**
 * Carga todos los sillones (activos e inactivos) ordenados por nombre en el cliente. Un contador
 * descarta las respuestas viejas cuando se encadenan varias recargas después de escribir.
 */
export function useSillones() {
  const [sillones, actualizarSillones] = useState<readonly SillonRespuesta[]>();
  const [cargando, actualizarCargando] = useState(true);
  const [errorCarga, actualizarErrorCarga] = useState(false);
  const solicitud = useRef(0);

  const recargar = useCallback(() => {
    const actual = ++solicitud.current;
    actualizarCargando(true);
    return api
      .get<SillonRespuesta[]>("/sillones")
      .then(
        (lista) => {
          if (actual !== solicitud.current) return;
          actualizarSillones([...lista].sort((a, b) => a.nombre.localeCompare(b.nombre, "es")));
          actualizarErrorCarga(false);
        },
        () => {
          if (actual !== solicitud.current) return;
          actualizarSillones(undefined);
          actualizarErrorCarga(true);
        },
      )
      .finally(() => {
        if (actual === solicitud.current) actualizarCargando(false);
      });
  }, []);

  useEffect(() => {
    void recargar();
  }, [recargar]);

  return { sillones, cargando, errorCarga, recargar };
}
