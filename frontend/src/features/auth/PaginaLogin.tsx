import { useEffect, useRef, useState, type FormEvent } from "react";
import { Navigate } from "react-router-dom";
import { ErrorApi } from "../../shared/api/errorApi";
import { Alerta } from "../../shared/ui/Alerta";
import { Boton } from "../../shared/ui/Boton";
import { Campo } from "../../shared/ui/Campo";
import { useSesion } from "./sesion";

export function PaginaLogin() {
  const { estado, comprobar, iniciar } = useSesion();
  const [pendiente, actualizarPendiente] = useState(false);
  const enviando = useRef(false);
  const [fallo, actualizarFallo] = useState<"credenciales" | "tecnico">();

  useEffect(() => {
    if (estado.tipo === "inicial") comprobar();
  }, [estado.tipo, comprobar]);

  if (estado.tipo === "activa") return <Navigate to="/" replace />;
  if (estado.tipo === "inicial" || estado.tipo === "comprobando") {
    return <p role="status">Comprobando la sesión…</p>;
  }

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (enviando.current) return;
    const datos = new FormData(evento.currentTarget);
    enviando.current = true;
    actualizarPendiente(true);
    actualizarFallo(undefined);
    try {
      await iniciar({ email: String(datos.get("email")).trim(), password: String(datos.get("password")) });
    } catch (error) {
      actualizarFallo(error instanceof ErrorApi && error.estado === 401 ? "credenciales" : "tecnico");
    } finally {
      enviando.current = false;
      actualizarPendiente(false);
    }
  }

  return (
    <main>
      <h1 id="titulo-login">Ingresar al área del personal</h1>
      <form aria-labelledby="titulo-login" onSubmit={(evento) => void enviar(evento)}>
        <Campo etiqueta="Email" name="email" type="email" autoComplete="username" required />
        <Campo etiqueta="Contraseña" name="password" type="password" autoComplete="current-password" required />
        {fallo === "credenciales" ? (
          <Alerta tipo="peligro" titulo="No pudimos iniciar sesión. Verificá tus credenciales">
            <p>Si ya hiciste varios intentos, esperá unos minutos antes de volver a probar.</p>
          </Alerta>
        ) : fallo === "tecnico" ? (
          <Alerta tipo="peligro" titulo="No pudimos completar el ingreso. Intentá de nuevo." />
        ) : null}
        <Boton type="submit" disabled={pendiente}>{pendiente ? "Ingresando…" : "Iniciar sesión"}</Boton>
      </form>
    </main>
  );
}
