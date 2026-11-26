import { useRef, useState } from "react";
import { Alerta } from "../../shared/ui/Alerta";
import { Boton } from "../../shared/ui/Boton";
import { Icono } from "../../shared/ui/Icono";
import { useSesion } from "./sesion";

export function CerrarSesion({ compacto = false }: { compacto?: boolean } = {}) {
  const { cerrar } = useSesion();
  const enCurso = useRef(false);
  const [pendiente, actualizarPendiente] = useState(false);
  const [fallo, actualizarFallo] = useState(false);

  async function salir() {
    if (enCurso.current) return;
    enCurso.current = true;
    actualizarPendiente(true);
    actualizarFallo(false);
    try {
      await cerrar();
    } catch {
      actualizarFallo(true);
    } finally {
      enCurso.current = false;
      actualizarPendiente(false);
    }
  }

  return (
    <div>
      {compacto ? (
        <button type="button" className="boton-icono" disabled={pendiente}
          aria-label={pendiente ? "Cerrando sesión…" : "Cerrar sesión"} onClick={() => void salir()}>
          <Icono nombre="salir" />
        </button>
      ) : (
        <Boton disabled={pendiente} onClick={() => void salir()}>{pendiente ? "Cerrando sesión…" : "Cerrar sesión"}</Boton>
      )}
      {fallo ? <Alerta tipo="peligro" titulo="No pudimos confirmar el cierre de sesión. Intentá de nuevo." /> : null}
    </div>
  );
}
