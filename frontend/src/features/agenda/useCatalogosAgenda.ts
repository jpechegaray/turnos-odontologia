import { useEffect, useState } from "react";
import { api } from "../../shared/api/clienteApi";
import type { PrestacionRespuesta } from "../prestaciones/tipos";
import type { ProfesionalRespuesta } from "../profesionales/profesionales/tipos";
import type { SillonRespuesta } from "../profesionales/sillones/tipos";

export type CatalogosAgenda = Readonly<{
  profesionales: readonly ProfesionalRespuesta[];
  sillones: readonly SillonRespuesta[];
  prestaciones: readonly PrestacionRespuesta[];
}>;

const VACIOS: CatalogosAgenda = { profesionales: [], sillones: [], prestaciones: [] };

/** Si un catálogo falla, queda vacío: la agenda se sigue mostrando con nombres de respaldo. */
function listaOVacia<T>(ruta: string): Promise<readonly T[]> {
  return api.get<T[]>(ruta).catch(() => []);
}

/** Nombres para columnas, filtros y turnos. Se cargan una sola vez, en paralelo. */
export function useCatalogosAgenda(): CatalogosAgenda {
  const [catalogos, actualizarCatalogos] = useState<CatalogosAgenda>(VACIOS);

  useEffect(() => {
    let vigente = true;
    void Promise.all([
      listaOVacia<ProfesionalRespuesta>("/profesionales"),
      listaOVacia<SillonRespuesta>("/sillones"),
      listaOVacia<PrestacionRespuesta>("/prestaciones"),
    ]).then(([profesionales, sillones, prestaciones]) => {
      if (vigente) actualizarCatalogos({ profesionales, sillones, prestaciones });
    });
    return () => {
      vigente = false;
    };
  }, []);

  return catalogos;
}
