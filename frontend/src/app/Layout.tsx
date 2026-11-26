import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { CerrarSesion } from "../features/auth/CerrarSesion";
import { useSesion } from "../features/auth/sesion";
import { ETIQUETAS_ROL } from "../features/auth/usuarios/roles";
import { Icono } from "../shared/ui/Icono";
import { Avatar } from "../shared/ui/Avatar";
import { Logo, NombreMarca } from "../shared/ui/Logo";
import { fechaLocal } from "../shared/fecha/zonaHoraria";
import type { EntradaMenuResuelta } from "./rutas/tipos";
import { agruparMenu, type ContextoLayout } from "./contextoLayout";
import "./layout.css";

function Enlace({ entrada, alElegir }: { entrada: EntradaMenuResuelta; alElegir: () => void }) {
  return (
    <NavLink to={entrada.ruta} end className="layout__enlace" onClick={alElegir}>
      <Icono nombre={entrada.icono ?? "punto"} />
      <span>{entrada.etiqueta}</span>
    </NavLink>
  );
}

/**
 * Marco de la propuesta: menú lateral agrupado, barra de acciones común y accesos inferiores en celular.
 */
export function Layout({ menu }: { menu: readonly EntradaMenuResuelta[] }) {
  const { estado } = useSesion();
  const [abierto, actualizarAbierto] = useState(false);
  const [busqueda, actualizarBusqueda] = useState("");
  const lateral = useRef<HTMLElement>(null);
  const disparador = useRef<HTMLButtonElement>();
  const { pathname } = useLocation();
  const navegar = useNavigate();

  useEffect(() => { actualizarAbierto(false); }, [pathname]);

  useEffect(() => {
    if (!abierto) return;
    if (disparador.current) lateral.current?.querySelector("a")?.focus();
    const alPresionar = (evento: KeyboardEvent) => {
      if (evento.key !== "Escape") return;
      actualizarAbierto(false);
      disparador.current?.focus();
    };
    document.addEventListener("keydown", alPresionar);
    return () => document.removeEventListener("keydown", alPresionar);
  }, [abierto]);

  const cerrar = () => actualizarAbierto(false);
  const grupos = agruparMenu(menu);
  const identidad = estado.tipo === "activa" ? estado.identidad : null;
  const accesos = ["/", "/agenda", "/pacientes"].flatMap((ruta) => menu.filter((entrada) => entrada.ruta === ruta));
  const puedeBuscar = menu.some((entrada) => entrada.ruta === "/pacientes");
  const puedeCrear = identidad?.rol === "ADMIN" || identidad?.rol === "RECEPCION";

  return (
    <div className="layout" data-menu-abierto={abierto || undefined}>
      <aside ref={lateral} className="layout__lateral" id="menu-principal">
        <div className="layout__marca">
          <span className="layout__logo"><Logo /></span>
          <div>
            <h1 className="layout__nombre" aria-label={window.BRAND.name}><NombreMarca /></h1>
            <p className="layout__subtitulo">{window.BRAND.subtitulo}</p>
          </div>
        </div>

        <nav className="layout__nav" aria-label="Principal">
          {grupos.map((grupo) => (
            <section key={grupo.id} className="layout__grupo">
              <h2 className="layout__rotulo" id={`grupo-${grupo.id}`}>{grupo.titulo}</h2>
              <ul aria-labelledby={`grupo-${grupo.id}`}>
                {grupo.entradas.map((entrada) => (
                  <li key={entrada.ruta}><Enlace entrada={entrada} alElegir={cerrar} /></li>
                ))}
              </ul>
            </section>
          ))}
        </nav>

        <div className="layout__usuario">
          {identidad ? (
            <>
              <Avatar nombre={identidad.email} neutro />
              <p className="layout__identidad">
                <strong>{identidad.email}</strong>
                <span>{ETIQUETAS_ROL[identidad.rol]}</span>
              </p>
            </>
          ) : null}
          <CerrarSesion compacto />
        </div>
      </aside>

      {abierto ? <button type="button" className="layout__velo" aria-label="Cerrar menú" tabIndex={-1} onClick={cerrar} /> : null}

      <div className="layout__principal">
        <header className="layout__barra">
          <button type="button" className="boton-icono layout__boton-menu" aria-label="Abrir menú"
            aria-expanded={abierto} aria-controls="menu-principal" onClick={(evento) => {
              disparador.current = evento.currentTarget;
              actualizarAbierto(!abierto);
            }}>
            <Icono nombre="menu" />
          </button>
          {puedeBuscar && (
            <form className="layout__buscar" role="search" aria-label="Buscar en el consultorio" onSubmit={(evento) => {
              evento.preventDefault();
              if (!busqueda.trim()) return;
              navegar("/pacientes", { state: { busqueda: busqueda.trim() } });
              actualizarBusqueda("");
            }}>
              <Icono nombre="buscar" />
              <input aria-label="Buscar paciente por nombre o DNI" placeholder="Buscar paciente por nombre o DNI…"
                value={busqueda} onChange={(evento) => actualizarBusqueda(evento.target.value)} />
            </form>
          )}
          <div className="layout__barra-fin">
            {pathname === "/" && <time className="layout__fecha mono" dateTime={fechaLocal(Date.now())}>
              {new Intl.DateTimeFormat("es-AR", { timeZone: "America/Argentina/Buenos_Aires", weekday: "short",
                day: "2-digit", month: "2-digit", year: "numeric" }).format(Date.now())}
            </time>}
            {puedeCrear && (
              <Link id="nuevo-turno-principal" className="btn btn--primary" to="/agenda?nuevo=1"><Icono nombre="mas" />Nuevo turno</Link>
            )}
          </div>
        </header>
        <main className="layout__contenido">
          <Outlet context={{ menu } satisfies ContextoLayout} />
        </main>
      </div>

      <nav className="layout__accesos" aria-label="Accesos rápidos">
        {accesos.map((entrada) => <Enlace key={entrada.ruta} entrada={entrada} alElegir={cerrar} />)}
        <button type="button" aria-expanded={abierto} aria-controls="menu-principal" onClick={(evento) => {
          disparador.current = evento.currentTarget;
          actualizarAbierto(!abierto);
        }}>
          <Icono nombre="menu" />Más
        </button>
      </nav>

    </div>
  );
}
