import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../shared/api/clienteApi";
import type { PrestacionRespuesta } from "./tipos";

/**
 * Carga el catálogo completo (activas e inactivas), ordenado por nombre. Un contador descarta las
 * respuestas viejas cuando se encadenan varias recargas después de escribir.
 */
export function useCatalogoPrestaciones() {
  const [prestaciones, actualizarPrestaciones] = useState<readonly PrestacionRespuesta[]>();
  const [cargando, actualizarCargando] = useState(true);
  const [errorCarga, actualizarErrorCarga] = useState(false);
  const solicitud = useRef(0);

  const recargar = useCallback(() => {
    const actual = ++solicitud.current;
    actualizarCargando(true);
    return api.get<PrestacionRespuesta[]>("/prestaciones?soloActivas=false").then(
      (lista) => {
        if (actual !== solicitud.current) return;
        actualizarPrestaciones([...lista].sort((a, b) => a.nombre.localeCompare(b.nombre, "es")));
        actualizarErrorCarga(false);
      },
      () => {
        if (actual !== solicitud.current) return;
        actualizarPrestaciones(undefined);
        actualizarErrorCarga(true);
      },
    ).finally(() => {
      if (actual === solicitud.current) actualizarCargando(false);
    });
  }, []);

  useEffect(() => {
    void recargar();
  }, [recargar]);

  return { prestaciones, cargando, errorCarga, recargar };
}
