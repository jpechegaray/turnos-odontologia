import { useEffect } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { Alerta } from "../../shared/ui/Alerta";
import { Boton } from "../../shared/ui/Boton";
import { useSesion } from "./sesion";

export function RequireSesion() {
  const { estado, comprobar } = useSesion();
  useEffect(() => {
    if (estado.tipo === "inicial") comprobar();
  }, [estado.tipo, comprobar]);

  if (estado.tipo === "activa") return <Outlet />;
  if (estado.tipo === "anonima") return <Navigate to="/login" replace />;
  if (estado.tipo === "error") {
    return (
      <>
        <Alerta tipo="peligro" titulo="No pudimos comprobar la sesión." />
        <Boton onClick={comprobar}>Reintentar</Boton>
      </>
    );
  }
  return <p role="status">Comprobando la sesión…</p>;
}
