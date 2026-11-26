import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { CerrarSesion } from "../features/auth/CerrarSesion";
import { useSesion } from "../features/auth/sesion";
import { ETIQUETAS_ROL } from "../features/auth/usuarios/roles";
import { Icono } from "../shared/ui/Icono";
import { Logo, NombreMarca } from "../shared/ui/Logo";
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
 * Marco común de las pantallas del personal: menú lateral verde agrupado en escritorio; en celular, una
 * barra superior con "Menú" abre el mismo menú como panel. Las pantallas reciben el menú ya filtrado por
 * rol en el contexto del Outlet (Inicio lo muestra como recuadros). El nombre sale de BRAND (design-system/brand.js)
 * para que renombrar el producto sea editar un solo lugar.
 */
export function Layout({ menu }: { menu: readonly EntradaMenuResuelta[] }) {
  const { estado } = useSesion();
  const [abierto, actualizarAbierto] = useState(false);
  const { pathname } = useLocation();

  useEffect(() => { actualizarAbierto(false); }, [pathname]);

  useEffect(() => {
    if (!abierto) return;
    const alPresionar = (evento: KeyboardEvent) => { if (evento.key === "Escape") actualizarAbierto(false); };
    document.addEventListener("keydown", alPresionar);
    return () => document.removeEventListener("keydown", alPresionar);
  }, [abierto]);

  const cerrar = () => actualizarAbierto(false);
  const grupos = agruparMenu(menu);
  const identidad = estado.tipo === "activa" ? estado.identidad : null;

  return (
    <div className="layout" data-menu-abierto={abierto || undefined}>
      <header className="layout__barra">
        <span className="layout__barra-marca" aria-hidden="true">
          <span className="layout__logo"><Logo /></span>
          <span className="layout__nombre"><NombreMarca /></span>
        </span>
        <button
          type="button"
          className="layout__boton-menu"
          aria-expanded={abierto}
          aria-controls="menu-principal"
          onClick={() => actualizarAbierto((previo) => !previo)}
        >
          <Icono nombre="menu" />
          <span>Menú</span>
        </button>
      </header>

      <aside className="layout__lateral" id="menu-principal">
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
            <p className="layout__identidad">
              <strong>{identidad.email}</strong>
              <span>{ETIQUETAS_ROL[identidad.rol]}</span>
            </p>
          ) : null}
          <CerrarSesion />
        </div>
      </aside>

      {abierto ? <button type="button" className="layout__velo" aria-label="Cerrar menú" tabIndex={-1} onClick={cerrar} /> : null}

      <main className="layout__contenido">
        <Outlet context={{ menu } satisfies ContextoLayout} />
      </main>

    </div>
  );
}
