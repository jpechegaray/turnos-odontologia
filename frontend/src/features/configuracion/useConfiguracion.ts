import { useCallback, useEffect, useState } from "react";
import { api } from "../../shared/api/clienteApi";
import type { ConfiguracionRespuesta } from "./tipos";

/**
 * Consulta la configuración vigente al montar. No hay escrituras que disparen recargas, así que no
 * hace falta descartar respuestas viejas: se reintenta manualmente desde la alerta de error.
 */
export function useConfiguracion() {
  const [configuracion, actualizarConfiguracion] = useState<ConfiguracionRespuesta>();
  const [cargando, actualizarCargando] = useState(true);
  const [errorCarga, actualizarErrorCarga] = useState(false);

  const recargar = useCallback(() => {
    actualizarCargando(true);
    return api.get<ConfiguracionRespuesta>("/configuracion").then(
      (vigente) => {
        actualizarConfiguracion(vigente);
        actualizarErrorCarga(false);
      },
      () => {
        actualizarConfiguracion(undefined);
        actualizarErrorCarga(true);
      },
    ).finally(() => actualizarCargando(false));
  }, []);

  useEffect(() => {
    void recargar();
  }, [recargar]);

  return { configuracion, cargando, errorCarga, recargar };
}
