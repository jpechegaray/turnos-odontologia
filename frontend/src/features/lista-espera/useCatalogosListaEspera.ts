import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../../shared/api/clienteApi";
import type { PacienteRespuesta } from "../pacientes/tipos";
import type { PrestacionRespuesta } from "../prestaciones/tipos";
import type { ProfesionalRespuesta } from "../profesionales/profesionales/tipos";

export type CatalogosListaEspera = Readonly<{
  prestaciones: readonly PrestacionRespuesta[];
  profesionales: readonly ProfesionalRespuesta[];
  nombrePaciente: (id: number) => string;
  nombrePrestacion: (id: number) => string;
  nombreProfesional: (id: number | null) => string;
  recargarPacientes: () => void;
}>;

/** Si un catálogo falla queda vacío y se muestra un nombre de respaldo con el ID, como en la agenda. */
function listaOVacia<T>(ruta: string): Promise<readonly T[]> {
  return api.get<T[]>(ruta).catch(() => []);
}

function porId<T extends { id: number }>(lista: readonly T[]): ReadonlyMap<number, T> {
  return new Map(lista.map((item) => [item.id, item]));
}

/**
 * La API de la lista de espera devuelve solo IDs (RN-35): los nombres salen de los catálogos, cargados en
 * paralelo. El padrón de un consultorio de 2 a 5 profesionales es chico (design).
 */
export function useCatalogosListaEspera(): CatalogosListaEspera {
  const [pacientes, actualizarPacientes] = useState<ReadonlyMap<number, PacienteRespuesta>>(new Map());
  const [prestaciones, actualizarPrestaciones] = useState<readonly PrestacionRespuesta[]>([]);
  const [profesionales, actualizarProfesionales] = useState<readonly ProfesionalRespuesta[]>([]);
  const [versionPacientes, cambiarVersionPacientes] = useState(0);

  useEffect(() => {
    let vigente = true;
    void Promise.all([
      listaOVacia<PrestacionRespuesta>("/prestaciones"),
      listaOVacia<ProfesionalRespuesta>("/profesionales"),
    ]).then(([prestacionesLeidas, profesionalesLeidos]) => {
      if (!vigente) return;
      actualizarPrestaciones(prestacionesLeidas);
      actualizarProfesionales(profesionalesLeidos);
    });
    return () => { vigente = false; };
  }, []);

  useEffect(() => {
    let vigente = true;
    void listaOVacia<PacienteRespuesta>("/pacientes").then((leidos) => {
      if (vigente) actualizarPacientes(porId(leidos));
    });
    return () => { vigente = false; };
  }, [versionPacientes]);

  const prestacionesPorId = useMemo(() => porId(prestaciones), [prestaciones]);
  const profesionalesPorId = useMemo(() => porId(profesionales), [profesionales]);
  const recargarPacientes = useCallback(() => cambiarVersionPacientes((anterior) => anterior + 1), []);

  return {
    prestaciones,
    profesionales,
    nombrePaciente: (id) => {
      const paciente = pacientes.get(id);
      return paciente ? `${paciente.nombre} ${paciente.apellido}` : `Paciente #${id}`;
    },
    nombrePrestacion: (id) => prestacionesPorId.get(id)?.nombre ?? `Prestación #${id}`,
    nombreProfesional: (id) => {
      if (id === null) return "Cualquiera";
      const profesional = profesionalesPorId.get(id);
      return profesional ? `${profesional.nombre} ${profesional.apellido}` : `Profesional #${id}`;
    },
    recargarPacientes,
  };
}
