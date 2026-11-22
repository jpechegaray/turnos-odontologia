import { Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { ProveedorSesion } from "../features/auth/ProveedorSesion";
import { RequireRole } from "../features/auth/RequireRole";
import { RequireSesion } from "../features/auth/RequireSesion";
import { tieneRol } from "../features/auth/permisos";
import { useSesion } from "../features/auth/sesion";
import { Layout } from "./Layout";
import { PaginaNoEncontrada } from "./PaginaNoEncontrada";
import type { RegistroRutas, RutaFeature } from "./rutas/tipos";

function LayoutInterno({ registro }: { registro: RegistroRutas }) {
  const { estado } = useSesion();
  if (estado.tipo !== "activa") return null;
  const rol = estado.identidad.rol;
  const permitidas = new Set(registro.rutas
    .filter(({ ambito, roles }) => ambito === undefined && tieneRol(rol, roles))
    .map(({ ruta }) => ruta));
  return <Layout menu={registro.menu.filter(({ ruta }) => permitidas.has(ruta))} />;
}

function pantalla({ componente: Componente }: RutaFeature) {
  return (
    <Suspense fallback={<p role="status">Cargando…</p>}>
      <Componente />
    </Suspense>
  );
}

export function AppRutas({ registro }: { registro: RegistroRutas }) {
  return (
    <ProveedorSesion>
      <Routes>
        {registro.rutas.filter(({ ambito }) => ambito !== undefined).map((entrada) => (
          <Route key={entrada.ruta} path={entrada.ruta} element={pantalla(entrada)} />
        ))}
        <Route element={<RequireSesion />}>
          <Route element={<LayoutInterno registro={registro} />}>
            {registro.rutas.filter(({ ambito }) => ambito === undefined).map((entrada) => (
              <Route
                key={entrada.ruta}
                path={entrada.ruta}
                element={<RequireRole roles={entrada.roles}>{pantalla(entrada)}</RequireRole>}
              />
            ))}
            <Route path="*" element={<PaginaNoEncontrada />} />
          </Route>
        </Route>
      </Routes>
    </ProveedorSesion>
  );
}
