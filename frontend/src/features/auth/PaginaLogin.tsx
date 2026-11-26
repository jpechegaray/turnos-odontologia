import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, Navigate } from "react-router-dom";
import { ErrorApi } from "../../shared/api/errorApi";
import { Alerta } from "../../shared/ui/Alerta";
import { Boton } from "../../shared/ui/Boton";
import { Campo } from "../../shared/ui/Campo";
import { Icono } from "../../shared/ui/Icono";
import { Logo, NombreMarca } from "../../shared/ui/Logo";
import "./login.css";
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

  const marca = (
    <div className="login__marca">
      <span className="login__logo"><Logo /></span>
      <span><NombreMarca /></span>
    </div>
  );

  return (
    <div className="login">
      <section className="login__lado" aria-label="Presentación">
        {marca}
        <div className="login__mensaje">
          <p className="login__lema">La agenda del consultorio, clara y sin sobreturnos.</p>
          <p className="login__bajada">Turnos por profesional y por sillón, recordatorios por email y lista de espera en un solo lugar.</p>
        </div>
        <ul className="login__puntos">
          <li><Icono nombre="agenda" />Agenda diaria y semanal por profesional o sillón</li>
          <li><Icono nombre="mail" />Recordatorio automático 24 h antes del turno</li>
          <li><Icono nombre="espera" />Lista de espera que ofrece los huecos liberados</li>
        </ul>
      </section>

      <main className="login__form">
        <div className="login__tarjeta">
          <div className="login__marca-movil" aria-hidden="true">{marca}</div>
          <div>
            <h1 id="titulo-login" className="login__titulo">Ingresar</h1>
            <p className="muted">Acceso para el personal del consultorio.</p>
          </div>
          <form className="login__campos" aria-labelledby="titulo-login" onSubmit={(evento) => void enviar(evento)}>
            <Campo etiqueta="Email" name="email" type="email" autoComplete="username" placeholder="nombre@consultorio.test" required />
            <Campo etiqueta="Contraseña" name="password" type="password" autoComplete="current-password" required />
            {fallo === "credenciales" ? (
              <Alerta tipo="peligro" titulo="No pudimos iniciar sesión. Verificá tus credenciales">
                <p>Si ya hiciste varios intentos, esperá unos minutos antes de volver a probar.</p>
              </Alerta>
            ) : fallo === "tecnico" ? (
              <Alerta tipo="peligro" titulo="No pudimos completar el ingreso. Intentá de nuevo." />
            ) : null}
            <Boton type="submit" anchoCompleto disabled={pendiente}>{pendiente ? "Ingresando…" : "Iniciar sesión"}</Boton>
          </form>
          <Alerta tipo="info" titulo="¿Sos paciente?">
            <p>Para gestionar tu turno usá el enlace que te llegó por email.</p>
            <Link className="btn btn--secondary btn--sm login__reserva" to="/reservar">Sacar turno</Link>
          </Alerta>
          <p className="login__pie"><Icono nombre="candado" />Sesión segura: se cierra sola tras un rato sin uso.</p>
        </div>
      </main>
    </div>
  );
}
