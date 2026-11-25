import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../../shared/api/clienteApi";
import type { ProfesionalVinculable, UsuarioRespuesta } from "./tipos";

type Datos = Readonly<{
  usuarios: readonly UsuarioRespuesta[];
  profesionales: readonly ProfesionalVinculable[];
}>;

/**
 * Carga en paralelo los usuarios (en el orden de la API, por email) y los profesionales, que hacen
 * falta para mostrar y elegir el vínculo del odontólogo. Un contador descarta las respuestas viejas
 * cuando se encadenan varias recargas después de escribir.
 */
export function useUsuarios() {
  const [datos, actualizarDatos] = useState<Datos>();
  const [cargando, actualizarCargando] = useState(true);
  const [errorCarga, actualizarErrorCarga] = useState(false);
  const solicitud = useRef(0);

  const recargar = useCallback(() => {
    const actual = ++solicitud.current;
    actualizarCargando(true);
    return Promise.all([
      api.get<UsuarioRespuesta[]>("/usuarios"),
      api.get<ProfesionalVinculable[]>("/profesionales"),
    ])
      .then(
        ([usuarios, profesionales]) => {
          if (actual !== solicitud.current) return;
          actualizarDatos({ usuarios, profesionales });
          actualizarErrorCarga(false);
        },
        () => {
          if (actual !== solicitud.current) return;
          actualizarDatos(undefined);
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

  return {
    usuarios: datos?.usuarios,
    profesionales: datos?.profesionales ?? [],
    cargando,
    errorCarga,
    recargar,
  };
}
