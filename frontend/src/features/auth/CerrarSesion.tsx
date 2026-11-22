import { useRef, useState } from "react";
import { Alerta } from "../../shared/ui/Alerta";
import { Boton } from "../../shared/ui/Boton";
import { useSesion } from "./sesion";

export function CerrarSesion() {
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
      <Boton disabled={pendiente} onClick={() => void salir()}>{pendiente ? "Cerrando sesión…" : "Cerrar sesión"}</Boton>
      {fallo ? <Alerta tipo="peligro" titulo="No pudimos confirmar el cierre de sesión. Intentá de nuevo." /> : null}
    </div>
  );
}
