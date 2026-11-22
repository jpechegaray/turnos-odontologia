import { NavLink, Outlet } from "react-router-dom";
import type { EntradaMenuResuelta } from "./rutas/tipos";
import "./layout.css";

/**
 * Marco común de las pantallas del personal. El nombre sale de BRAND (design-system/brand.js) para que
 * renombrar el producto sea editar un solo lugar.
 */
export function Layout({ menu }: { menu: readonly EntradaMenuResuelta[] }) {
  return (
    <div className="layout">
      <header className="layout__cabecera">
        <h1 className="layout__marca">{window.BRAND.name}</h1>
        <nav className="layout__nav" aria-label="Principal">
          <ul>
            {menu.map((entrada) => (
              <li key={entrada.ruta}>
                <NavLink to={entrada.ruta} end className="layout__enlace">
                  {entrada.etiqueta}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </header>
      <main className="layout__contenido">
        <Outlet />
      </main>
    </div>
  );
}
