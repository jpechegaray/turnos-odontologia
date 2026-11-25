import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../../shared/api/clienteApi";
import { ErrorApi } from "../../../shared/api/errorApi";
import type { ProfesionalRespuesta } from "./tipos";

/** Estados excluyentes de la carga: la pantalla muestra uno solo por vez. */
export type EstadoProfesionales =
  | Readonly<{ tipo: "cargando" }>
  | Readonly<{ tipo: "error" }>
  /** `403` del `GET`: el odontólogo no tiene un profesional vinculado (no es un error recuperable). */
  | Readonly<{ tipo: "sinVinculo" }>
  | Readonly<{ tipo: "listo"; profesionales: readonly ProfesionalRespuesta[] }>;

/**
 * Carga `GET /api/profesionales` en el orden de la API. Un contador descarta las respuestas viejas
 * cuando se encadenan varias recargas después de escribir.
 */
export function useProfesionales() {
  const [estado, actualizarEstado] = useState<EstadoProfesionales>({ tipo: "cargando" });
  const solicitud = useRef(0);

  const recargar = useCallback(async () => {
    const actual = ++solicitud.current;
    actualizarEstado({ tipo: "cargando" });
    try {
      const lista = await api.get<ProfesionalRespuesta[]>("/profesionales");
      if (actual === solicitud.current) actualizarEstado({ tipo: "listo", profesionales: lista });
    } catch (error) {
      if (actual !== solicitud.current) return;
      const sinVinculo = error instanceof ErrorApi && error.estado === 403;
      actualizarEstado({ tipo: sinVinculo ? "sinVinculo" : "error" });
    }
  }, []);

  useEffect(() => {
    void recargar();
  }, [recargar]);

  return { estado, recargar };
}
