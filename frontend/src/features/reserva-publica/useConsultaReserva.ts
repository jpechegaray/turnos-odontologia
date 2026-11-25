import { useEffect, useState } from "react";
import { api } from "../../shared/api/clienteApi";

type Resultado<T> = { tipo: "listo"; datos: T } | { tipo: "error" };

export function useConsultaReserva<T>(ruta: string | null) {
  const [intento, cambiarIntento] = useState(0);
  const [resultado, cambiarResultado] = useState<{
    ruta: string;
    intento: number;
    valor: Resultado<T>;
  }>();
  useEffect(() => {
    if (ruta === null) return;
    let vigente = true;
    void api.get<T>(ruta).then(
      (datos) => { if (vigente) cambiarResultado({ ruta, intento, valor: { tipo: "listo", datos } }); },
      () => { if (vigente) cambiarResultado({ ruta, intento, valor: { tipo: "error" } }); },
    );
    return () => { vigente = false; };
  }, [ruta, intento]);
  return {
    estado: resultado?.ruta === ruta && resultado.intento === intento
      ? resultado.valor
      : { tipo: "cargando" } as const,
    recargar: () => cambiarIntento((anterior) => anterior + 1),
  };
}
