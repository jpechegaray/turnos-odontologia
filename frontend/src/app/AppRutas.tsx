import { Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { Layout } from "./Layout";
import { PaginaNoEncontrada } from "./PaginaNoEncontrada";
import type { RegistroRutas } from "./rutas/tipos";

/** Monta todas las rutas del registro dentro del layout; lo desconocido cae en "página no encontrada". */
export function AppRutas({ registro }: { registro: RegistroRutas }) {
  return (
    <Routes>
      <Route element={<Layout menu={registro.menu} />}>
        {registro.rutas.map(({ ruta, componente: Componente }) => (
          <Route
            key={ruta}
            path={ruta}
            element={
              <Suspense fallback={<p role="status">Cargando…</p>}>
                <Componente />
              </Suspense>
            }
          />
        ))}
        <Route path="*" element={<PaginaNoEncontrada />} />
      </Route>
    </Routes>
  );
}
