import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { api, observarSesion } from "../../shared/api/clienteApi";
import { ContextoSesion, esNoAutenticado, type Credenciales, type EstadoSesion, type Identidad } from "./sesion";

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [estado, actualizar] = useState<EstadoSesion>({ tipo: "inicial" });
  const estadoActual = useRef(estado);
  const fijarEstado = useCallback((nuevo: EstadoSesion) => {
    estadoActual.current = nuevo;
    actualizar(nuevo);
  }, []);

  useEffect(() => observarSesion(() => {
    const origen = estadoActual.current;
    if (origen.tipo !== "activa") return undefined;
    return () => {
      if (estadoActual.current === origen) fijarEstado({ tipo: "anonima" });
    };
  }), [fijarEstado]);

  const pedido = useRef(false);
  const comprobar = useCallback(() => {
    if (pedido.current) return;
    pedido.current = true;
    const origen: EstadoSesion = { tipo: "comprobando" };
    fijarEstado(origen);
    void api.get<Identidad>("/auth/me").then(
      (identidad) => {
        if (estadoActual.current === origen) fijarEstado({ tipo: "activa", identidad });
      },
      (error: unknown) => {
        if (estadoActual.current === origen) fijarEstado({ tipo: esNoAutenticado(error) ? "anonima" : "error" });
      },
    ).finally(() => { pedido.current = false; });
  }, [fijarEstado]);
  const iniciar = async (credenciales: Credenciales) => {
    const identidad = await api.post<Identidad>("/auth/login", credenciales);
    fijarEstado({ tipo: "activa", identidad });
  };
  const cerrar = async () => {
    const origen = estadoActual.current;
    try {
      await api.post<void>("/auth/logout");
    } catch (error) {
      if (!esNoAutenticado(error)) throw error;
    }
    if (estadoActual.current === origen) fijarEstado({ tipo: "anonima" });
  };
  return <ContextoSesion.Provider value={{ estado, comprobar, iniciar, cerrar }}>{children}</ContextoSesion.Provider>;
}
